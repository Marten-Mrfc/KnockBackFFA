package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.utils.message
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin
import java.util.*

class PlayerJoinListener : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val source = event.player
        event.joinMessage(message("<white>${source.name}<gray> has joined the arena!"))
        val scoreboardHandler = ScoreboardHandler()
        scoreboardHandler.createScoreboard(source)
    }
}