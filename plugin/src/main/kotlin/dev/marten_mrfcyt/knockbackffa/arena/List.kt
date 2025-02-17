package dev.marten_mrfcyt.knockbackffa.arena

import mlib.api.utilities.*
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File

fun Plugin.listArena(source: CommandSender) {
    if (source is Player) {
        val config = File("$dataFolder/arena.yml")
        val arenaConfig = YamlConfiguration.loadConfiguration(config)
        val arenas = arenaConfig.getConfigurationSection("arenas")?.getKeys(false)
        if (arenas.isNullOrEmpty()) {
            source.error("No arenas found!")
            return
        }
        source.message("<bold>Arenas:<reset>")
        arenas.forEach {
            val location = arenaConfig.getConfigurationSection("arenas.$it.location")
            source.sendMini("<white><bold>*</bold> <green>$it<reset> <gray>at <white>${location?.getString("world")}<dark_gray> (<gray>${"%.1f".format(location?.getDouble("x"))}<white> <gray>${"%.1f".format(location?.getDouble("y"))}<white> <gray>${"%.1f".format(location?.getDouble("z"))}<dark_gray>)<reset>")
        }
    }
    else {
        source.error("You must be a player to list arenas!")
    }
}