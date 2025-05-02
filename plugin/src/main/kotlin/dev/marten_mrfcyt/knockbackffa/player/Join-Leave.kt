package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.loadKit
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translateListRandom
import mlib.api.utilities.asMini
import mlib.api.utilities.debug
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
class PlayerJoinListener(
    private val scoreboardHandler: ScoreboardHandler,
    private val bossBarHandler: BossBarHandler
) : Listener {    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val source = event.player
        debug("Player ${source.name} joining with UUID: ${source.uniqueId}")
        event.joinMessage((translateListRandom("player.join_message", "player_name" to source.name)).asMini())
        scoreboardHandler.startUpdatingScoreboard(source)
        bossBarHandler.showBossBar(source)

        val currentArena = KnockBackFFA.instance.config.get("currentLocation") as? Location
        if (currentArena != null) {
            debug("Teleporting ${source.name} to current arena at ${currentArena.x}, ${currentArena.y}, ${currentArena.z}")
            loadKit(KnockBackFFA.instance, source, true)
            source.teleport(currentArena)
        } else {
            debug("No current arena location found for ${source.name}")
        }
    }
}

class PlayerQuitListener(
    private val scoreboardHandler: ScoreboardHandler,
    private val bossBarHandler: BossBarHandler
) : Listener {    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val source = event.player
        debug("Player ${source.name} quitting with UUID: ${source.uniqueId}")
        event.quitMessage((translateListRandom("player.leave_message", "player_name" to source.name)).asMini())
        scoreboardHandler.stopUpdatingScoreboard(source)
        bossBarHandler.removeBossBar(source)
    }
}