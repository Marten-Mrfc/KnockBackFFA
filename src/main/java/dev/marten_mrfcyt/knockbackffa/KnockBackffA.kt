package dev.marten_mrfcyt.knockbackffa

import dev.marten_mrfcyt.knockbackffa.arena.ArenaHandler
import dev.marten_mrfcyt.knockbackffa.kits.guis.GuiListener
import dev.marten_mrfcyt.knockbackffa.player.PlayerJoinListener
import dev.marten_mrfcyt.knockbackffa.player.PlayerQuitListener
import dev.marten_mrfcyt.knockbackffa.player.ScoreHandler
import dev.marten_mrfcyt.knockbackffa.player.ScoreboardHandler
import dev.marten_mrfcyt.knockbackffa.utils.PlaceHolderAPI
import lirand.api.architecture.KotlinPlugin
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.scheduler.BukkitRunnable
import java.time.Instant

class KnockBackFFA : KotlinPlugin() {
    companion object {
        lateinit var instance: KnockBackFFA
            private set
        var lastSwitchTime: Instant = Instant.now()
        var nextSwitchTime: Instant = Instant.now()
    }

    override fun onEnable() {
        val mapDuration = config.getInt("mapDuration", 60)
        instance = this

        logger.info("Registering commands...")
        if (!dataFolder.exists() && !dataFolder.mkdir()) {
            logger.severe("Failed to create data folder!")
            return
        }
        try {
            saveDefaultConfig()
        } catch (ex: IllegalArgumentException) {
            logger.severe("Failed to save default config: ${ex.message}")
        }
        val scoreboardHandler = ScoreboardHandler(this)
        val arenaHandler = ArenaHandler(this)
        kbffaCommand(arenaHandler)
        logger.info("Commands registered -> Registering events...")
        var amount = 1
        listOf(
            registerEvents(
                PlayerJoinListener(scoreboardHandler),
                PlayerQuitListener(scoreboardHandler),
                ScoreHandler(this),
                GuiListener(this)
            )
        ).forEach { _ -> amount++ }
        logger.info("$amount events registered -> Starting arena handler...")
        val task = object : BukkitRunnable() {
            override fun run() {
                lastSwitchTime = Instant.now()
                nextSwitchTime =
                    lastSwitchTime.plusSeconds(mapDuration.toLong()) // Use the map duration from the config file
                arenaHandler.switchArena()
            }
        }
        task.run()
        task.runTaskTimer(this, 0, mapDuration * 20L)  // mapDuration seconds * 20 ticks/second
        logger.info("Arena handler started -> Registering placeholders...")
        val placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI")
        if (placeholderAPI != null) {
            PlaceHolderAPI(this).register()
            logger.info("Placeholders registered -> KnockBackFFA has been enabled!")
        } else {
            logger.warning("Could not find PlaceholderAPI! This plugin is required.")
            Bukkit.getPluginManager().disablePlugin(this)
        }
    }

    override fun onDisable() {
        logger.info("KnockBackFFA has been disabled!")
    }

    private fun registerEvents(vararg listeners: Listener) {
        val pluginManager = Bukkit.getPluginManager()
        for (listener in listeners) {
            pluginManager.registerEvents(listener, this)
        }
    }
}