package dev.marten_mrfcyt.knockbackffa.arena

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.handlers.Arena
import dev.marten_mrfcyt.knockbackffa.handlers.ArenaHandler
import dev.marten_mrfcyt.knockbackffa.utils.error
import dev.marten_mrfcyt.knockbackffa.utils.message
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File

@OptIn(DelicateCoroutinesApi::class)
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
                GlobalScope.launch {
                    KnockBackFFA.instance.arenaHandler.addArena(Arena(arenaName, location, killBlock))
                }
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

@OptIn(DelicateCoroutinesApi::class)
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

    val location = ArenaHandler(KnockBackFFA.instance).locationFetcher(name)

    if (location != null) {
        GlobalScope.launch {
            ArenaHandler(KnockBackFFA.instance).removeArena(Arena(name, location))
        }
        source.message("Arena $name <dark_red>deleted<white> successfully!")
    }
}