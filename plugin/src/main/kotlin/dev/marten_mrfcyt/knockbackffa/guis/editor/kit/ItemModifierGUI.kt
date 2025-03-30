package dev.marten_mrfcyt.knockbackffa.guis.editor.kit

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import mlib.api.gui.GuiSize
import mlib.api.gui.types.StandardGui
import mlib.api.gui.types.builder.StandardGuiBuilder
import mlib.api.utilities.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

class ItemModifierGUI(val plugin: KnockBackFFA, val source: Player, kitName: String, slot: Int) {
    private val modifierManager = KnockBackFFA.instance.modifierManager
    private val inventoryTitle = "<!italic><gray>Editing slot:</gray><white> $slot".asMini()

    init {
        val gui = StandardGuiBuilder()
            .title(inventoryTitle)
            .size(GuiSize.ROW_TWO)
            .setup { standardGui ->
                standardGui.item(Material.GRAY_STAINED_GLASS_PANE) {
                    name(Component.text(""))
                    description(listOf(Component.text("")))
                    slots(0, 1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 14, 15, 16, 17)
                    onClick { event -> event.isCancelled = true }
                }

                val meta = ItemStack(Material.BARRIER).itemMeta
                setCustomValue(meta, plugin, "kit_name", kitName)
                standardGui.item(Material.BARRIER) {
                    name("<!italic><gray>Go Back".asMini())
                    description(listOf(Component.text("Go back to the previous menu")))
                    slots(8)
                    onClick { event -> onGoBackClick(event, kitName) }
                    meta(meta)
                }

                standardGui.item(Material.RED_CONCRETE) {
                    name("<!italic><red>Delete Item".asMini())
                    description(listOf(Component.text("Delete the item from the kit")))
                    slots(7)
                    onClick { event -> handleDeleteItem(event, source, kitName, slot) }
                }

                loadKitItem(standardGui, kitName, slot)

                loadModifyObjects(standardGui, kitName, slot)
            }
            .build()

        gui.open(source)
    }

    private fun onGoBackClick(event: InventoryClickEvent, kitName: String) {
        event.isCancelled = true
        EditKitItemSelector(plugin, event.whoClicked as Player, kitName).initialize()
    }

    private fun loadKitItem(gui: StandardGui, kitName: String, slot: Int) {
        val kit = KnockBackFFA.kitManager.getKit(kitName)
        val kitItem = kit.getItem(slot) ?: return

        val item = kitItem.build(plugin)
        val itemMeta = item.itemMeta

        gui.item(item.type) {
            name(itemMeta.displayName() ?: "".asMini())
            description(itemMeta.lore() ?: listOf())
            slots(13)
            onClick { event -> event.isCancelled = true }
            meta(itemMeta)
        }
    }

    private fun loadModifyObjects(gui: StandardGui, kitName: String, slot: Int) {
        var index = 0
        modifierManager.getModifyObjects().forEach { modifyObject ->
            val guiItem = modifyObject.createGuiItem(kitName, slot, modifyObject)
            gui.item(guiItem.type) {
                name(guiItem.itemMeta.displayName() ?: "".asMini())
                description(guiItem.itemMeta.lore() ?: listOf())
                slots(index++)
                onClick { event ->
                    event.isCancelled = true
                    modifierManager.handleModifier(event.whoClicked as Player, kitName, slot, modifyObject.id)
                }
                meta(guiItem.itemMeta)
            }
        }
    }

    private fun handleDeleteItem(event: InventoryClickEvent, player: Player, kitName: String, slot: Int) {
        event.isCancelled = true
        val kit = KnockBackFFA.kitManager.getKit(kitName)
        kit.removeItem(slot)
        kit.save()
        EditKitItemSelector(plugin, player, kitName).initialize()
    }
}