package dev.marten_mrfcyt.knockbackffa.guis.shop.boosts

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.guis.shop.ShopCategorySelector
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import mlib.api.gui.GuiSize
import mlib.api.gui.types.PaginatedGui
import mlib.api.gui.types.builder.PaginatedGuiBuilder
import mlib.api.utilities.asMini
import mlib.api.utilities.message
import net.kyori.adventure.text.Component
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
            .title(TranslationManager.translate("shop.boosts.title").asMini())
            .size(GuiSize.ROW_SIX)
            .setBackground(Material.BLACK_STAINED_GLASS_PANE)

        val playerData = PlayerData.getInstance(plugin).getPlayerData(player.uniqueId)
        val coins = playerData.getInt("coins", 0)

        val boostItems = mutableListOf<PaginatedGui.PaginatedItem>()
        plugin.boostManager.getAllBoosts().forEach { boost ->
            val canAfford = coins >= boost.price
            val desc = mutableListOf<Component>()
            boost.description.forEach { line ->
                desc.add(
                    TranslationManager.translate("shop.boosts.description", "description" to line)
                        .asMini()
                )
            }
            desc.add("".asMini())
            if (boost.getDuration() != null) {
                desc.add(TranslationManager.translate("shop.boosts.duration", "duration" to formatDuration(boost.getDuration()!!)).asMini())
            } else {
                desc.add("".asMini())
            }

            desc.add(TranslationManager.translate("shop.boosts.price", "price" to boost.price).asMini())

            if (plugin.playerBoostManager.hasActiveBoost(player.uniqueId, boost.id)) {
                val remainingTime = plugin.playerBoostManager.getRemainingTime(player.uniqueId, boost.id)
                val minutes = remainingTime.toMinutes()
                val seconds = remainingTime.seconds % 60
                desc.add(TranslationManager.translate("shop.boosts.active", "minutes" to minutes, "seconds" to seconds).asMini())
            } else if (canAfford) {
                desc.add(TranslationManager.translate("shop.boosts.click_to_purchase").asMini())
            } else {
                desc.add(TranslationManager.translate("shop.kits.not_enough_coins").asMini())
            }

            boostItems.add(
                PaginatedGui.PaginatedItem(
                    boost.icon,
                    TranslationManager.translate("shop.boosts.item_name", "name" to boost.name).asMini(),
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
                player.message(TranslationManager.translate("shop.boosts.already_active"))
                return@onItemClick
            }

            BoostPurchaseConfirmation(plugin, player, boost.id)
        }

        builder.customizeGui { gui ->
            gui.item(Material.ARROW) {
                name(TranslationManager.translate("shop.common.back").asMini())
                slots(45)
                onClick { event ->
                    event.isCancelled = true
                    ShopCategorySelector(plugin, player)
                }
            }

            gui.item(Material.GOLD_INGOT) {
                name(TranslationManager.translate("shop.common.your_coins", "coins" to coins).asMini())
                slots(49)
                onClick {  event ->
                    event.isCancelled = true
                }
            }

            gui.item(Material.EXPERIENCE_BOTTLE) {
                name(TranslationManager.translate("shop.boosts.active_boosts.name").asMini())
                description(listOf(
                    TranslationManager.translate("shop.boosts.active_boosts.description").asMini(),
                    "".asMini(),
                    TranslationManager.translate("shop.boosts.active_boosts.click").asMini()
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
            TranslationManager.translate("shop.boosts.duration_format.hours", "hours" to hours, "minutes" to minutes)
        } else {
            TranslationManager.translate("shop.boosts.duration_format.minutes", "minutes" to minutes)
        }
    }
}