package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.*
import mlib.api.utilities.*
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
import kotlin.collections.set
import kotlin.text.set
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
            descriptionWithStatus.add("<!i><green>Enabled")
            args.forEach { (key, _) ->
                val value = kitConfig.get("kit.$kitName.items.$slot.modifiers.$key")
                descriptionWithStatus.add("<dark_gray>$key:<white> $value")
            }
        } else {
            descriptionWithStatus.add("<!i><red>Disabled")
        }

        meta.displayName("<!italic><white>$name".asMini())
        meta.lore(descriptionWithStatus.map { "<reset><gray>$it".asMini() })
        setCustomValue(meta, plugin, "type", "modifier")
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
    private val plugin = KnockBackFFA.instance
    private val logger: Logger = plugin.logger
    private val modify = mutableMapOf<String, ModifyObject>()
    companion object {
        private val reflections: Reflections = Reflections("dev.marten_mrfcyt.knockbackffa.kits.modifiers")
        val classes = reflections.getSubTypesOf(ModifyObject::class.java)!!
    }
    init {
        try {
            for (clazz in classes) {
                clazz.kotlin.findAnnotation<Modify>()?.let { annotation ->
                    val instance = clazz.kotlin.objectInstance ?: clazz.kotlin.primaryConstructor?.call()
                    if (instance is ModifyObject) {
                        modify[annotation.id] = instance
                    }
                } ?: run {
                    logger.severe("No Modify annotation found on ${clazz.kotlin.jvmName}")
                }
            }
        } catch (e: Exception) {
            logger.severe("Error during ModifyHandler initialization: ${e.message}")
            throw e
        }
    }

    fun getModifyObjects(): Collection<ModifyObject> = modify.values

    fun handleModifier(
        player: Player,
        kitName: String,
        slot: Int,
        modifierId: String
    ) {
        println("Handling modifier")
        val config = File("${plugin.dataFolder}/kits.yml")
        val kitConfig = YamlConfiguration.loadConfiguration(config)
        val currentValue = kitConfig.getBoolean("kit.$kitName.items.$slot.modifiers.$modifierId", false)
        val modifyObject = getModifyObjects().find { it.id == modifierId }
        println("Current value: $currentValue, modifyObject: $modifyObject")
        if (modifyObject != null && modifyObject.args.isNotEmpty() && !currentValue) {
            player.closeInventory()
            player.message("Please provide the following values for the modifier:")
            modifyObject.args.forEach { arg -> player.message("${arg.first}:") }
            return
        }

        if (modifyObject != null && modifyObject.args.isNotEmpty() && currentValue) {
            modifyObject.args.forEach { arg -> kitConfig.set("kit.$kitName.items.$slot.modifiers.${arg.first}", null) }
        }

        kitConfig.set("kit.$kitName.items.$slot.modifiers.$modifierId", !currentValue)
        kitConfig.save(config)
    }

    fun handleEvent(player: Player, item: ItemStack?, args: Map<String, Any>, id: String) {
        item?.let {
            modify[id]?.takeIf { checkCustomValue(item.itemMeta ?: return, plugin, "modify", id) }
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