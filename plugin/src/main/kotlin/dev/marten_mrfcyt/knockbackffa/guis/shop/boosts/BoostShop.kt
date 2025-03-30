package dev.marten_mrfcyt.knockbackffa.guis.shop.boosts

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.guis.shop.ShopCategorySelector
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import mlib.api.gui.GuiSize
import mlib.api.gui.types.PaginatedGui
import mlib.api.gui.types.builder.PaginatedGuiBuilder
import mlib.api.utilities.asMini
import org.bukkit.Material
import org.bukkit.entity.Player
import java.time.Duration

class BoostShop(private val plugin: KnockBackFFA, private val player: Player) {

    init {
        val gui = createGui()
        gui.open(player)
    }

    private fun createGui(): PaginatedGui {
        val builder = PaginatedGuiBuilder()
            .title("<dark_gray>Shop <gray>» <white>Boosts".asMini())
            .size(GuiSize.ROW_SIX)
            .setBackground(Material.BLACK_STAINED_GLASS_PANE)

        val playerData = PlayerData.Companion.getInstance(plugin).getPlayerData(player.uniqueId)
        val coins = playerData.getInt("coins", 0)

        val boostItems = mutableListOf<PaginatedGui.PaginatedItem>()
        plugin.boostManager.getAllBoosts().forEach { boost ->
            val canAfford = coins >= boost.price

            val desc = mutableListOf(
                "<gray>${boost.description.joinToString(" ")}".asMini(),
                "".asMini(),
                if (boost.getDuration() != null) {
                    "<gray>Duration: <white>${formatDuration(boost.getDuration()!!)}".asMini()
                } else {
                    "".asMini()
                },
                "<gray>Price: <gold>${boost.price} coins".asMini(),
            )

            if (plugin.playerBoostManager.hasActiveBoost(player.uniqueId, boost.id)) {
                val remainingTime = plugin.playerBoostManager.getRemainingTime(player.uniqueId, boost.id)
                val minutes = remainingTime.toMinutes()
                val seconds = remainingTime.seconds % 60
                desc.add("<green>Active! <gray>Time left: <white>${minutes}m ${seconds}s".asMini())
            } else if (canAfford) {
                desc.add("<yellow>Click to purchase!".asMini())
            } else {
                desc.add("<red>You don't have enough coins!".asMini())
            }

            boostItems.add(
                PaginatedGui.PaginatedItem(
                    boost.icon,
                    "<yellow>${boost.name}".asMini(),
                    desc,
                    1
                )
            )
        }

        boostItems.forEach { item ->
            builder.addItem(item.material, item.name, item.description, item.amount)
        }

        builder.onItemClick { _, _, index ->
            val boosts = plugin.boostManager.getAllBoosts().toList()
            val boost = boosts.getOrNull(index) ?: return@onItemClick

            val hasActive = plugin.playerBoostManager.hasActiveBoost(player.uniqueId, boost.id)
            if (hasActive) {
                player.sendMessage("<yellow>You already have this boost active!".asMini())
                return@onItemClick
            }

            BoostPurchaseConfirmation(plugin, player, boost.id)
        }

        builder.customizeGui { gui ->
            gui.item(Material.ARROW) {
                name("<yellow>« Back".asMini())
                slots(45) // Bottom left corner
                onClick { event ->
                    event.isCancelled = true
                    ShopCategorySelector(plugin, player)
                }
            }

            gui.item(Material.GOLD_INGOT) {
                name("<yellow>Your Coins: <gold>$coins".asMini())
                slots(49) // Bottom middle
            }

            gui.item(Material.EXPERIENCE_BOTTLE) {
                name("<yellow>Your Active Boosts".asMini())
                description(listOf(
                    "<gray>View your active boosts".asMini(),
                    "".asMini(),
                    "<white>Click to view!".asMini()
                ))
                slots(53)
                onClick { event ->
                    event.isCancelled = true
                    ActiveBoostsGUI(plugin, player)
                }
            }
        }

        return builder.build()
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60

        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}