package dev.marten_mrfcyt.knockbackffa.arena

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import java.io.File
import java.util.logging.Logger

class DeathBlock(private val plugin: KnockBackFFA) : Listener {
    private val config = YamlConfiguration.loadConfiguration(File("${plugin.dataFolder}/arena.yml"))
    private val logger: Logger = plugin.logger

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDeathBlock(event: PlayerMoveEvent) {
        val arena = plugin.config.getString("currentArena")
        if (arena == null) {
            return
        }
        val killBlock = config.getString("arenas.$arena.killBlock")?.replace("minecraft:", "", ignoreCase = true)?.uppercase()
        if (killBlock == null) {
            logger.warning("Kill block is null for arena: $arena")
            return
        }

        val adjustedLocation = event.to.clone().add(0.0, -0.245, 0.0)

        val blockBelowPlayer = adjustedLocation.block

        if (blockBelowPlayer.type.name.equals(killBlock, ignoreCase = true)) {
            event.player.health = 0.0
            event.player.damage(100.0)
        }
    }
}