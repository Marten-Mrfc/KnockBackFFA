package dev.marten_mrfcyt.knockbackffa

import dev.marten_mrfcyt.knockbackffa.player.PlayerJoinListener
import org.bukkit.Bukkit
import lirand.api.architecture.KotlinPlugin
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
    }

    override suspend fun onDisableAsync() {
        logger.info("KnockBackFFA has been disabled!")
    }

    private fun registerEvents(vararg listeners: Listener) {
        val pluginManager = Bukkit.getPluginManager()
        for (listener in listeners) {
            pluginManager.registerEvents(listener, this)
        }
    }
}