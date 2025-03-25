// src/main/kotlin/dev/marten_mrfcyt/knockbackffa/guis/shop/KitShop.kt
package dev.marten_mrfcyt.knockbackffa.guis.shop

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.KitOwnership
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import mlib.api.gui.GuiSize
import mlib.api.gui.types.PaginatedGui
import mlib.api.gui.types.builder.PaginatedGuiBuilder
import mlib.api.utilities.asMini
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

class KitShop(private val plugin: KnockBackFFA, private val player: Player) {

    init {
        val gui = createGui()
        gui.open(player)
    }

    private fun createGui(): PaginatedGui {
        val builder = PaginatedGuiBuilder()
            .title("<dark_gray>Shop <gray>» <white>Kits".asMini())
            .size(GuiSize.ROW_SIX)
            .setBackground(Material.BLACK_STAINED_GLASS_PANE)

        // Load all kits
        val kitItems = mutableListOf<PaginatedGui.PaginatedItem>()
        val playerData = PlayerData.getInstance(plugin).getPlayerData(player.uniqueId)
        val ownedKits = KitOwnership.getOwnedKits(player.uniqueId)
        val coins = playerData.getInt("coins", 0)

        // Add all kits to the shop
        KnockBackFFA.kitManager.getAllKitNames().forEach { kitName ->
            val kit = KnockBackFFA.kitManager.getKit(kitName)

            val material = try {
                kit.displayIcon
            } catch (_: Exception) {
                Material.BARRIER
            }

            val owned = ownedKits.contains(kitName)
            val canAfford = coins >= kit.price

            val desc = mutableListOf<Component>()
            // Add description
            desc.add(kit.description.asMini())
            desc.add("".asMini())

            // Add status
            if (owned) {
                desc.add("<green>You own this kit".asMini())
                desc.add("<yellow>Click to select!".asMini())
            } else {
                desc.add("<gray>Price: <gold>${kit.price} coins".asMini())
                if (canAfford) {
                    desc.add("<yellow>Click to purchase!".asMini())
                } else {
                    desc.add("<red>You don't have enough coins!".asMini())
                }
            }

            kitItems.add(
                PaginatedGui.PaginatedItem(
                    material,
                    kit.displayName.asMini(),
                    desc.map { it },
                    1
                )
            )
        }

        // Add all items to the builder
        kitItems.forEach { item ->
            builder.addItem(item.material, item.name, item.description, item.amount)
        }

        // Handle clicking on a kit
        builder.onItemClick { clickedPlayer, itemStack, index ->
            val kitName = KnockBackFFA.kitManager.getAllKitNames().getOrNull(index) ?: return@onItemClick
            val owned = KitOwnership.ownsKit(player.uniqueId, kitName)

            if (owned) {
                // Select the kit
                val playerData = PlayerData.getInstance(plugin).getPlayerData(player.uniqueId)
                playerData.set("kit", kitName)
                PlayerData.getInstance(plugin).savePlayerData(player.uniqueId, playerData)
                player.sendMessage("<green>You selected the kit <gold>$kitName</gold>!".asMini())
                player.closeInventory()
            } else {
                // Show purchase confirmation
                KitPurchaseConfirmation(plugin, player, kitName)
            }
        }

        // Add back button
        builder.customizeGui { gui ->
            gui.item(Material.ARROW) {
                name("<yellow>« Back".asMini())
                slots(45) // Bottom left corner
                onClick { event ->
                    event.isCancelled = true
                    ShopCategorySelector(plugin, player)
                }
            }

            // Show player's coins
            gui.item(Material.GOLD_INGOT) {
                name("<yellow>Your Coins: <gold>$coins".asMini())
                slots(49) // Bottom middle
            }
        }

        return builder.build()
    }
}