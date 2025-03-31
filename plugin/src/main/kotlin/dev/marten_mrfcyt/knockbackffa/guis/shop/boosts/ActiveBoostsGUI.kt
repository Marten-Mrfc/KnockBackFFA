package dev.marten_mrfcyt.knockbackffa.guis.shop.boosts

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import mlib.api.gui.GuiSize
import mlib.api.gui.types.StandardGui
import mlib.api.utilities.asMini
import org.bukkit.Material
import org.bukkit.entity.Player
import java.time.Duration

class ActiveBoostsGUI(private val plugin: KnockBackFFA, private val player: Player) {
    private val gui: StandardGui = StandardGui(TranslationManager.translate("shop.boosts.active_gui.title").asMini(), GuiSize.ROW_THREE)

    init {
        setupGui()
        gui.open(player)
    }

    private fun setupGui() {
        gui.fill(Material.BLACK_STAINED_GLASS_PANE) {}

        val activeBoosts = plugin.playerBoostManager.getActiveBoosts(player.uniqueId)
        val activeKitBoosts = plugin.playerBoostManager.getActiveKitBoosts(player.uniqueId)
        if (activeBoosts.isEmpty() && activeKitBoosts.isEmpty()) {
            gui.item(Material.BARRIER) {
                name(TranslationManager.translate("shop.boosts.active_gui.no_boosts.title").asMini())
                description(TranslationManager.getStringList("shop.boosts.active_gui.no_boosts.description").map { it.asMini() })
                slots(13)
            }
        } else {
            var slot = 10
            activeBoosts.forEach { playerBoost ->
                val boost = plugin.boostManager.getBoost(playerBoost.boostId)
                if (activeKitBoosts.any { it.id == playerBoost.boostId }) {
                    return@forEach
                }
                val remainingTime = plugin.playerBoostManager.getRemainingTime(player.uniqueId, boost.id)
                val hours = remainingTime.toHours()
                val minutes = remainingTime.toMinutes() % 60
                val seconds = remainingTime.seconds % 60

                val timeStr = if (hours > 0) {
                    TranslationManager.translate("shop.boosts.active_gui.time_format.hours",
                        "hours" to hours, "minutes" to minutes, "seconds" to seconds)
                } else if (minutes > 0) {
                    TranslationManager.translate("shop.boosts.active_gui.time_format.minutes",
                        "minutes" to minutes, "seconds" to seconds)
                } else {
                    TranslationManager.translate("shop.boosts.active_gui.time_format.seconds",
                        "seconds" to seconds)
                }

                gui.item(boost.icon) {
                    name(TranslationManager.translate("shop.boosts.active_gui.boost_name", "name" to boost.name).asMini())
                    description(buildList {
                        boost.description.forEach { line ->
                            add(TranslationManager.translate("shop.boosts.active_gui.boost_description_line", "line" to line).asMini())
                        }
                        add("".asMini())
                        add(TranslationManager.translate("shop.boosts.active_gui.time_remaining", "time" to timeStr).asMini())
                    })
                    slots(slot)
                }

                slot++
                if (slot == 17) slot = 19
                if (slot > 25) return@forEach
            }
            activeKitBoosts.forEach { playerBoost ->
                gui.item(playerBoost.icon) {
                    name(TranslationManager.translate("shop.boosts.active_gui.boost_name", "name" to playerBoost.name).asMini())
                    description(buildList {
                        playerBoost.description.forEach { line ->
                            add(TranslationManager.translate("shop.boosts.active_gui.boost_description_line", "line" to line).asMini())
                        }
                        add("".asMini())
                        add(TranslationManager.translate("shop.boosts.active_gui.kit_boost").asMini())
                    })
                    slots(slot)
                }

                slot++
                if (slot == 17) slot = 19
                if (slot > 25) return@forEach
            }
        }

        gui.item(Material.ARROW) {
            name(TranslationManager.translate("shop.boosts.active_gui.back").asMini())
            slots(22)
            onClick { event ->
                event.isCancelled = true
                BoostShop(plugin, player)
            }
        }
    }
}