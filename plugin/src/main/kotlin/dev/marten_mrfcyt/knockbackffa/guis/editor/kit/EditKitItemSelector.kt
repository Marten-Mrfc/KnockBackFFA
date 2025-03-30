package dev.marten_mrfcyt.knockbackffa.guis.editor.kit

import KitItem
import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import mlib.api.gui.GuiSize
import mlib.api.gui.types.StandardGui
import mlib.api.gui.types.builder.StandardGuiBuilder
import mlib.api.utilities.*
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta

class EditKitItemSelector(private val plugin: KnockBackFFA, private val source: Player, private val kitName: String) {
    private val inventoryTitle = "<!italic><gray>Editing kit:</gray><white> $kitName".asMini()

    fun initialize() {
        val gui = StandardGuiBuilder()
            .title(inventoryTitle)
            .size(GuiSize.ROW_TWO)
            .setup { standardGui ->
                standardGui.item(Material.GRAY_STAINED_GLASS_PANE) {
                    name("<gray>Click to edit slot</gray>".asMini())
                    description(listOf("<dark_purple>Drag an item onto me".asMini(), "<dark_purple>to change me completely!".asMini()))
                    slots(0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17)
                    onClick { event -> addNewItem(event, kitName) }
                }

                loadKitItems(standardGui, kitName)

                standardGui.item(Material.BARRIER) {
                    name("<!italic><gray>Go Back".asMini())
                    description(listOf("Go back to the previous menu".asMini()))
                    slots(8)
                    onClick { event -> onGoBackClick(event, kitName) }
                }
            }
            .build()

        gui.open(source)
    }

    private fun loadKitItems(gui: StandardGui, kitName: String) {
        val kit = KnockBackFFA.kitManager.getKit(kitName)

        kit.items.forEach { (slot, kitItem) ->
            val item = kitItem.build(plugin)
            val itemMeta = item.itemMeta

            setCustomValue(itemMeta, plugin, "type", "edit_kit_item")
            setCustomValue(itemMeta, plugin, "kit_name", kitName)
            setCustomValue(itemMeta, plugin, "slot", slot)
            item.itemMeta = itemMeta

            val adjustedSlot = when (slot) {
                in 9..18 -> slot - 9
                in 0..8 -> slot + 9
                else -> return@forEach
            }

            gui.item(item.type) {
                name(itemMeta.displayName() ?: "".asMini())
                description(itemMeta.lore()?.map { it } ?: listOf())
                amount(item.amount)
                slots(adjustedSlot)
                onClick { event -> onItemClick(event, kitName) }
                meta(itemMeta)
            }
        }
    }

    private fun onItemClick(event: InventoryClickEvent, kitName: String) {
        val item = event.currentItem ?: return
        val slot = getCustomValue(item.itemMeta, plugin, "slot") as Int
        val player = event.whoClicked as Player
        ItemModifierGUI(plugin, player, kitName, slot)
    }

    private fun onGoBackClick(event: InventoryClickEvent, kitName: String) {
        val kit = KnockBackFFA.kitManager.getKit(kitName)
        (event.whoClicked as? Player)?.apply {
            EditKit(plugin).kitEditor(this, kit.displayName.asMini(),
                kit.description.asMini(), kitName, new = false)
        }
    }
private fun addNewItem(event: InventoryClickEvent, kitName: String) {
    val player = event.whoClicked as? Player ?: return
    val clickedItem = event.cursor

    if (clickedItem.type == Material.AIR) {
        return
    }

    val slot = event.slot
    event.isCancelled = true

    val adjustedSlot = when (slot) {
        in 9..18 -> slot - 9
        in 0..8 -> slot + 9
        else -> return
    }

    val kit = KnockBackFFA.kitManager.getKit(kitName)

    val meta = clickedItem.itemMeta

    val itemName = meta?.displayName()?.notMini() ?: ""
    val itemLore = meta?.lore()?.map { it.notMini() } ?: listOf()

    val kitItem = KitItem(
        name = itemName,
        material = clickedItem.type.name,
        amount = clickedItem.amount,
        lore = itemLore,
        enchantments = meta?.enchants?.mapKeys { it.key.key.key } ?: emptyMap(),
        metadata = extractMetadata(meta),
        modifiers = emptyMap(),
        kitName = kitName,
        slot = adjustedSlot
    )

    kit.setItem(adjustedSlot, kitItem)
    kit.save()

    player.message("Item added to the kit in slot $adjustedSlot.")
    initialize()
}

private fun extractMetadata(meta: ItemMeta?): Map<String, Any> {
    val metadata = mutableMapOf<String, Any>()

    if (meta == null) return metadata

    if (meta.hasCustomModelData()) {
        metadata["model"] = meta.customModelData
    }

    if (meta.isUnbreakable) {
        metadata["unbreakable"] = true
    }

    if (meta is Damageable) {
        metadata["durability"] = meta.damage
    }

    if (meta.itemFlags.isNotEmpty()) {
        metadata["itemFlags"] = meta.itemFlags.map { it.name }
    }

    return metadata
}
}