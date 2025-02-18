package dev.marten_mrfcyt.knockbackffa.arena

import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
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
            source.message(translate("arena.list.none"))
            return
        }
        source.message(translate("arena.list.header"))
        arenas.forEach {
            val location = arenaConfig.getConfigurationSection("arenas.$it.location")
            source.sendMini(translate("arena.list.entry",
                "arena_name" to it,
                "world" to (location?.getString("world") ?: "unknown"),
                "x" to "%.1f".format(location?.getDouble("x")),
                "y" to "%.1f".format(location?.getDouble("y")),
                "z" to "%.1f".format(location?.getDouble("z"))
            ))
        }
    } else {
        source.message(translate("error.player_only"))
    }
}