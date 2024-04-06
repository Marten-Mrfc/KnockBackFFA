package dev.marten_mrfcyt.knockbackffa

import lirand.api.architecture.KotlinPlugin

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
    }

    override suspend fun onDisableAsync() {
        logger.info("KnockBackFFA has been disabled!")
    }
}
