package dev.marten_mrfcyt.knockbackffa.kits.managers

import dev.marten_mrfcyt.knockbackffa.kits.models.ModifyObject
import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.guis.editor.ItemModifierGUI
import dev.marten_mrfcyt.knockbackffa.kits.models.Kit
import dev.marten_mrfcyt.knockbackffa.kits.models.KitModifier
import mlib.api.forms.Form
import mlib.api.forms.FormType
import mlib.api.utilities.*
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.reflections.Reflections
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

class ModifierManager {
    private val plugin = KnockBackFFA.instance
    private val logger = plugin.logger
    private val modify = mutableMapOf<String, ModifyObject>()

    companion object {
        private val reflections = Reflections("dev.marten_mrfcyt.knockbackffa.kits.modifiers")
        val classes = reflections.getSubTypesOf(ModifyObject::class.java)!!
    }

    init {
        try {
            for (clazz in classes) {
                clazz.kotlin.findAnnotation<KitModifier>()?.let { annotation ->
                    val instance = clazz.kotlin.objectInstance ?: clazz.kotlin.primaryConstructor?.call()
                    if (instance is ModifyObject) {
                        modify[annotation.id] = instance
                    }
                } ?: logger.severe("No Modify annotation found on ${clazz.kotlin.simpleName}")
            }
        } catch (e: Exception) {
            logger.severe("Error during ModifyHandler initialization: ${e.message}")
            throw e
        }
    }

    fun getModifyObjects() = modify.values

    fun handleModifier(player: Player, kitName: String, slot: Int, modifierId: String) {
        val kit = KnockBackFFA.kitManager.getKit(kitName) ?: return
        val modifyObject = getModifyObjects().find { it.id == modifierId } ?: return

        val item = kit.getItem(slot) ?: return
        val currentValue = item.modifiers[modifierId] as? Boolean ?: false

        if (modifyObject.args.isNotEmpty() && !currentValue) {
            handleModifierArgs(player, kit, slot, modifyObject)
            return
        }

        if (modifyObject.args.isNotEmpty() && currentValue) {
            // Disable the modifier
            kit.setModifier(slot, modifierId, false)
        } else {
            // Toggle the modifier
            kit.setModifier(slot, modifierId, !currentValue)
        }

        // Reopen the GUI
        ItemModifierGUI(KnockBackFFA.instance, player, kitName, slot)
    }

    private fun handleModifierArgs(player: Player, kit: Kit, slot: Int, modifyObject: ModifyObject) {
        var currentArgIndex = 0
        val args = mutableMapOf<String, Any>()

        fun processNextArg() {
            if (currentArgIndex >= modifyObject.args.size) {
                // All args collected, enable the modifier with the args
                kit.setModifier(slot, modifyObject.id, true, args)

                // Reopen the GUI
                ItemModifierGUI(plugin, player, kit.name, slot)
                return
            }

            val (argName, argType) = modifyObject.args[currentArgIndex]
            val formType = when (argType) {
                Int::class.java -> FormType.INTEGER
                Boolean::class.java -> FormType.BOOLEAN
                else -> FormType.STRING
            }

            val form = Form("Enter value for $argName", formType, 30) { _, response ->
                args[argName] = response
                currentArgIndex++
                processNextArg()
            }
            player.closeInventory()
            form.show(player)
        }

        processNextArg()
    }

    fun handleEvent(player: Player, item: ItemStack?, args: Map<String, Any>, id: String) {
        item?.itemMeta?.let { meta ->
            if (checkCustomValue(meta, plugin, "modify", id)) {
                modify[id]?.handle(player, item, args)
            }
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