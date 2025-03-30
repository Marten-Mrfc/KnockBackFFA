package dev.marten_mrfcyt.knockbackffa.guis.shop.boosts

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import mlib.api.gui.types.builder.ConfirmationGuiBuilder
import mlib.api.utilities.asMini
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.function.Consumer

class BoostPurchaseConfirmation(
    private val plugin: KnockBackFFA,
    player: Player,
    private val boostId: String
) {
    init {
        val boost = plugin.boostManager.getBoost(boostId)

        val playerData = PlayerData.Companion.getInstance(plugin).getPlayerData(player.uniqueId)
        val coins = playerData.getInt("coins", 0)
        val canAfford = coins >= boost.price

        val message = mutableListOf<Component>(
            "<yellow>Purchase Boost: <gold>${boost.name}".asMini(),
            "<gray>Price: <gold>${boost.price} coins".asMini(),
            "".asMini(),
            if (canAfford) {
                "<gray>Your balance: <green>$coins coins".asMini()
            } else {
                "<gray>Your balance: <red>$coins coins".asMini()
            },
            if (boost.getDuration() != null) {
                "".asMini()
                "<gray>Duration: <white>${formatDuration(boost.getDuration()!!)}".asMini()
            } else {
                "".asMini()
            }
        )

        if (!canAfford) {
            message.add("".asMini())
            message.add("<red>You don't have enough coins to buy this boost!".asMini())
        }

        val confirmBuilder = ConfirmationGuiBuilder()
            .title("<dark_gray>Shop <gray>» <white>Confirm Purchase".asMini())
            .message(message)
            .confirmEnabled(canAfford)
            .confirmText("<green>Purchase for ${boost.price} coins".asMini())
            .cancelText("<red>Cancel".asMini())
            .onConfirm(Consumer { p ->
                val data = PlayerData.Companion.getInstance(plugin).getPlayerData(p.uniqueId)
                val currentCoins = data.getInt("coins", 0)

                if (currentCoins < boost.price) {
                    p.sendMessage("<red>You don't have enough coins!".asMini())
                    BoostShop(plugin, p)
                    return@Consumer
                }

                data.set("coins", currentCoins - boost.price)
                PlayerData.Companion.getInstance(plugin).savePlayerData(p.uniqueId, data)

                plugin.playerBoostManager.addBoost(p.uniqueId, boostId, boost.getDuration() ?: java.time.Duration.ZERO)

                p.sendMessage("<green>You purchased the ${boost.name} <green>boost!".asMini())
                p.closeInventory()
            })
            .onCancel(Consumer { p ->
                BoostShop(plugin, p)
            })

        val gui = confirmBuilder.build()
        gui.open(player)
    }

    private fun formatDuration(duration: java.time.Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60

        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}