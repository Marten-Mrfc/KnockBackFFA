package dev.marten_mrfcyt.knockbackffa.guis.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.ModifyHandler
import dev.marten_mrfcyt.knockbackffa.utils.setCustomValue
import mlib.api.gui.Gui
import mlib.api.gui.GuiSize
import mlib.api.utilities.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.io.File

@Suppress("unused")
class ItemModifierGUI(val plugin: KnockBackFFA, val source: Player, kitName: String, slot: Int) {
    private val modifyHandler = ModifyHandler()
    private val config = File("${plugin.dataFolder}/kits.yml")
    private val kitConfig = YamlConfiguration.loadConfiguration(config)
    private val inventoryTitle = "<!italic><gray>Editing slot:</gray><white> $slot".asMini()

    init {
        println("ItemModifierGUI")
        val gui = Gui(inventoryTitle, GuiSize.ROW_TWO).apply {
            item(Material.GRAY_STAINED_GLASS_PANE) {
                name(Component.text(""))
                description(listOf(Component.text("")))
                slots(0, 1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 14, 15, 16, 17)
                onClick { event -> onGlassPaneClick(event) }
            }
            val meta = ItemStack(Material.BARRIER).itemMeta
            setCustomValue(meta, plugin, "kit_name", kitName)
            item(Material.BARRIER) {
                name("<!italic><gray>Go Back".asMini())
                description(listOf(Component.text("Go back to the previous menu")))
                slots(8)
                onClick { event -> onGoBackClick(event, kitName) }
                meta(meta)
            }
            item(Material.RED_CONCRETE) {
                name("<!italic><red>Delete Item".asMini())
                description(listOf(Component.text("Delete the item from the kit")))
                slots(7)
                onClick { event -> handleDeleteItem(event, source, kitName, slot) }
            }
        }
        loadKitItem(gui, kitName, slot)
        loadModifyObjects(gui, kitName, slot)
        println("ItemModifierGUI.open")
        gui.open(source)
    }

    private fun onGlassPaneClick(event: InventoryClickEvent) {
        event.isCancelled = true
    }

    private fun onGoBackClick(event: InventoryClickEvent, kitName: String) {
        event.isCancelled = true
        EditKitItemSelector(plugin,event.whoClicked as Player, kitName).initialize()
    }

    private fun onDeleteButtonClick(event: InventoryClickEvent) {
        (event.whoClicked as? Player)?.sendMessage("Item deleted from the kit.")
    }

    private fun loadKitItem(gui: Gui, kitName: String, slot: Int) {
        val kitItem = kitConfig.getConfigurationSection("kit.$kitName.items.$slot")
        if (kitItem != null) {
            val item = EditKitItemSelector(plugin, source, kitName).loadItemData(kitItem, kitName, true)
            val itemMeta = item?.itemMeta
            if (itemMeta != null) {
                setCustomValue(itemMeta, plugin, "slot", slot)
                setCustomValue(itemMeta, plugin, "edit_kit_item", true)
                item.itemMeta = itemMeta
            }
            gui.item(item!!.type) {
                name(item.itemMeta.displayName() ?: "".asMini())
                description(item.itemMeta.lore() ?: listOf())
                slots(13)
                onClick { event -> onGlassPaneClick(event) }
            }
        }
    }

    private fun loadModifyObjects(gui: Gui, kitName: String, slot: Int) {
        var index = 0
        modifyHandler.getModifyObjects().forEach { modifyObject ->
            val guiItem = modifyObject.createGuiItem(kitName, slot, modifyObject)
            gui.item(guiItem.type) {
                name(guiItem.itemMeta.displayName() ?: "".asMini())
                description(guiItem.itemMeta.lore() ?: listOf())
                slots(index++)
                onClick { event ->
                    changeModifier(event, kitName, slot, modifyObject.id)
                }
                meta(guiItem.itemMeta)
            }
        }
    }

    fun changeModifier(event: InventoryClickEvent, kitName: String, slot: Int, modifierId: String) {
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        ModifyHandler().handleModifier(player, kitName, slot, modifierId)
    }

    private fun handleDeleteItem(event: InventoryClickEvent, player: Player, kitName: String, slot: Int) {
        kitConfig.set("kit.$kitName.items.$slot", null)
        kitConfig.save(config)
        EditKitItemSelector(plugin, player, kitName).initialize()
    }

}