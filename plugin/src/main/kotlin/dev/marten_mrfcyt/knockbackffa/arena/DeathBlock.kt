package dev.marten_mrfcyt.knockbackffa.arena

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class DeathBlock() : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDeathBlock(event: PlayerMoveEvent) {
        val currentArena = currentArena ?: return
        val killBlock = currentArena.killBlock

        val adjustedLocation = event.to.clone().add(0.0, -0.245, 0.0)
        val blockBelowPlayer = adjustedLocation.block

        if (blockBelowPlayer.type == killBlock) {
            event.player.health = 0.0
            event.player.damage(100.0)
        }
    }
}