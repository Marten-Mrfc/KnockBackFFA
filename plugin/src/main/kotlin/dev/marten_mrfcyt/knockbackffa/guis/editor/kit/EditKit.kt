package dev.marten_mrfcyt.knockbackffa.guis.editor.kit

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import mlib.api.forms.Form
import mlib.api.forms.FormType
import mlib.api.gui.GuiSize
import mlib.api.gui.types.builder.StandardGuiBuilder
import mlib.api.utilities.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

class EditKit(private val plugin: KnockBackFFA) {

    fun kitEditor(source: Player, name: Component, lore: Component, kitName: String, new: Boolean = true) {
        if (new) {
            KnockBackFFA.kitManager.createKit(
                kitName = kitName,
                displayName = name.notMini(),
                description = lore.notMini()
            )
        }

        val kit = KnockBackFFA.kitManager.getKit(kitName)
        val inventoryTitle = "<gray>Editing:</gray><white> ".asMini().append(name)

        val gui = StandardGuiBuilder()
            .title(inventoryTitle)
            .size(GuiSize.ROW_TWO)
            .setup { standardGui ->
                standardGui.fill(Material.BLACK_STAINED_GLASS_PANE)
                standardGui.item(Material.NAME_TAG) {
                    name("<gray>Edit Display Name".asMini())
                    description(listOf("<gray>Current: <yellow>${kit.displayName}".asMini()))
                    slots(0)
                    onClick { event -> editDisplayName(event, kitName) }
                }

                standardGui.item(Material.BOOK) {
                    name("<gray>Edit Lore".asMini())
                    description(listOf("<gray>Current: <yellow>${kit.description}".asMini()))
                    slots(1)
                    onClick { event -> editLore(event, kitName) }
                }

                standardGui.item(Material.CHEST) {
                    name("<gray>Edit Items".asMini())
                    description(listOf("<gray>Change the kit's inventory items".asMini()))
                    slots(2)
                    onClick { event -> editItems(event, kitName) }
                }

                standardGui.item(Material.EXPERIENCE_BOTTLE) {
                    name("<gray>Manage Kit Boosts".asMini())
                    description(listOf(
                        "<gray>Add or remove boosts".asMini(),
                        "<gray>that are applied when".asMini(),
                        "<gray>a player selects this kit".asMini(),
                        "<gray>Active boosts: <yellow>${kit.boosts.size}".asMini()
                    ))
                    slots(3)
                    onClick { event ->
                        event.isCancelled = true
                        KitBoostManager(plugin, source, kitName).openBoostManager()
                    }
                }
                if( kit.name != "default" ) {
                    standardGui.item(Material.GOLD_INGOT) {
                        name("<gray>Edit Price".asMini())
                        description(
                            listOf(
                                "<gray>Current price: <yellow>${kit.price} coins".asMini(),
                                "".asMini(),
                                "<gray>Click to change the".asMini(),
                                "<gray>purchase price of this kit".asMini()
                            )
                        )
                        slots(4)
                        onClick { event -> editPrice(event, kitName) }
                    }
                }

                val displayItemMaterial = kit.displayIcon
                val modifiedKit = ItemStack(displayItemMaterial)
                val modifiedKitMeta = modifiedKit.itemMeta
                modifiedKitMeta.displayName(kit.displayName.asMini())
                modifiedKitMeta.lore(listOf(
                    kit.description.asMini(),
                    "<gray>------------------<reset>".asMini(),
                    "<dark_purple>Drag an item onto me".asMini(),
                    "<dark_purple>to change my DisplayIcon!".asMini()
                ))
                modifiedKit.itemMeta = modifiedKitMeta

                standardGui.item(modifiedKit.type) {
                    name(modifiedKitMeta.displayName() ?: "".asMini())
                    description(modifiedKitMeta.lore()?.map { it } ?: listOf())
                    slots(13)
                    onClick { event -> editDisplayItem(event, kitName) }
                }

                // Back button
                standardGui.item(Material.BARRIER) {
                    name("<gray>Go Back".asMini())
                    description(listOf("<gray>Return to kit selection".asMini()))
                    slots(17)
                    onClick { event -> goBack(event) }
                }
            }
            .build()

        gui.open(source)
    }

    private fun editItems(event: InventoryClickEvent, kitName: String) {
        val player = event.whoClicked as? Player ?: return
        EditKitItemSelector(plugin, player, kitName).initialize()
    }

    private fun editDisplayName(event: InventoryClickEvent, kitName: String) {
        val player = event.whoClicked as? Player ?: return
        val kit = KnockBackFFA.kitManager.getKit(kitName)

        val form = Form("Enter the new display name for kit $kitName", FormType.STRING, 30) { p, response ->
            val newName = response as String
            kit.displayName = newName
            kit.save()

            p.message("Display name updated!")
            kitEditor(p, newName.asMini(), kit.description.asMini(), kitName, false)
        }
        form.show(player)
    }

    private fun editLore(event: InventoryClickEvent, kitName: String) {
        val player = event.whoClicked as? Player ?: return
        val kit = KnockBackFFA.kitManager.getKit(kitName)

        val form = Form("Enter the new lore for kit $kitName", FormType.STRING, 30) { p, response ->
            val newLore = response as String
            kit.description = newLore
            kit.save()

            p.message("Lore updated!")
            kitEditor(p, kit.displayName.asMini(), newLore.asMini(), kitName, false)
        }
        form.show(player)
    }

    private fun editPrice(event: InventoryClickEvent, kitName: String) {
        val player = event.whoClicked as? Player ?: return
        val kit = KnockBackFFA.kitManager.getKit(kitName)

        val form = Form("Enter the new price for kit $kitName", FormType.INTEGER, 30) { p, response ->
            val newPrice = response as Int
            if (newPrice < 0) {
                p.error("Price cannot be negative!")
                return@Form
            }

            kit.price = newPrice
            kit.save()

            p.message("Kit price updated to $newPrice coins!")
            kitEditor(p, kit.displayName.asMini(), kit.description.asMini(), kitName, false)
        }
        form.show(player)
    }

    private fun goBack(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        EditKitSelector(plugin, player)
    }

    private fun editDisplayItem(event: InventoryClickEvent, kitName: String) {
        val player = event.whoClicked as? Player ?: return
        val item = event.cursor

        if (item.type == Material.AIR) {
            player.error("You must drag an item onto the display item to change it!")
            return
        }

        val kit = KnockBackFFA.kitManager.getKit(kitName)
        kit.displayIcon = item.type
        kit.save()

        player.message("Display item updated!")
        kitEditor(player, kit.displayName.asMini(), kit.description.asMini(), kitName, false)
    }
}