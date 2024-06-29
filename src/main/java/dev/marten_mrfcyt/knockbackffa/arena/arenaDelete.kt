package dev.marten_mrfcyt.knockbackffa.arena

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.handlers.Arena
import dev.marten_mrfcyt.knockbackffa.handlers.ArenaHandler
import dev.marten_mrfcyt.knockbackffa.utils.error
import dev.marten_mrfcyt.knockbackffa.utils.message
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File

fun Plugin.deleteArena(source: CommandSender, name: String) {
    if (source is Player) {
        // delete arena from arena.yml
        source.message("<dark_red>Deleting<white> arena $name!")
        val config = File("$dataFolder/arena.yml")
        val arenaConfig = YamlConfiguration.loadConfiguration(config)
        if (!arenaConfig.contains("arenas.$name")) {
            source.error("Arena $name <dark_red>not found. Is it misspelled<white>!")
            return
        }
        arenaConfig.set("arenas.$name", null)
        arenaConfig.save(config)
        ArenaHandler(KnockBackFFA()).locationFetcher(name, arenaConfig)
            ?.let { Arena(name, it) }?.let { ArenaHandler(KnockBackFFA()).removeArena(it) }
        source.message("Arena $name <dark_red>deleted<white> successfully!")
    } else {
        source.error("You must be a player to create an arena!")
    }
}