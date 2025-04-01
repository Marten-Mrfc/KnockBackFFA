package dev.marten_mrfcyt.knockbackffa

import dev.marten_mrfcyt.knockbackffa.arena.ArenaHandler
import dev.marten_mrfcyt.knockbackffa.arena.DeathBlock
import dev.marten_mrfcyt.knockbackffa.boosts.managers.BoostManager
import dev.marten_mrfcyt.knockbackffa.boosts.managers.PlayerBoostManager
import dev.marten_mrfcyt.knockbackffa.kits.managers.KitManager
import dev.marten_mrfcyt.knockbackffa.kits.managers.ModifierManager
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
        lateinit var kitManager: KitManager
            private set
        var lastSwitchTime: Instant = Instant.now()
        var nextSwitchTime: Instant = Instant.now()
    }

    lateinit var modifierManager: ModifierManager
    lateinit var arenaHandler: ArenaHandler
    lateinit var playerBoostManager: PlayerBoostManager
        private set
    lateinit var boostManager: BoostManager
        private set

    override fun onEnable() {
        super.onEnable()
        instance = this

        printStartupHeader()
        setupDataFolder()
        TranslationManager.init(this)
        setupConfig()
        setupUpdateTracker()

        if (isEnabled) {
            arenaHandler = ArenaHandler(this)
        }

        PlayerData.getInstance(this)
        startupKits()
        loadBoosts()
        registerCommands()
        registerListeners()

        startArenaHandler(this.config.getInt("mapDuration", 60))
        setupPlaceholders()
        setupModifiers()

        BStatsMetrics.registerMetrics()
        printReadyMessage()
    }

    override fun onDisable() {
        logger.info(TranslationManager.translate("plugin.disabled"))
        PlayerData.getInstance(this).mysqlHandler.disconnect()
    }

    private fun printStartupHeader() {
        logger.info("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
        logger.info("┃      🚀 KnockBackFFA Start      ┃")
        logger.info("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")
    }

    private fun printReadyMessage() {
        logger.info("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
        logger.info("┃    ✨ KnockBackFFA is Ready     ┃")
        logger.info("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")
    }

    fun setupDataFolder() {
        if (!dataFolder.exists()) {
            logger.info("1️⃣ First time setup: Extra logging of file creations!")
            logger.warning("⚠️ Data folder not found, creating...")
            dataFolder.mkdirs()
            logger.info("📁 Data folder created")
        }
    }

    private fun setupConfig() {
        try {
            if (!File(dataFolder, "config.yml").exists()) {
                saveResource("config.yml", false)
            }
            reloadConfig()
            saveDefaultConfig()
        } catch (ex: IllegalArgumentException) {
            logger.severe(TranslationManager.translate("plugin.config_error", "error" to ex.message.toString()))
        }
    }

    private fun startupKits() {
        val kitConfig = File(dataFolder, "kits.yml")
        if (!kitConfig.exists()) {
            logger.warning(TranslationManager.translate("plugin.kits_file_missing"))
            saveResource("kits.yml", false)
            logger.info(TranslationManager.translate("plugin.kits_file_created"))
        }
        kitManager = KitManager(this)
        logger.info(TranslationManager.translate("plugin.kits_loaded", "count" to kitManager.getAllKitNames().size))
    }

    private fun setupUpdateTracker() {
        UpdateTracker.init(this)
    }

    fun loadBoosts() {
        val boostConfig = File(dataFolder, "boosts.yml")
        if (!boostConfig.exists()) {
            logger.warning(TranslationManager.translate("plugin.boosts_file_missing"))
            saveResource("boosts.yml", false)
            logger.info(TranslationManager.translate("plugin.boosts_file_created"))
        }
        boostManager = BoostManager(this)
        boostManager.registerEvents(this)
        logger.info(TranslationManager.translate("plugin.boosts_loaded", "count" to boostManager.getAllBoosts().size))
        playerBoostManager = PlayerBoostManager(this)
        logger.info(TranslationManager.translate("plugin.player_boost_manager_initialized"))
    }

    private fun setupModifiers() {
        modifierManager = ModifierManager(this)
        modifierManager.registerEvents(this)
        logger.info(TranslationManager.translate("plugin.modifiers_loaded", "count" to modifierManager.getModifyObjects().size))
    }

    private fun registerCommands() {
        logger.info(TranslationManager.translate("plugin.setting_up_commands"))
        kbffaCommand(arenaHandler)
        kitSelectorCommand()
        shopCommand()
        boostsCommand()
        logger.info(TranslationManager.translate("plugin.commands_ready"))
    }

    private fun registerListeners() {
        logger.info(TranslationManager.translate("plugin.registering_events"))
        registerEvents(
            PlayerJoinListener(ScoreboardHandler(this), BossBarHandler(this)),
            PlayerQuitListener(ScoreboardHandler(this), BossBarHandler(this)),
            ScoreHandler(this),
            DeathBlock(),
            PlayerHandler(this)
        )
        logger.info(TranslationManager.translate("plugin.events_registered", "count" to 5))
    }

    private fun startArenaHandler(mapDuration: Int) {
        logger.info(TranslationManager.translate("plugin.starting_arena_handler"))
        arenaHandler.loadArenas()

        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            lastSwitchTime = Instant.now()
            nextSwitchTime = lastSwitchTime.plusSeconds(mapDuration.toLong())
            arenaHandler.switchArena()
        }, 0L, mapDuration * 20L)

        logger.info(TranslationManager.translate("plugin.arena_handler_ready", "duration" to mapDuration))
    }

    private fun setupPlaceholders() {
        val placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI")
        if (placeholderAPI != null) {
            PlaceHolderAPI(this).register()
            logger.info(TranslationManager.translate("plugin.placeholders_ready"))
        } else {
            logger.warning(TranslationManager.translate("plugin.placeholderapi_missing"))
            Bukkit.getPluginManager().disablePlugin(this)
        }
    }
}