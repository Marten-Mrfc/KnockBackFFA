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
            event.deathMessage((translateListRandom("player.killed_by_message",
                "player_name" to source.name,
                "killer_name" to killer.name)).asMini())
        } else {
            event.deathMessage((translateListRandom("player.death_message",
                "player_name" to source.name)).asMini())
        }

        source.inventory.clear()

        try {
            val playerDataInstance = PlayerData.getInstance(plugin)

            // Update the victim's data
            val sourceDataModel = playerDataInstance.getPlayerDataModel(source.uniqueId)
            sourceDataModel.apply {
                deaths += 1
                killstreak = 0

                val df = DecimalFormat("#.##")
                df.roundingMode = RoundingMode.CEILING
                val kdRatio = if (deaths != 0) kills.toDouble() / deaths else kills.toDouble()
                val kdRatioRounded = df.format(kdRatio).replace(',', '.').toDouble()
                this.kdRatio = kdRatioRounded
            }
            playerDataInstance.savePlayerDataModel(source.uniqueId, sourceDataModel)

            // Update the killer's data if exists
            killer?.let { killerPlayer ->
                val killerDataModel = playerDataInstance.getPlayerDataModel(killerPlayer.uniqueId)
                killerDataModel.apply {
                    kills += 1
                    killstreak += 1
                    coins += 1

                    if (killstreak > maxKillstreak) {
                        maxKillstreak = killstreak
                    }

                    val df = DecimalFormat("#.##")
                    df.roundingMode = RoundingMode.CEILING
                    val killerKdRatio = if (deaths != 0) kills.toDouble() / deaths else kills.toDouble()
                    val killerKdRatioRounded = df.format(killerKdRatio).replace(',', '.').toDouble()
                    this.kdRatio = killerKdRatioRounded
                }
                playerDataInstance.savePlayerDataModel(killerPlayer.uniqueId, killerDataModel)
            }
        } catch (e: Exception) {
            plugin.logger.severe(TranslationManager.translate("error.data_save",
                "error" to e.message.toString()))
            e.printStackTrace()
            plugin.server.onlinePlayers.forEach {
                it.error(TranslationManager.translate("error.data_save_admin"))
            }
        }
    }

    @EventHandler
    fun respawn(event: PlayerRespawnEvent) {
        val source = event.player
        val currentArena = KnockBackFFA.instance.config.get("currentLocation") as? Location

        source.message(TranslationManager.translate("kit.loading_kit"))
        loadKit(KnockBackFFA.instance, source, false)

        if (currentArena != null) {
            event.respawnLocation = currentArena
        }
    }
}