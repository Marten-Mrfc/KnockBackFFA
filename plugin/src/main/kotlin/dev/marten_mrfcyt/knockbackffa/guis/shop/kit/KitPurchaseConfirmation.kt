package dev.marten_mrfcyt.knockbackffa.guis.shop.kit

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.KitOwnership
import dev.marten_mrfcyt.knockbackffa.kits.models.Kit
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import mlib.api.gui.types.builder.ConfirmationGuiBuilder
import mlib.api.utilities.asMini
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.function.Consumer

class KitPurchaseConfirmation(
    private val plugin: KnockBackFFA,
    private val player: Player,
    private val kitName: String
) {
    fun getKit(): Kit = KnockBackFFA.Companion.kitManager.getKit(kitName)
    init {
        val kit = getKit()
        val playerData = PlayerData.Companion.getInstance(plugin).getPlayerData(player.uniqueId)
        val coins = playerData.getInt("coins", 0)
        val canAfford = coins >= kit.price
        var message = mutableListOf(
            "<yellow>Purchase Kit: <gold>${kit.displayName}".asMini(),
            "<gray>Price: <gold>${kit.price} coins".asMini(),
            "".asMini(),
            if (canAfford) {
                "<gray>Your balance: <green>$coins coins".asMini()
            } else {
                "<gray>Your balance: <red>$coins coins".asMini()
            }
        )

        if (!canAfford) {
            message.add("".asMini())
            message.add("<red>You don't have enough coins to buy this kit!".asMini())
        }

        val confirmBuilder = ConfirmationGuiBuilder()
            .title("<dark_gray>Shop <gray>» <white>Confirm Purchase".asMini())
            .message(message)
            .confirmEnabled(canAfford)
            .confirmText("<green>Purchase for ${kit.price} coins".asMini())
            .cancelText("<red>Cancel".asMini())
            .onConfirm(Consumer { p ->
                if (KitOwnership.buyKit(p, kitName)) {
                    p.sendMessage("<green>You purchased the kit <gold>${kit.displayName}</gold>!".asMini())

                    val data = PlayerData.Companion.getInstance(plugin).getPlayerData(p.uniqueId)
                    data.set("kit", kitName)
                    PlayerData.Companion.getInstance(plugin).savePlayerData(p.uniqueId, data)

                    p.closeInventory()
                } else {
                    p.sendMessage("<red>Failed to purchase the kit!".asMini())
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