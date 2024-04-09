package dev.marten_mrfcyt.knockbackffa.utils

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*

class PlayerData(plugin: KnockBackFFA) {
    // Player data directory
    private val playerDataDirectory = File(plugin.dataFolder, "PlayerData")

    // Get player data
    fun getPlayerData(playerId: UUID): YamlConfiguration {
        // Load player data from file
        val playerDataFile = File(playerDataDirectory, "$playerId.yml")
        if (!playerDataFile.exists()) {
            playerDataFile.createNewFile()
        }
        return YamlConfiguration.loadConfiguration(playerDataFile)
    }

    // Save player data
    fun savePlayerData(playerId: UUID, playerData: YamlConfiguration, plugin: KnockBackFFA) {
        val playerDataFile = File(playerDataDirectory, "$playerId.yml")
        plugin.logger.info("Saving player data for $playerId...")
        playerData.save(playerDataFile)
    }
}