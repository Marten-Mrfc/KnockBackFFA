package dev.marten_mrfcyt.knockbackffa.utils

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.mysql.MySQLHandler
import dev.marten_mrfcyt.knockbackffa.utils.mysql.StorageConfig
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*

class PlayerData private constructor(private val plugin: KnockBackFFA) {
    private val storageConfig = StorageConfig(plugin)
    internal val mysqlHandler = MySQLHandler(storageConfig, plugin)

    init {
        plugin.logger.info("Storage type: ${storageConfig.storageType}")
        if (storageConfig.storageType.lowercase() == "mysql") {
            mysqlHandler.connect()
            createPlayerDataTable()
        }
    }

    private val playerDataDirectory = File(plugin.dataFolder, "PlayerData").apply {
        if (!exists()) {
            mkdirs()
        }
    }
    private fun createPlayerDataTable() {
        val connection: Connection? = mysqlHandler.getConnection()
        if (connection != null) {
            val query = """
            CREATE TABLE IF NOT EXISTS player_data (
                player_id VARCHAR(36) NOT NULL,
                kit VARCHAR(255),
                PRIMARY KEY (player_id)
            );
        """.trimIndent()
            val statement: PreparedStatement = connection.prepareStatement(query)
            statement.executeUpdate()
            statement.close()
        }
    }
    fun getPlayerData(playerId: UUID): YamlConfiguration {
        return if (storageConfig.storageType.lowercase() == "mysql") {
            getPlayerDataFromMySQL(playerId)
        } else {
            getPlayerDataFromFile(playerId)
        }
    }

    private fun getPlayerDataFromFile(playerId: UUID): YamlConfiguration {
        val playerDataFile = File(playerDataDirectory, "$playerId.yml")
        if (!playerDataFile.exists()) {
            playerDataFile.createNewFile()
        }
        return YamlConfiguration.loadConfiguration(playerDataFile)
    }

    private fun getPlayerDataFromMySQL(playerId: UUID): YamlConfiguration {
        val connection: Connection? = mysqlHandler.getConnection()
        val playerData = YamlConfiguration()
        if (connection != null) {
            val query = "SELECT * FROM player_data WHERE player_id = ?"
            val statement: PreparedStatement = connection.prepareStatement(query)
            statement.setString(1, playerId.toString())
            val resultSet: ResultSet = statement.executeQuery()
            if (resultSet.next()) {
                playerData.set("kit", resultSet.getString("kit"))
            }
            resultSet.close()
            statement.close()
        }
        return playerData
    }

    fun savePlayerData(playerId: UUID, playerData: YamlConfiguration) {
        if (storageConfig.storageType.lowercase() == "mysql") {
            savePlayerDataToMySQL(playerId, playerData)
        } else {
            savePlayerDataToFile(playerId, playerData)
        }
    }

    private fun savePlayerDataToFile(playerId: UUID, playerData: YamlConfiguration) {
        val playerDataFile = File(playerDataDirectory, "$playerId.yml")
        playerData.save(playerDataFile)
    }

    private fun savePlayerDataToMySQL(playerId: UUID, playerData: YamlConfiguration) {
        val connection: Connection? = mysqlHandler.getConnection()
        if (connection != null) {
            val query = "REPLACE INTO player_data (player_id, kit) VALUES (?, ?)"
            val statement: PreparedStatement = connection.prepareStatement(query)
            statement.setString(1, playerId.toString())
            statement.setString(2, playerData.getString("kit"))
            statement.executeUpdate()
            statement.close()
        }
    }

    companion object {
        @Volatile
        private var instance: PlayerData? = null

        fun getInstance(plugin: KnockBackFFA): PlayerData {
            return instance ?: synchronized(this) {
                instance ?: PlayerData(plugin).also { instance = it }
            }
        }
    }
}