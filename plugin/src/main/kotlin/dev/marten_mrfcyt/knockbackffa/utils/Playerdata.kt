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
import kotlin.text.set

class PlayerData private constructor(private val plugin: KnockBackFFA) {
    private val storageConfig = StorageConfig(plugin)
    internal val mysqlHandler = MySQLHandler(storageConfig, plugin)

    init {
        plugin.logger.info("📃 Storage type: ${storageConfig.storageType}")
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
            deaths INT DEFAULT 0,
            kills INT DEFAULT 0,
            killstreak INT DEFAULT 0,
            max_killstreak INT DEFAULT 0,
            coins INT DEFAULT 0,
            kd_ratio DOUBLE DEFAULT 0.0,
            owned_kits TEXT,
            boosts TEXT,
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
                playerData.set("deaths", resultSet.getInt("deaths"))
                playerData.set("kills", resultSet.getInt("kills"))
                playerData.set("killstreak", resultSet.getInt("killstreak"))
                playerData.set("max-killstreak", resultSet.getInt("max_killstreak"))
                playerData.set("coins", resultSet.getInt("coins"))
                playerData.set("kd-ratio", resultSet.getDouble("kd_ratio"))
                playerData.set("owned_kits", resultSet.getString("owned_kits").split(",").map { it.trim() })
                playerData.set("boosts", resultSet.getString("boosts").split(",").map { it.trim() })
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
            val query = """
        REPLACE INTO player_data (player_id, kit, deaths, kills, killstreak, max_killstreak, coins, kd_ratio, owned_kits, boosts)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
            val statement: PreparedStatement = connection.prepareStatement(query)
            statement.setString(1, playerId.toString())
            statement.setString(2, playerData.getString("kit"))
            statement.setInt(3, playerData.getInt("deaths"))
            statement.setInt(4, playerData.getInt("kills"))
            statement.setInt(5, playerData.getInt("killstreak"))
            statement.setInt(6, playerData.getInt("max-killstreak"))
            statement.setInt(7, playerData.getInt("coins"))
            statement.setDouble(8, playerData.getDouble("kd-ratio"))
            val ownedKits = playerData.getStringList("owned_kits")
            statement.setString(9, ownedKits.joinToString(","))
            val boosts = playerData.getStringList("boosts")
            statement.setString(10, boosts.joinToString(","))

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

    fun getTotalKills(): Int {
        // get via file or mysql
        var totalKills = 0
        if (storageConfig.storageType.lowercase() == "mysql") {
            val connection: Connection? = mysqlHandler.getConnection()
            if (connection != null) {
                val query = "SELECT SUM(kills) FROM player_data"
                val statement: PreparedStatement = connection.prepareStatement(query)
                val resultSet: ResultSet = statement.executeQuery()
                if (resultSet.next()) {
                    totalKills = resultSet.getInt(1)
                }
                resultSet.close()
                statement.close()
            }
        } else {
            playerDataDirectory.listFiles()?.forEach { file ->
                val playerData = YamlConfiguration.loadConfiguration(file)
                totalKills += playerData.getInt("kills")
            }
        }
        return totalKills
    }
}