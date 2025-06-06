package dev.marten_mrfcyt.knockbackffa.arena.utils

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.arena.currentArena
import dev.marten_mrfcyt.knockbackffa.bypassMode
import mlib.api.utilities.debug
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class DeathBlock : Listener {

    private var taskId: Int = -1

    private fun isBypassing(player: Player): Boolean {
        val bypassing = bypassMode.getOrDefault(player, false)
        if (bypassing) {
            debug("Player ${player.name} is bypassing restrictions")
        }
        return bypassing
    }

    init {
        // Run task less frequently (every 5 ticks instead of every tick)
        taskId = Bukkit.getScheduler().runTaskTimer(KnockBackFFA.instance, Runnable {
            val arena = currentArena ?: return@Runnable
            val killBlock = arena.killBlock

            for (player in Bukkit.getOnlinePlayers()) {
                if (isBypassing(player) || arena.isInSpawnRegion(player.location)) {
                    continue
                }
                checkAndHandleDeathBlock(player, arena, killBlock)
            }
        }, 0L, 5).taskId
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDeathBlock(event: PlayerMoveEvent) {
        val player = event.player
        val arena = currentArena ?: return

        if (arena.isInSpawnRegion(player.location) || isBypassing(player)) {
            return
        }

        if (event.to.y < -64) {
            killPlayer(player)
            return
        }

        checkAndHandleDeathBlock(player, arena, arena.killBlock)
    }

    // Optimized method that takes arena and killBlock as parameters to avoid lookups
    private fun checkAndHandleDeathBlock(player: Player, arena: ArenaModel, killBlock: Any) {
        if (arena.isInSpawnRegion(player.location)) {
            return
        }
        val blockBelowPlayer = player.location.clone().add(0.0, -0.245, 0.0).block

        if (blockBelowPlayer.type == killBlock) {
            killPlayer(player)
        }
    }

    private fun killPlayer(player: Player) {
        player.health = 0.0
        player.damage(100.0)
    }

    fun cleanup() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId)
            taskId = -1
        }
    }
}
