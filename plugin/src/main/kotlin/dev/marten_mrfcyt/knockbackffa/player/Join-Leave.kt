package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.loadKit
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translateListRandom
import mlib.api.utilities.asMini
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerJoinListener(private val scoreboardHandler: ScoreboardHandler) : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val source = event.player
        event.joinMessage((translateListRandom("player.join_message", "player_name" to source.name)).asMini())
        scoreboardHandler.startUpdatingScoreboard(source)
        val currentArena = KnockBackFFA.instance.config.get("currentLocation") as? Location
        if (currentArena != null) {
            loadKit(KnockBackFFA.instance, source)
            source.teleport(currentArena)
        }
    }
}

class PlayerQuitListener(private val scoreboardHandler: ScoreboardHandler) : Listener {
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val source = event.player
        event.quitMessage((translateListRandom("player.leave_message", "player_name" to source.name)).asMini())
        scoreboardHandler.stopUpdatingScoreboard(source)
    }
}