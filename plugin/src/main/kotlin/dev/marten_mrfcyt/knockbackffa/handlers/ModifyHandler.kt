package dev.marten_mrfcyt.knockbackffa.handlers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.annotations.Modify
import dev.marten_mrfcyt.knockbackffa.utils.asMini
import dev.marten_mrfcyt.knockbackffa.utils.checkCustomValue
import dev.marten_mrfcyt.knockbackffa.utils.setCustomValue
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin
import java.util.logging.Logger
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import org.reflections.Reflections
import java.io.File
import kotlin.reflect.jvm.jvmName

abstract class ModifyObject(
    open val id: String,
    open val name: String,
    open val description: List<String>,
    open val icon: Material,
    open val plugin: KnockBackFFA
) {
    abstract fun handle(player: Player, item: ItemStack, args: Map<String, Any>)

}

class ModifyHandler {
    private val logger: Logger = KnockBackFFA.instance.logger
    private val modify = mutableMapOf<String, ModifyObject>()

    init {
        try {
            val reflections = Reflections("dev.marten_mrfcyt.knockbackffa.modifiers")
            val classes = reflections.getSubTypesOf(ModifyObject::class.java)
            for (clazz in classes) {
                clazz.kotlin.findAnnotation<Modify>()?.let { annotation ->
                    val instance = clazz.kotlin.objectInstance ?: clazz.kotlin.primaryConstructor?.call()
                    if (instance is ModifyObject) {
                        modify[annotation.id] = instance
                    }
                } ?: logger.severe("No Modify annotation found on ${clazz.kotlin.jvmName}")
            }
        } catch (e: Exception) {
            logger.severe("Error during ModifyHandler initialization: ${e.message}")
            throw e
        }
    }

    fun getModifyObjects(): Collection<ModifyObject> = modify.values

    fun handleEvent(event: Event, player: Player, item: ItemStack?, args: Map<String, Any>, id: String) {
        item?.let {
            modify[id]?.takeIf { checkCustomValue(item.itemMeta ?: return, KnockBackFFA.instance, "modify", id) }
                ?.handle(player, it, args)
        }
    }

    fun registerEvents(plugin: Plugin) {
        modify.values.forEach { entry ->
            if (entry is Listener) {
                plugin.server.pluginManager.registerEvents(entry, plugin)
            }
        }
    }
}