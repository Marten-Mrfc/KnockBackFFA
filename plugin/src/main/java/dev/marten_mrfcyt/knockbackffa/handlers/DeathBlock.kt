package dev.marten_mrfcyt.knockbackffa.handlers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import java.io.File

class DeathBlock(private val plugin: KnockBackFFA) : Listener {
    private val config = YamlConfiguration.loadConfiguration(File("${plugin.dataFolder}/arena.yml"))

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDeathBlock(event: PlayerMoveEvent) {
        val arena = plugin.config.getString("currentArena") ?: return
        val killBlock = config.getString("arenas.$arena.killBlock")?.replace("minecraft:", "", ignoreCase = true)?.uppercase()
        // Adjust the Y-coordinate by subtracting the desired amount
        val adjustedLocation = event.to.clone().add(0.0, -0.245, 0.0)
        val blockBelowPlayer = adjustedLocation.block

        println("$arena = $killBlock = ${blockBelowPlayer.type.name}")
        if (killBlock != null && blockBelowPlayer.type.name.equals(killBlock, ignoreCase = true)) {
            event.player.health = 0.0
            event.player.damage(100.0)
        }
    }
}
