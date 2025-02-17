package dev.marten_mrfcyt.knockbackffa.arena

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
import mlib.api.utilities.*
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File

fun Plugin.createArena(source: CommandSender, name: String, killBlock: String) {
    if (source !is Player) {
        source.message(translate("error.player_only"))
        return
    }

    val location = source.location
    val arenaName = name.replace(" ", "_")

    source.message(translate("arena.create.location",
        "arena_name" to arenaName,
        "x" to "%.1f".format(location.x),
        "y" to "%.1f".format(location.y),
        "z" to "%.1f".format(location.z)
    ))

    val config = File("$dataFolder/arena.yml")
    if (!config.exists()) {
        source.message(translate("arena.create.file.not_found"))
        try {
            config.createNewFile()
        } catch (ex: Exception) {
            source.message(translate("arena.create.file.error",
                "error" to ex.message.toString()
            ))
            return
        }
        source.message(translate("arena.create.file.created"))
    }

    val arenaConfig = YamlConfiguration.loadConfiguration(config)
    if (!arenaConfig.contains("arenas.$arenaName")) {
        if (location.world?.pvp == true) {
            server.scheduler.runTaskAsynchronously(this, Runnable {
                KnockBackFFA.instance.arenaHandler.addArena(Arena(arenaName, location, killBlock))
            })
            source.message(translate("arena.create.success",
                "arena_name" to arenaName
            ))
        } else {
            source.message(translate("arena.create.pvp_required"))
        }
    } else {
        source.message(translate("arena.create.exists",
            "arena_name" to arenaName
        ))
    }
}

fun Plugin.deleteArena(source: CommandSender, name: String) {
    if (source !is Player) {
        source.message(translate("error.player_only"))
        return
    }

    source.message(translate("arena.delete.start",
        "arena_name" to name
    ))

    val configFile = File("$dataFolder/arena.yml")
    val arenaConfig = YamlConfiguration.loadConfiguration(configFile)
    if (!arenaConfig.contains("arenas.$name")) {
        source.message(translate("arena.delete.not_found",
            "arena_name" to name
        ))
        return
    }

    val location = ArenaHandler(KnockBackFFA.instance).locationFetcher(name)

    if (location != null) {
        server.scheduler.runTaskAsynchronously(this, Runnable {
            ArenaHandler(KnockBackFFA.instance).removeArena(Arena(name, location))
        })
        source.message(translate("arena.delete.success",
            "arena_name" to name
        ))
    }
}