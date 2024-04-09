package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.utils.message
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerJoinListener(private val scoreboardHandler: ScoreboardHandler) : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val source = event.player
        event.joinMessage(message("<white>${source.name}<gray> has joined the arena!"))
        scoreboardHandler.startUpdatingScoreboard(source)
    }
}
class PlayerQuitListener(private val scoreboardHandler: ScoreboardHandler) : Listener {
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val source = event.player
        scoreboardHandler.stopUpdatingScoreboard(source)
    }
}