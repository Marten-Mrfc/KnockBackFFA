package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.arena.ArenaHandler
import dev.marten_mrfcyt.knockbackffa.utils.message
import org.bukkit.Location
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
        val currentArena = KnockBackFFA.instance.config.get("currentLocation") as? Location
        if (currentArena != null) {
            source.teleport(currentArena)
        }
    }
}
class PlayerQuitListener(private val scoreboardHandler: ScoreboardHandler) : Listener {
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val source = event.player
        scoreboardHandler.stopUpdatingScoreboard(source)
    }
}