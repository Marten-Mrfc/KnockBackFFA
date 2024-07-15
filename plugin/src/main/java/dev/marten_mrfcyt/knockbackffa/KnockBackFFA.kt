package dev.marten_mrfcyt.knockbackffa

import dev.marten_mrfcyt.knockbackffa.handlers.ArenaHandler
import dev.marten_mrfcyt.knockbackffa.handlers.DeathBlock
import dev.marten_mrfcyt.knockbackffa.handlers.PlayerHandler
import dev.marten_mrfcyt.knockbackffa.kits.custom.BuildBlocks
import dev.marten_mrfcyt.knockbackffa.kits.custom.ItemCooldownListener
import dev.marten_mrfcyt.knockbackffa.kits.guis.GuiListener
import dev.marten_mrfcyt.knockbackffa.player.PlayerJoinListener
import dev.marten_mrfcyt.knockbackffa.player.PlayerQuitListener
import dev.marten_mrfcyt.knockbackffa.handlers.ScoreHandler
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

    lateinit var arenaHandler: ArenaHandler

    override fun onEnable() {
        instance = this
        arenaHandler = ArenaHandler(this)
        val mapDuration = config.getInt("mapDuration", 60)

        logger.info("Initializing KnockBackFFA...")

        if (!dataFolder.exists() && !dataFolder.mkdir()) {
            logger.severe("Failed to create data folder!")
            return
        }

        try {
            saveDefaultConfig()
        } catch (ex: IllegalArgumentException) {
            logger.severe("Failed to save default config: ${ex.message}")
        }

        registerCommands()
        registerEvents(
            PlayerJoinListener(ScoreboardHandler(this)),
            PlayerQuitListener(ScoreboardHandler(this)),
            ScoreHandler(this),
            GuiListener(this),
            BuildBlocks(this),
            ItemCooldownListener(),
            DeathBlock(this),
            PlayerHandler(this)
        )

        startArenaHandler(mapDuration)
        setupPlaceholders()

        logger.info("KnockBackFFA has been enabled successfully!")
    }

    override fun onDisable() {
        logger.info("KnockBackFFA has been disabled!")
    }

    private fun registerCommands() {
        logger.info("Registering commands...")
        kbffaCommand(arenaHandler)
        kitSelectorCommand()
        logger.info("Commands registered successfully!")
    }

    private fun registerEvents(vararg listeners: Listener) {
        val pluginManager = Bukkit.getPluginManager()
        listeners.forEach { listener ->
            pluginManager.registerEvents(listener, this)
        }
        logger.info("${listeners.size} events registered successfully!")
    }

    private fun startArenaHandler(mapDuration: Int) {
        logger.info("Starting arena handler...")
        object : BukkitRunnable() {
            override fun run() {
                lastSwitchTime = Instant.now()
                nextSwitchTime = lastSwitchTime.plusSeconds(mapDuration.toLong())
                arenaHandler.switchArena()
            }
        }.apply {
            run()
            runTaskTimer(this@KnockBackFFA, 0, mapDuration * 20L)
        }
        logger.info("Arena handler started successfully with $mapDuration seconds interval.")
    }

    private fun setupPlaceholders() {
        val placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI")
        if (placeholderAPI != null) {
            PlaceHolderAPI(this).register()
            logger.info("Placeholders registered successfully!")
        } else {
            logger.warning("Could not find PlaceholderAPI! This plugin is required.")
            Bukkit.getPluginManager().disablePlugin(this)
        }
    }
}