package dev.marten_mrfcyt.knockbackffa.arena.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
import mlib.api.utilities.*
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File

fun Plugin.createArena(source: CommandSender, name: String, killBlock: Material) {
    if (source !is Player) {
        source.message(translate("error.player_only"))
        return
    }

    val arenaName = name.replace(" ", "_")

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
    if (arenaConfig.contains("arenas.$arenaName")) {
        source.message(translate("arena.create.exists",
            "arena_name" to arenaName
        ))
        return
    }

    if (source.location.world?.pvp != true) {
        source.message(translate("arena.create.pvp_required"))
        return
    }

    KnockBackFFA.instance.arenaCreationHandler.startArenaCreation(source, arenaName, killBlock)
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

    val arena = KnockBackFFA.instance.arenaHandler.loadArenaByName(name)

    if (arena != null) {
        server.scheduler.runTaskAsynchronously(this, Runnable {
            KnockBackFFA.instance.arenaHandler.removeArena(arena)
        })
        source.message(translate("arena.delete.success",
            "arena_name" to name
        ))
    } else {
        source.message(translate("arena.delete.failed",
            "arena_name" to name
        ))
    }
}