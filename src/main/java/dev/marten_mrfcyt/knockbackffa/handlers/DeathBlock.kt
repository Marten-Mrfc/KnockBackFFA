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
    private val arena = plugin.config.get("currentArena") as? String
    val config = YamlConfiguration.loadConfiguration(File("${plugin.dataFolder}/arena.yml"))
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDeathBlock(event: PlayerMoveEvent) {
        if (arena == null) {
            return
        }
        val killBlock = config.get("arenas.$arena.killBlock") as String
        val source = event.player
        if (source.location.add(0.0, -0.425, 0.0).block.type.name == killBlock) {
            source.health = 0.0
        }
    }
}