package dev.marten_mrfcyt.knockbackffa.arena

import dev.marten_mrfcyt.knockbackffa.utils.asMini
import dev.marten_mrfcyt.knockbackffa.utils.error
import dev.marten_mrfcyt.knockbackffa.utils.message
import dev.marten_mrfcyt.knockbackffa.utils.sendMini
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File

fun Plugin.createArena(source: CommandSender, name: String) {
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
        arenaConfig.set("arenas.$arenaName.location", location)
        arenaConfig.save(config)
        source.message("Arena $arenaName <green>created<white> successfully!")
    }
    else {
        source.error("You must be a player to create an arena!")
    }
}
