package dev.marten_mrfcyt.knockbackffa.guis.shop.boosts

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import mlib.api.gui.types.builder.ConfirmationGuiBuilder
import mlib.api.utilities.asMini
import mlib.api.utilities.message
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

        val playerData = PlayerData.getInstance(plugin).getPlayerData(player.uniqueId)
        val coins = playerData.getInt("coins", 0)
        val canAfford = coins >= boost.price

        val message = mutableListOf<Component>(
            TranslationManager.translate("shop.boosts.confirmation.title", "name" to boost.name).asMini(),
            TranslationManager.translate("shop.boosts.price", "price" to boost.price).asMini(),
            "".asMini(),
            if (canAfford) {
                TranslationManager.translate("shop.boosts.confirmation.balance_sufficient", "coins" to coins).asMini()
            } else {
                TranslationManager.translate("shop.boosts.confirmation.balance_insufficient", "coins" to coins).asMini()
            }
        )

        if (boost.getDuration() != null) {
            message.add("".asMini())
            message.add(TranslationManager.translate("shop.boosts.duration", "duration" to formatDuration(boost.getDuration()!!)).asMini())
        }

        if (!canAfford) {
            message.add("".asMini())
            message.add(TranslationManager.translate("shop.boosts.not_enough_coins").asMini())
        }

        val confirmBuilder = ConfirmationGuiBuilder()
            .title(TranslationManager.translate("shop.boosts.confirmation.gui_title").asMini())
            .message(message)
            .confirmEnabled(canAfford)
            .confirmText(TranslationManager.translate("shop.boosts.confirmation.confirm", "price" to boost.price).asMini())
            .cancelText(TranslationManager.translate("shop.boosts.confirmation.cancel").asMini())
            .onConfirm(Consumer { p ->
                val data = PlayerData.getInstance(plugin).getPlayerData(p.uniqueId)
                val currentCoins = data.getInt("coins", 0)

                if (currentCoins < boost.price) {
                    p.message(TranslationManager.translate("shop.boosts.not_enough_coins"))
                    BoostShop(plugin, p)
                    return@Consumer
                }

                data.set("coins", currentCoins - boost.price)
                PlayerData.getInstance(plugin).savePlayerData(p.uniqueId, data)

                plugin.playerBoostManager.addBoost(p.uniqueId, boostId, boost.getDuration() ?: java.time.Duration.ZERO)

                p.message(TranslationManager.translate("shop.boosts.confirmation.success", "name" to boost.name))
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
            TranslationManager.translate("shop.boosts.duration_format.hours", "hours" to hours, "minutes" to minutes)
        } else {
            TranslationManager.translate("shop.boosts.duration_format.minutes", "minutes" to minutes)
        }
    }
}