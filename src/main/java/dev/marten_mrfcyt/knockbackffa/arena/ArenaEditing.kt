package dev.marten_mrfcyt.knockbackffa.arena

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.handlers.Arena
import dev.marten_mrfcyt.knockbackffa.utils.error
import dev.marten_mrfcyt.knockbackffa.utils.message
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File

fun Plugin.createArena(source: CommandSender, name: String, killBlock: String) {
    if (source is Player) {
        val location = source.location
        val arenaName = name.replace(" ", "_")
        source.message("<green>Creating<white> arena $arenaName at ${"%.1f".format(location.x)}, ${"%.1f".format(location.y)}, ${"%.1f".format(location.z)}")

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
                KnockBackFFA.instance.arenaHandler.addArena(Arena(arenaName, location))
                source.message("Arena $arenaName <green>created<white> successfully!")
            } else {
                source.error("This world needs pvp to be enabled!")
            }
        } else {
            source.error("Arena $arenaName <green>already exists<white>!")
        }
    } else {
        source.error("You must be a player to create an arena!")
    }
}

fun Plugin.deleteArena(source: CommandSender, name: String) {
    if (source !is Player) {
        source.error("You must be a player to delete an arena!")
        return
    }

    source.message("<dark_red>Deleting<white> arena $name!")

    val configFile = File("$dataFolder/arena.yml")
    val arenaConfig = YamlConfiguration.loadConfiguration(configFile)

    if (!arenaConfig.contains("arenas.$name")) {
        source.error("Arena $name <dark_red>not found. Is it misspelled<white>!")
        return
    }

    val location = KnockBackFFA.instance.arenaHandler.locationFetcher(name)
    println("Location fetched: $location")

    arenaConfig.set("arenas.$name", null)
    arenaConfig.save(configFile)
    println("Deleted arena $name from arena.yml")

    if (location != null) {
        KnockBackFFA.instance.arenaHandler.removeArena(Arena(name, location))
        println("Successfully removed arena $name from arenas list")
        source.message("Arena $name <dark_red>deleted<white> successfully!")
    } else {
        println("Location for arena $name not found in configuration")
    }
}