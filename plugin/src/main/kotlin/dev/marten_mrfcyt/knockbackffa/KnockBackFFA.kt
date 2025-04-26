package dev.marten_mrfcyt.knockbackffa

import dev.marten_mrfcyt.knockbackffa.arena.editor.ArenaCreationHandler
import dev.marten_mrfcyt.knockbackffa.arena.ArenaHandler
import dev.marten_mrfcyt.knockbackffa.arena.ArenaInitializer
import dev.marten_mrfcyt.knockbackffa.arena.editor.SelectionManager
import dev.marten_mrfcyt.knockbackffa.boosts.managers.BoostManager
import dev.marten_mrfcyt.knockbackffa.boosts.managers.PlayerBoostManager
import dev.marten_mrfcyt.knockbackffa.kits.KitLayoutManager
import dev.marten_mrfcyt.knockbackffa.kits.managers.KitManager
import dev.marten_mrfcyt.knockbackffa.kits.managers.ModifierManager
import dev.marten_mrfcyt.knockbackffa.player.*
import dev.marten_mrfcyt.knockbackffa.utils.*
import mlib.api.architecture.KotlinPlugin
import mlib.api.architecture.extensions.registerEvents
import org.bukkit.Bukkit
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
        private set
    lateinit var playerBoostManager: PlayerBoostManager
        private set
    lateinit var boostManager: BoostManager
        private set
    lateinit var arenaCreationHandler: ArenaCreationHandler
        private set
    lateinit var selectionManager: SelectionManager
        private set
    private lateinit var arenaInitializer: ArenaInitializer


    override fun onEnable() {
        super.onEnable()
        instance = this

        printStartupHeader()
        
        // Set up resources and config first
        setupDataFolder()
        TranslationManager.init(this)
        setupConfig()
        setupUpdateTracker()

        if (isEnabled) {
            arenaHandler = ArenaHandler(this)
            selectionManager = SelectionManager(this)
            arenaCreationHandler = ArenaCreationHandler(this)
            arenaInitializer = ArenaInitializer(this)
        }

        PlayerData.getInstance(this)
        startupKits()
        loadBoosts()
        registerCommands()
        registerListeners()
        arenaInitializer.initializeArenas(this.config.getInt("mapDuration", 60))

        setupPlaceholders()
        setupModifiers()

        BStatsMetrics.registerMetrics()
        printReadyMessage()
    }
    override fun onDisable() {
        try {
            logger.info("Saving all player data before shutdown...")
            PlayerData.getInstance(this).saveAll()
            logger.info("Player data saved successfully")
        } catch (e: Exception) {
            logger.severe("Error saving player data: ${e.message}")
            e.printStackTrace()
        }
        
        try {
            logger.info("Closing database connections...")
            PlayerData.getInstance(this).mysqlHandler.disconnect()
            logger.info("Database connections closed")
        } catch (e: Exception) {
            logger.severe("Error closing database connections: ${e.message}")
            e.printStackTrace()
        }
        
        logger.info(TranslationManager.translate("plugin.disabled"))
        printStoppedMessage()
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

    private fun printStoppedMessage() {
        logger.info("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
        logger.info("┃    ❌ KnockBackFFA Stopped      ┃")
        logger.info("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")
    }

    fun setupDataFolder() {
        if (!dataFolder.exists()) {
            logger.info("1️⃣ First time setup: Extra logging of file creations!")
            logger.warning("⚠️ Data folder not found, creating...")
            dataFolder.mkdirs()
            logger.info("📁 Data folder created")
        }
        
        ensureResourceFileExists("config.yml")
        ensureResourceFileExists("kits.yml")
        ensureResourceFileExists("boosts.yml")
        
        val langFolder = File(dataFolder, "lang")
        if (!langFolder.exists()) {
            langFolder.mkdirs()
        }
    }
    
    private fun ensureResourceFileExists(fileName: String) {
        val file = File(dataFolder, fileName)
        if (!file.exists()) {
            logger.warning("⚠️ $fileName not found, creating from template...")
            try {
                saveResource(fileName, false)
                logger.info("📄 $fileName created successfully")
            } catch (e: Exception) {
                logger.severe("❌ Failed to create $fileName: ${e.message}")
                e.printStackTrace()
            }
        }
    }
      private fun setupConfig() {
        try {
            if (!File(dataFolder, "config.yml").exists()) {
                ensureResourceFileExists("config.yml")
            }
            reloadConfig()
            saveDefaultConfig()
        } catch (ex: IllegalArgumentException) {
            logger.severe(TranslationManager.translate("plugin.config_error", "error" to ex.message.toString()))
        }
    }

    private fun startupKits() {
        ensureResourceFileExists("kits.yml")
        kitManager = KitManager(this)
        logger.info(TranslationManager.translate("plugin.kits_loaded", "count" to kitManager.getAllKitNames().size))
    }

    private fun setupUpdateTracker() {
        UpdateTracker.init(this)
    }

    fun loadBoosts() {
        ensureResourceFileExists("boosts.yml")
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
            PlayerHandler(this),
            KitLayoutManager(this),
        )

        arenaInitializer.setupArenaSystem()

        logger.info(TranslationManager.translate("plugin.events_registered", "count" to 5))
    }

    private fun setupPlaceholders() {
        val placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI")
        if (placeholderAPI != null && placeholderAPI.isEnabled) {
            val expansion = PlaceHolderAPI(this)
            if (expansion.register()) {
                logger.info(TranslationManager.translate("plugin.placeholders_ready"))
            } else {
                logger.warning(TranslationManager.translate("plugin.placeholders_failed_registration"))
            }
        } else {
            logger.warning(TranslationManager.translate("plugin.placeholderapi_missing"))
            Bukkit.getPluginManager().disablePlugin(this)
        }
    }
}