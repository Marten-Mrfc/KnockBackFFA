package dev.marten_mrfcyt.knockbackffa

import dev.marten_mrfcyt.knockbackffa.arena.ArenaHandler
import dev.marten_mrfcyt.knockbackffa.arena.DeathBlock
import dev.marten_mrfcyt.knockbackffa.kits.ModifyHandler
import dev.marten_mrfcyt.knockbackffa.player.*
import dev.marten_mrfcyt.knockbackffa.utils.*
import mlib.api.architecture.KotlinPlugin
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import java.io.File
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
        logger.info("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
        logger.info("┃      🚀 KnockBackFFA Start      ┃")
        logger.info("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

        super.onEnable()
        instance = this
        TranslationManager.init(this)
        if (isEnabled) {
            arenaHandler = ArenaHandler(this)
        }
        val kitConfig = File(dataFolder, "kits.yml")

        PlayerData.getInstance(this)

        if (!dataFolder.exists() && !dataFolder.mkdir()) {
            logger.severe("❌ Failed to create data folder!")
            return
        }

        if (!kitConfig.exists()) {
            logger.warning("⚠️ kits.yml not found, creating...")
            saveResource("kits.yml", false)
        } else {
            logger.info("📁 kits.yml loaded")
        }

        try {
            saveDefaultConfig()
        } catch (ex: IllegalArgumentException) {
            logger.severe("❌ Config error: ${ex.message}")
        }

        registerCommands()
        registerEvents(
            PlayerJoinListener(ScoreboardHandler(this), BossBarHandler(this)),
            PlayerQuitListener(ScoreboardHandler(this), BossBarHandler(this)),
            ScoreHandler(this),
            DeathBlock(),
            PlayerHandler(this),
        )

        startArenaHandler(this.config.getInt("mapDuration", 60))
        setupPlaceholders()

        ModifyHandler().registerEvents(this)
        logger.info("⚙️ ${ModifyHandler().getModifyObjects().size} modify objects loaded")

        BStatsMetrics.registerMetrics()

        logger.info("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
        logger.info("┃    ✨ KnockBackFFA is Ready     ┃")
        logger.info("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")
    }

    override fun onDisable() {
        logger.info("💤 KnockBackFFA disabled")
        PlayerData.getInstance(this).mysqlHandler.disconnect()
    }

    private fun registerCommands() {
        logger.info("🔧 Setting up commands...")
        kbffaCommand(arenaHandler)
        kitSelectorCommand()
        logger.info("✅ Commands ready")
    }

    private fun registerEvents(vararg listeners: Listener) {
        val pluginManager = Bukkit.getPluginManager()
        listeners.forEach { listener ->
            pluginManager.registerEvents(listener, this)
        }
        logger.info("📌 ${listeners.size} events registered")
    }

    private fun startArenaHandler(mapDuration: Int) {
        logger.info("🎮 Starting arena handler...")
        arenaHandler.loadArenas()

        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            lastSwitchTime = Instant.now()
            nextSwitchTime = lastSwitchTime.plusSeconds(mapDuration.toLong())
            arenaHandler.switchArena()
        }, 0L, mapDuration * 20L)

        logger.info("✅ Arena handler ready (${mapDuration}s)")
    }

    private fun setupPlaceholders() {
        val placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI")
        if (placeholderAPI != null) {
            PlaceHolderAPI(this).register()
            logger.info("📎 Placeholders ready")
        } else {
            logger.warning("⚠️ PlaceholderAPI missing!")
            Bukkit.getPluginManager().disablePlugin(this)
        }
    }
}