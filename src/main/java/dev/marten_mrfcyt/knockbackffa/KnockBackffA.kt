package dev.marten_mrfcyt.knockbackffa

import dev.marten_mrfcyt.knockbackffa.player.PlayerJoinListener
import dev.marten_mrfcyt.knockbackffa.player.ScoreHandler
import dev.marten_mrfcyt.knockbackffa.utils.PlaceHolderAPI
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import lirand.api.architecture.KotlinPlugin
import org.bukkit.Bukkit
import org.bukkit.event.Listener

class KnockBackFFA : KotlinPlugin() {
    override suspend fun onEnableAsync() {
        logger.info("KnockBackFFA enabled!")
        if (!dataFolder.exists() && !dataFolder.mkdir()) {
            logger.severe("Failed to create data folder!")
            return
        }
        try { saveDefaultConfig() }
        catch (ex: IllegalArgumentException) { logger.severe("Failed to save default config: ${ex.message}") }
        kbffaCommand()

        // Register all event listeners
        registerEvents(PlayerJoinListener())
        val placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI")
        if (placeholderAPI != null) {
            PlaceHolderAPI(this).register()
            logger.info("Placeholders registered -> KnockBackFFA has been enabled!")
        } else {
            logger.warning("Could not find PlaceholderAPI! This plugin is required.")
            Bukkit.getPluginManager().disablePlugin(this)
        }
    }

    override suspend fun onDisableAsync() {
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