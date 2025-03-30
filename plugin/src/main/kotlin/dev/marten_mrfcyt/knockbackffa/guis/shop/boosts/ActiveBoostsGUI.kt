package dev.marten_mrfcyt.knockbackffa.guis.shop.boosts

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import mlib.api.gui.GuiSize
import mlib.api.gui.types.StandardGui
import mlib.api.utilities.asMini
import org.bukkit.Material
import org.bukkit.entity.Player
import java.time.Duration

class ActiveBoostsGUI(private val plugin: KnockBackFFA, private val player: Player) {
    private val gui: StandardGui = StandardGui("<dark_gray>Boosts <gray>» <white>Active Boosts".asMini(), GuiSize.ROW_THREE)

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
                name("<red>No Active Boosts".asMini())
                description(listOf(
                    "<gray>You don't have any active boosts.".asMini(),
                    "<gray>Visit the shop to purchase boosts!".asMini()
                ))
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
                    "${hours}h ${minutes}m ${seconds}s"
                } else if (minutes > 0) {
                    "${minutes}m ${seconds}s"
                } else {
                    "${seconds}s"
                }

                gui.item(boost.icon) {
                    name("<yellow>${boost.name}".asMini())
                    description(buildList {
                        boost.description.forEach { line ->
                            add("<gray>$line".asMini())
                        }
                        add("".asMini())
                        add("<gray>Time remaining: <green>$timeStr".asMini())
                    })
                    slots(slot)
                }

                slot++
                if (slot == 17) slot = 19
                if (slot > 25) return@forEach
            }
            activeKitBoosts.forEach { playerBoost ->
                gui.item(playerBoost.icon) {
                    name("<yellow>${playerBoost.name}".asMini())
                    description(buildList {
                        playerBoost.description.forEach { line ->
                            add("<gray>$line".asMini())
                        }
                        add("".asMini())
                        add("<gray>KitBoost - Not Timed".asMini())
                    })
                    slots(slot)
                }

                slot++
                if (slot == 17) slot = 19
                if (slot > 25) return@forEach
            }
        }

        gui.item(Material.ARROW) {
            name("<yellow>« Back to Shop".asMini())
            slots(22)
            onClick { event ->
                event.isCancelled = true
                BoostShop(plugin, player)
            }
        }
    }
}