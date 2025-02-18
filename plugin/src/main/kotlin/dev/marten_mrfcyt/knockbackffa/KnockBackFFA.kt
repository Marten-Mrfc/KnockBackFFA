package dev.marten_mrfcyt.knockbackffa

import dev.marten_mrfcyt.knockbackffa.arena.ArenaHandler
import dev.marten_mrfcyt.knockbackffa.arena.DeathBlock
import dev.marten_mrfcyt.knockbackffa.kits.ModifyHandler
import dev.marten_mrfcyt.knockbackffa.kits.listKits
import dev.marten_mrfcyt.knockbackffa.player.*
import dev.marten_mrfcyt.knockbackffa.utils.*
import mlib.api.architecture.KotlinPlugin
import mlib.api.architecture.extensions.registerEvents
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
        super.onEnable()
        instance = this

        logger.info("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
        logger.info("┃      🚀 KnockBackFFA Start      ┃")
        logger.info("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

        if (!dataFolder.exists()) {
            logger.info("1️⃣ First time setup: Extra logging of file creations!")
            logger.warning("⚠️ Data folder not found, creating...")
            dataFolder.mkdirs()
            logger.info("📁 Data folder created")
        }

        try {
            saveDefaultConfig()
            saveConfig()
        } catch (ex: IllegalArgumentException) {
            logger.severe("❌ Config error: ${ex.message}")
        }

        TranslationManager.init(this)
        if (isEnabled) { arenaHandler = ArenaHandler(this) }
        PlayerData.getInstance(this)

        val kitConfig = File(dataFolder, "kits.yml")
        if (!kitConfig.exists()) {
            logger.warning("⚠️ kits.yml not found, creating...")
            saveResource("kits.yml", false)
            logger.info("📁 kits.yml created")
        }
        logger.info("🦾 Loaded ${listKits(this).size} kits")
        registerCommands()
        registerEvent(
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

    private fun registerEvent(vararg listeners: Listener) {
        logger.info("🔧 Registering events...")
        registerEvents(*listeners)
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