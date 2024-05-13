package dev.marten_mrfcyt.knockbackffa.handlers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import java.io.File

class DeathBlock(private val plugin: KnockBackFFA) : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDeathBlock(event: PlayerMoveEvent) {
        val source = event.player
        val arena = plugin.config.get("currentArena")
        val config = YamlConfiguration.loadConfiguration(File("${plugin.dataFolder}/arena.yml"))
        val killBlock = config.get("arenas.$arena.killBlock") as String
        if (source.location.add(0.0, -0.425, 0.0).block.type.name == killBlock) {
            source.damage(1000.0)
        }
    }
}