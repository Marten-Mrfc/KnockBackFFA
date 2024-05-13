package dev.marten_mrfcyt.knockbackffa.arena

import dev.marten_mrfcyt.knockbackffa.utils.error
import dev.marten_mrfcyt.knockbackffa.utils.message
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File
fun Plugin.createArena(source: CommandSender, name: String, killBlock: String) {
    if (source is Player) {
    // Setting location and modifying name.
        val location = source.location
        val arenaName = name.replace(" ", "_")
        source.message("<green>Creating<white> arena $arenaName at ${"%.1f".format(location.x)}, ${"%.1f".format(location.y)}, ${"%.1f".format(location.z)}")
    // Save arena to arena.yml
        val config = File("$dataFolder/arena.yml")
        if (!config.exists()) {
            source.message("No arena.yml file found, creating a new one!")
            try {
                config.createNewFile()
            } catch (ex: Exception) {
                source.error("Failed to create arena.yml file: ${ex.message}")
                return
            }
            source.message("arena.yml file created!")
        }
        val arenaConfig = YamlConfiguration.loadConfiguration(config)
        if (!arenaConfig.contains("arenas.$arenaName")) {
            if (location.world?.pvp == true) {
                with(arenaConfig) {
                    set("arenas.$arenaName.location.world", location.world?.name)
                    set("arenas.$arenaName.location.x", location.x)
                    set("arenas.$arenaName.location.y", location.y)
                    set("arenas.$arenaName.location.z", location.z)
                    set("arenas.$arenaName.location.yaw", location.yaw)
                    set("arenas.$arenaName.location.pitch", location.pitch)
                    set("arenas.$arenaName.killBlock", killBlock)
                }
                arenaConfig.save(config)
                source.message("Arena $arenaName <green>created<white> successfully!")
            }
            else {
                source.error("This world needs pvp to be enabled!")
            }
        }
        else {
            source.error("Arena $arenaName <green>already exists<white>!")
        }
    }
    else {
        source.error("You must be a player to create an arena!")
    }
}
