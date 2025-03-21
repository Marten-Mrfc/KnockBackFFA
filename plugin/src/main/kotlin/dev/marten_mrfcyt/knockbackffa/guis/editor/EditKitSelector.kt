// src/main/kotlin/dev/marten_mrfcyt/knockbackffa/guis/editor/EditKitSelector.kt
package dev.marten_mrfcyt.knockbackffa.guis.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import mlib.api.gui.GuiSize
import mlib.api.gui.types.builder.StandardGuiBuilder
import mlib.api.utilities.*
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import kotlin.math.ceil

class EditKitSelector(private val plugin: KnockBackFFA, source: CommandSender) {
    private val kitNames = KnockBackFFA.kitManager.getAllKitNames()
    private val kitCount = kitNames.size
    private val inventorySize = ceil((kitCount + 1) / 9.0).toInt() * 9

    init {
        if (source is Player) {
            val guiSize = GuiSize.fromRows(inventorySize / 9)

            val gui = StandardGuiBuilder()
                .title("<gray>Please select or create kit</gray>".asMini())
                .size(guiSize)
                .setup { standardGui ->
                    kitNames.forEachIndexed { index, kitName ->
                        val item = loadKitGUI(kitName)
                        standardGui.item(item.type) {
                            name(item.itemMeta?.displayName() ?: "".asMini())
                            description(item.itemMeta?.lore() ?: listOf())
                            slots(index)
                            onClick { event -> openKitEditor(event) }
                            meta(item.itemMeta)
                        }
                    }
                }
                .build()

            gui.open(source)
        } else {
            source.error("You must be a player to use this command!")
        }
    }

    private fun loadKitGUI(kitName: String): ItemStack {
        val kit = KnockBackFFA.kitManager.getKit(kitName) ?: return ItemStack(Material.BARRIER)

        val item = ItemStack(kit.displayIcon)
        val meta = item.itemMeta ?: return ItemStack(Material.BARRIER)

        meta.displayName("<!italic>${kit.displayName}".asMini())
        meta.lore(listOf(kit.description.asMini()))
        setCustomValue(meta, plugin, "kit_name", kitName)
        item.itemMeta = meta

        return item
    }

    private fun openKitEditor(event: InventoryClickEvent) {
        val item = event.currentItem ?: return
        val player = event.whoClicked as? Player ?: return
        val kitName = getCustomValue(item.itemMeta, plugin, "kit_name") as String
        val kit = KnockBackFFA.kitManager.getKit(kitName) ?: return

        EditKit(plugin).kitEditor(
            player,
            kit.displayName.asMini(),
            kit.description.asMini(),
            kitName,
            new = false
        )
    }
}