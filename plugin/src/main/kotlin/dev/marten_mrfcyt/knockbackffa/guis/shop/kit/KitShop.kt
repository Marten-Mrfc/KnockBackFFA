package dev.marten_mrfcyt.knockbackffa.guis.shop.kit

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.guis.shop.ShopCategorySelector
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

        val kitItems = mutableListOf<PaginatedGui.PaginatedItem>()
        val playerData = PlayerData.Companion.getInstance(plugin).getPlayerData(player.uniqueId)
        val ownedKits = KitOwnership.getOwnedKits(player.uniqueId)
        val coins = playerData.getInt("coins", 0)

        KnockBackFFA.Companion.kitManager.getAllKitNames().forEach { kitName ->
            val kit = KnockBackFFA.Companion.kitManager.getKit(kitName)

            val material = try {
                kit.displayIcon
            } catch (_: Exception) {
                Material.BARRIER
            }

            val owned = ownedKits.contains(kitName)
            val canAfford = coins >= kit.price

            val desc = mutableListOf<Component>()
            desc.add(kit.description.asMini())
            desc.add("".asMini())

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

        kitItems.forEach { item ->
            builder.addItem(item.material, item.name, item.description, item.amount)
        }

        builder.onItemClick { _, _, index ->
            val kitName = KnockBackFFA.Companion.kitManager.getAllKitNames().getOrNull(index) ?: return@onItemClick
            val owned = KitOwnership.ownsKit(player.uniqueId, kitName)

            if (owned) {
                playerData.set("kit", kitName)
                PlayerData.Companion.getInstance(plugin).savePlayerData(player.uniqueId, playerData)
                player.sendMessage("<green>You selected the kit <gold>$kitName</gold>!".asMini())
                player.closeInventory()
            } else {
                KitPurchaseConfirmation(plugin, player, kitName)
            }
        }

        builder.customizeGui { gui ->
            gui.item(Material.ARROW) {
                name("<yellow>« Back".asMini())
                slots(45)
                onClick { event ->
                    event.isCancelled = true
                    ShopCategorySelector(plugin, player)
                }
            }

            gui.item(Material.GOLD_INGOT) {
                name("<yellow>Your Coins: <gold>$coins".asMini())
                slots(49)
            }
        }

        return builder.build()
    }
}