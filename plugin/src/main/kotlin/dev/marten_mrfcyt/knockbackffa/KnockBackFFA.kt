package dev.marten_mrfcyt.knockbackffa

import dev.marten_mrfcyt.knockbackffa.handlers.*
import dev.marten_mrfcyt.knockbackffa.kits.guis.GuiListener
import dev.marten_mrfcyt.knockbackffa.player.PlayerJoinListener
import dev.marten_mrfcyt.knockbackffa.player.PlayerQuitListener
import dev.marten_mrfcyt.knockbackffa.player.ScoreboardHandler
import dev.marten_mrfcyt.knockbackffa.utils.PlaceHolderAPI
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lirand.api.architecture.KotlinPlugin
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import java.io.File
import java.time.Instant
import java.util.logging.Level

class KnockBackFFA : KotlinPlugin() {
    companion object {
        lateinit var instance: KnockBackFFA
            private set
        var lastSwitchTime: Instant = Instant.now()
        var nextSwitchTime: Instant = Instant.now()
    }

    lateinit var arenaHandler: ArenaHandler
    lateinit var playerData: PlayerData

    @OptIn(DelicateCoroutinesApi::class)
    override fun onEnable() {
        logger.info("--------------------------------")
        logger.info("--- KnockBackFFA is starting ---")
        instance = this
        if (isEnabled) {
            arenaHandler = ArenaHandler(this)
        }
        playerData = PlayerData.getInstance(this)
        val mapDuration = config.getInt("mapDuration", 60)
        if (!dataFolder.exists() && !dataFolder.mkdir()) {
            logger.severe("Failed to create data folder!")
            return
        }
        val config = File(dataFolder, "kits.yml")
        if (!config.exists()) {
            logger.log(Level.INFO, "kits.yml does not exist. Saving resource...")
            saveResource("kits.yml", false)
        } else {
            logger.log(Level.INFO, "kits.yml found.")
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
            DeathBlock(this),
            PlayerHandler(this),
        )
        startArenaHandler(mapDuration)
        setupPlaceholders()
        ModifyHandler().registerEvents(this)
        logger.info("${ModifyHandler().getModifyObjects().size} modify objects registered successfully!")
        logger.info("--- KnockBackFFA has started ---")
        logger.info("--------------------------------")
    }

    override fun onDisable() {
        logger.info("KnockBackFFA has been disabled!")
        playerData.mysqlHandler.disconnect()
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun startArenaHandler(mapDuration: Int) {
        logger.info("Starting arena handler...")
        GlobalScope.launch {
            arenaHandler.loadArenas()
            while (true) {
                lastSwitchTime = Instant.now()
                nextSwitchTime = lastSwitchTime.plusSeconds(mapDuration.toLong())
                arenaHandler.switchArena()
                delay(mapDuration * 1000L)
            }
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