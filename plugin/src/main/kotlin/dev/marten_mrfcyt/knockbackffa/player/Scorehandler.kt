package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.arena.ArenaHandler
import dev.marten_mrfcyt.knockbackffa.arena.currentArena
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

        debug(plugin, "Player ${source.name} died" + if (killer != null) " killed by ${killer.name}" else " (not by a player)")

        // Handle death messages
        if (killer != null) {
            val message = translateListRandom("player.killed_by_message",
                "player_name" to source.name,
                "killer_name" to killer.name)
            if (!message.contains("null")) {
                event.deathMessage(message.asMini())
            } else {
                event.deathMessage(null)
            }
        } else {
            val message = translateListRandom("player.death_message",
                "player_name" to source.name)
            if (!message.contains("null")) {
                event.deathMessage(message.asMini())
            } else {
                event.deathMessage(null)
            }        }

        source.inventory.clear()
        
        // The combat system handles kill/death processing through its event listeners
        // This ScoreHandler only handles death messages and respawning
    }

    @EventHandler
    fun respawn(event: PlayerRespawnEvent) {
        val source = event.player
        val currentArena = currentArena?.spawnpoint

        debug(plugin, "Player ${source.name} respawning")
        source.message(TranslationManager.translate("kit.loading_kit"))
        loadKit(KnockBackFFA.instance, source, false)

        if (currentArena != null) {
            debug(plugin, "Setting respawn location for ${source.name} to ${currentArena.x}, ${currentArena.y}, ${currentArena.z}")
            event.respawnLocation = currentArena
        } else {
            debug(plugin, "No current arena location found for respawning ${source.name}")
        }
    }
}