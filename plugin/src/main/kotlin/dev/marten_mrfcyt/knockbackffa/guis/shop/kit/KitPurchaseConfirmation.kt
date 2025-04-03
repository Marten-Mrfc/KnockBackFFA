package dev.marten_mrfcyt.knockbackffa.guis.shop.kit

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.KitOwnership
import dev.marten_mrfcyt.knockbackffa.kits.models.Kit
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import mlib.api.gui.types.builder.ConfirmationGuiBuilder
import mlib.api.utilities.asMini
import mlib.api.utilities.message
import org.bukkit.entity.Player
import java.util.function.Consumer

class KitPurchaseConfirmation(
    private val plugin: KnockBackFFA,
    private val player: Player,
    private val kitName: String
) {
    fun getKit(): Kit = KnockBackFFA.kitManager.getKit(kitName)

    init {
        val kit = getKit()
        val playerData = PlayerData.getInstance(plugin).getPlayerData(player.uniqueId)
        val coins = playerData.getInt("coins", 0)
        val canAfford = coins >= kit.price

        val message = mutableListOf(
            TranslationManager.translate("shop.kits.confirmation.title", "kit_name" to kit.displayName).asMini(),
            TranslationManager.translate("shop.kits.confirmation.price", "price" to kit.price).asMini(),
            "".asMini(),
            if (canAfford) {
                TranslationManager.translate("shop.kits.confirmation.balance_sufficient", "coins" to coins).asMini()
            } else {
                TranslationManager.translate("shop.kits.confirmation.balance_insufficient", "coins" to coins).asMini()
            }
        )

        if (!canAfford) {
            message.add("".asMini())
            message.add(TranslationManager.translate("shop.kits.not_enough_coins").asMini())
        }

        val confirmBuilder = ConfirmationGuiBuilder()
            .title(TranslationManager.translate("shop.kits.confirmation.gui_title").asMini())
            .message(message)
            .confirmEnabled(canAfford)
            .confirmText(TranslationManager.translate("shop.kits.confirmation.confirm", "price" to kit.price).asMini())
            .cancelText(TranslationManager.translate("shop.kits.confirmation.cancel").asMini())
            .onConfirm(Consumer { p ->
                if (KitOwnership.buyKit(p, kitName)) {
                    p.message(TranslationManager.translate("shop.kits.confirmation.success", "kit_name" to kit.displayName))

                    val data = PlayerData.getInstance(plugin).getPlayerData(p.uniqueId)
                    data.set("kit", kitName)
                    PlayerData.getInstance(plugin).savePlayerData(p.uniqueId, data)

                    p.closeInventory()
                } else {
                    p.message(TranslationManager.translate("shop.kits.confirmation.failed"))
                    KitShop(plugin, p)
                }
            })
            .onCancel(Consumer { p ->
                KitShop(plugin, p)
            })

        val gui = confirmBuilder.build()
        gui.open(player)
    }
}