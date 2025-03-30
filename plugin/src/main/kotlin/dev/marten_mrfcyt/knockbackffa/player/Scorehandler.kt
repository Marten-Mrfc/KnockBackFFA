package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.loadKit
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translateListRandom
import mlib.api.utilities.*
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import java.math.RoundingMode
import java.text.DecimalFormat

class ScoreHandler(private val plugin: KnockBackFFA) : Listener {
    @EventHandler
    fun onPlayerKill(event: PlayerDeathEvent) {
        event.drops.clear()
        val source = event.player
        val killer = source.killer
        if (killer != null) {
            event.deathMessage((translateListRandom("player.killed_by_message", "player_name" to source.name, "killer_name" to killer.name)).asMini())
        } else {
            event.deathMessage((translateListRandom("player.death_message", "player_name" to source.name)).asMini())
        }
        source.inventory.clear()
        try {
            val playerDataHandler = PlayerData.getInstance(plugin)
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

            killer?.let {
                val killerData = playerDataHandler.getPlayerData(it.uniqueId)
                killerData.apply {
                    val currentKills = getInt("kills", 0) + 1
                    val currentKillstreak = getInt("killstreak", 0) + 1
                    val currentMaxKillstreak = getInt("max-killstreak", 0)

                    set("kills", currentKills)
                    set("killstreak", currentKillstreak)
                    set("coins", getInt("coins", 0) + 1)

                    if (currentKillstreak > currentMaxKillstreak) {
                        set("max-killstreak", currentKillstreak)
                    }

                    val killerDeaths = getInt("deaths", 0)
                    val killerKills = getInt("kills", 0)
                    val killerKdRatio =
                        if (killerDeaths != 0) killerKills.toDouble() / killerDeaths else killerKills.toDouble()
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

    @EventHandler
    fun respawn(event: PlayerRespawnEvent) {
        val source = event.player
        val currentArena = KnockBackFFA.instance.config.get("currentLocation") as? Location
        source.message("Loading kit...")
        loadKit(KnockBackFFA.instance, source)
        if (currentArena != null) {
            event.respawnLocation = currentArena
        }
    }
}