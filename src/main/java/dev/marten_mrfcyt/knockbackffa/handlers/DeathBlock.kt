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
    val config = YamlConfiguration.loadConfiguration(File("${plugin.dataFolder}/arena.yml"))
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDeathBlock(event: PlayerMoveEvent) {
        println("DeathBlock event")
        val arena = plugin.config.get("currentArena") as? String ?: return
        val killBlock = config.get("arenas.$arena.killBlock") as String
        val source = event.player
        println("$killBlock, ${source.location.add(0.0, -0.425, 0.0).block.type.name}, ${source.location.add(0.0, -0.425, 0.0).block.type.name == killBlock},$arena,${config.get("arenas.$arena.killBlock")} ")
        if (source.location.add(0.0, -0.425, 0.0).block.type.name == killBlock) {
            source.health = 0.0
            source.damage(100.0)
            println("Player died")
        }
    }
}