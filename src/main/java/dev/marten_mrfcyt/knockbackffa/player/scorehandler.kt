package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import dev.marten_mrfcyt.knockbackffa.utils.error
import dev.marten_mrfcyt.knockbackffa.utils.cmessage
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
class ScoreHandler(private val plugin: KnockBackFFA) : Listener {
    // Player data directory
    // Event handler for player death
    @EventHandler
    fun onPlayerKill(event: PlayerDeathEvent) {
        // Get killer and killed player
        val source = event.player
        val killer = source.killer
        // Set death message
        if (killer != null){
            event.deathMessage(cmessage("killed_by_message", source, killer.name))
        }
        else{
            event.deathMessage(cmessage("death_message", source))
        }
        try {
            // Increment deaths for the killed player
            val playerData = PlayerData(plugin).getPlayerData(source.uniqueId)
            playerData.set("deaths", playerData.getInt("deaths", 0) + 1)

            // Increment kills for the killer player
            if (killer != null) {
                val killerData = PlayerData(plugin).getPlayerData(killer.uniqueId)
                killerData.set("kills", killerData.getInt("kills", 0) + 1)
                PlayerData(plugin).savePlayerData(killer.uniqueId, killerData)
            }
            PlayerData(plugin).savePlayerData(source.uniqueId, playerData)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to load or save player data: ${e.message}")
            e.printStackTrace()
            plugin.server.onlinePlayers.forEach { it.error("Failed to load or save player data! Please contact an administrator.") }
        }
    }
}