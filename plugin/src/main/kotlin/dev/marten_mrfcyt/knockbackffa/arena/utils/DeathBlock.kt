package dev.marten_mrfcyt.knockbackffa.arena.utils

import dev.marten_mrfcyt.knockbackffa.arena.currentArena
import dev.marten_mrfcyt.knockbackffa.bypassMode
import mlib.api.utilities.debug
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class DeathBlock() : Listener {
    private fun isBypassing(player: Player): Boolean {
        val bypassing = bypassMode.getOrDefault(player, false)
        if (bypassing) {
            debug("Player ${player.name} is bypassing restrictions")
        }
        return bypassing
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDeathBlock(event: PlayerMoveEvent) {
        val currentArena = currentArena ?: return
        val killBlock = currentArena.killBlock

        // Check if player is below y=-64 (void death)
        if (event.to.y < -64) {
            event.player.health = 0.0
            event.player.damage(100.0)
            return
        }

        if (currentArena.isInSpawnRegion(event.to) || isBypassing(event.player)) {
            return
        }

        val adjustedLocation = event.to.clone().add(0.0, -0.245, 0.0)
        val blockBelowPlayer = adjustedLocation.block

        if (blockBelowPlayer.type == killBlock) {
            event.player.health = 0.0
            event.player.damage(100.0)
        }
    }
}