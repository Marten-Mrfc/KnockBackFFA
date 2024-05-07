package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import java.math.RoundingMode
import java.text.DecimalFormat

class ScoreHandler(private val plugin: KnockBackFFA) : Listener {
    // Player data directory
    // Event handler for player death
    @EventHandler
    fun onPlayerKill(event: PlayerDeathEvent) {
        val source = event.player
        val killer = source.killer

        if (killer != null) {
            event.deathMessage(cmessage("killed_by_message", source, killer.name))
        } else {
            event.deathMessage(cmessage("death_message", source))
        }

        try {
            val playerDataHandler = PlayerData(plugin)

            // Handle the killed player
            val playerData = playerDataHandler.getPlayerData(source.uniqueId)
            playerData.apply {
                set("deaths", getInt("deaths", 0) + 1)
                set("killstreak", 0)
                val deaths = getInt("deaths", 0)
                val kills = getInt("kills", 0)
                val df = DecimalFormat("#.##")
                df.roundingMode = RoundingMode.CEILING
                val kdRatio = if (deaths != 0) kills.toFloat() / deaths else kills.toFloat()
                val kdRatioRounded = df.format(kdRatio).replace(',', '.').toFloat()
                set("kd-ratio", kdRatioRounded)
            }
            playerDataHandler.savePlayerData(source.uniqueId, playerData)

            // Handle the killer player
            killer?.let {
                val killerData = playerDataHandler.getPlayerData(it.uniqueId)
                killerData.apply {
                    set("kills", getInt("kills", 0) + 1)
                    set("killstreak", getInt("killstreak", 0) + 1)
                    set("coins", getInt("coins", 0) + 1)
                    val killerDeaths = getInt("deaths", 0)
                    val killerKills = getInt("kills", 0)
                    val killerKdRatio = if (killerDeaths != 0) killerKills.toDouble() / killerDeaths else killerKills.toDouble()
                    val df = DecimalFormat("#.##")
                    df.roundingMode = RoundingMode.CEILING
                    val killerKdRatioRounded = df.format(killerKdRatio).replace(',', '.').toDouble()
                    set("kd-ratio", killerKdRatioRounded)
                }
                playerDataHandler.savePlayerData(it.uniqueId, killerData)
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to load or save player data: ${e.message}")
            e.printStackTrace()
            plugin.server.onlinePlayers.forEach { it.error("Failed to load or save player data! Please contact an administrator.") }
        }
    }
}