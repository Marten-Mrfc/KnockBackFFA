package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.Modify
import dev.marten_mrfcyt.knockbackffa.utils.asMini
import dev.marten_mrfcyt.knockbackffa.utils.checkCustomValue
import dev.marten_mrfcyt.knockbackffa.utils.setCustomValue
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin
import org.reflections.Reflections
import java.io.File
import java.util.logging.Logger
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmName

abstract class ModifyObject(
    open val id: String,
    open val name: String,
    open val description: List<String>,
    open val icon: Material,
    open val args: List<Pair<String, Class<*>>> = emptyList(),
    open val plugin: KnockBackFFA
) {
    abstract fun handle(player: Player, item: ItemStack, args: Map<String, Any>)

    fun createGuiItem(kitName: String, slot: Int, modifyObject: ModifyObject): ItemStack {
        val descriptionWithStatus = description.toMutableList()
        val kitConfig = YamlConfiguration.loadConfiguration(File("${plugin.dataFolder}/kits.yml"))
        val item = ItemStack(icon)
        val meta: ItemMeta = item.itemMeta

        if (kitConfig.getBoolean("kit.$kitName.items.$slot.modifiers.$id", false)) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            descriptionWithStatus.add("<green>Enabled")
            args.forEach { (key, _) ->
                val value = kitConfig.get("kit.$kitName.items.$slot.modifiers.$key")
                descriptionWithStatus.add("<dark_gray>$key:<white> $value")
            }
        } else {
            descriptionWithStatus.add("<red>Disabled")
        }

        meta.displayName("<!italic><white>$name".asMini())
        meta.lore(descriptionWithStatus.map { "<reset><gray>$it".asMini() })
        setCustomValue(meta, plugin, "is_modifier", true)
        setCustomValue(meta, plugin, "modifier", modifyObject.id)
        setCustomValue(meta, plugin, "kit_name", kitName)
        setCustomValue(meta, plugin, "slot", slot)
        if (modifyObject.args.isNotEmpty()) {
            setCustomValue(meta, plugin, "args", modifyObject.args)
        }
        item.itemMeta = meta
        return item
    }
}

class ModifyHandler {
    private val logger: Logger = KnockBackFFA.instance.logger
    private val modify = mutableMapOf<String, ModifyObject>()

    init {
        try {
            val reflections = Reflections("dev.marten_mrfcyt.knockbackffa.kits.modifiers")
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

    fun handleEvent(player: Player, item: ItemStack?, args: Map<String, Any>, id: String) {
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