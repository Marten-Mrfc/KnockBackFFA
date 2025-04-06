package dev.marten_mrfcyt.knockbackffa.utils

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.models.PlayerDataModel
import dev.marten_mrfcyt.knockbackffa.utils.mysql.MySQLHandler
import dev.marten_mrfcyt.knockbackffa.utils.mysql.StorageConfig
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.sql.PreparedStatement
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PlayerData private constructor(private val plugin: KnockBackFFA) {
    private val storageConfig = StorageConfig(plugin)
    internal val mysqlHandler = MySQLHandler(storageConfig, plugin)

    private val playerDataCache = ConcurrentHashMap<UUID, PlayerDataModel>()
    private val dirtyPlayerData = Collections.newSetFromMap(ConcurrentHashMap<UUID, Boolean>())
    private val preparedStatements = HashMap<String, PreparedStatement>()
    private val saveInterval = plugin.config.getLong("playerdata.save_interval", 100L)
    private val playerDataDirectory = File(plugin.dataFolder, "PlayerData").apply {
        if (!exists()) mkdirs()
    }

    private val useMySQL get() = storageConfig.storageType.lowercase() == "mysql"

    init {
        plugin.logger.info("📃 Storage type: ${storageConfig.storageType}")
        if (useMySQL) {
            mysqlHandler.connect()
            createPlayerDataTable()
            prepareStatements()
        }
        startPeriodicSaving()
    }
    private fun prepareStatements() {
        try {
            mysqlHandler.getConnection()?.let { connection ->
                if (!connection.isValid(3)) {
                    plugin.logger.warning("Connection is invalid, reconnecting...")
                    mysqlHandler.connect()
                }

                preparedStatements["select"] = connection.prepareStatement("SELECT * FROM player_data WHERE player_id = ?")
                preparedStatements["replace"] = connection.prepareStatement("""
                    REPLACE INTO player_data (player_id, kit, deaths, kills, killstreak, max_killstreak,
                    coins, kd_ratio, owned_kits, boosts, kit_layouts) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent())
                preparedStatements["sum_kills"] = connection.prepareStatement("SELECT SUM(kills) FROM player_data")
            } ?: throw IllegalStateException("Database connection is null")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to prepare statements: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createPlayerDataTable() {
        try {
            mysqlHandler.getConnection()?.let { connection ->
                // Don't use connection.use{} here as that will close the connection
                // We want to keep the connection open
                val statement = connection.createStatement()
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_data (
                        player_id VARCHAR(36) NOT NULL,
                        kit VARCHAR(255),
                        deaths INT DEFAULT 0,
                        kills INT DEFAULT 0,
                        killstreak INT DEFAULT 0,
                        max_killstreak INT DEFAULT 0,
                        coins INT DEFAULT 0,
                        kd_ratio DOUBLE DEFAULT 0,
                        owned_kits TEXT,
                        boosts TEXT,
                        kit_layouts TEXT,
                        PRIMARY KEY (player_id)
                    )
                """.trimIndent())
                statement.close() // Just close the statement, not the connection
            } ?: throw IllegalStateException("Database connection is null")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to create player_data table: ${e.message}")
            e.printStackTrace()
        }
    }
    private fun startPeriodicSaving() {
        object : BukkitRunnable() {
            override fun run() = saveAllDirtyData()
        }.runTaskTimerAsynchronously(plugin, saveInterval, saveInterval)
    }

    private fun saveAllDirtyData() {
        if (dirtyPlayerData.isEmpty()) return

        val dataToSave = HashSet(dirtyPlayerData)
        dirtyPlayerData.removeAll(dataToSave)

        for (playerId in dataToSave) {
            val playerData = playerDataCache[playerId] ?: continue
            if (useMySQL) {
                savePlayerDataToMySQL(playerId, playerData)
            } else {
                savePlayerDataToFile(playerId, playerData)
            }
        }
    }

    // Legacy method for backward compatibility
    fun getPlayerData(playerId: UUID): YamlConfiguration =
        PlayerDataSerializer.toYaml(getPlayerDataModel(playerId))

    fun getPlayerDataModel(playerId: UUID): PlayerDataModel {
        playerDataCache[playerId]?.let { return it }

        val model = if (useMySQL) getPlayerDataModelFromMySQL(playerId)
        else getPlayerDataModelFromFile(playerId)

        playerDataCache[playerId] = model
        return model
    }

    private fun getPlayerDataModelFromFile(playerId: UUID): PlayerDataModel {
        val playerDataFile = File(playerDataDirectory, "$playerId.yml")
        return if (!playerDataFile.exists()) {
            playerDataFile.createNewFile()
            PlayerDataModel(playerId)
        } else {
            val config = YamlConfiguration.loadConfiguration(playerDataFile)
            PlayerDataSerializer.fromYaml(config, playerId)
        }
    }

    private fun getPlayerDataModelFromMySQL(playerId: UUID): PlayerDataModel {
        mysqlHandler.getConnection()?.let { connection ->
            try {
                val statement = preparedStatements["select"] ?: return@let
                statement.setString(1, playerId.toString())

                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return PlayerDataSerializer.fromResultSet(resultSet, playerId)
                    }
                }
            } catch (e: Exception) {
                plugin.logger.severe("Error loading player data: ${e.message}")
            }
        }
        return PlayerDataModel(playerId)
    }

    // Legacy method for backward compatibility
    fun savePlayerData(playerId: UUID, playerData: YamlConfiguration) {
        savePlayerDataModel(playerId, PlayerDataSerializer.fromYaml(playerData, playerId))
    }

    fun savePlayerDataModel(playerId: UUID, model: PlayerDataModel) {
        playerDataCache[playerId] = model
        dirtyPlayerData.add(playerId)
    }

    private fun savePlayerDataToFile(playerId: UUID, model: PlayerDataModel) {
        try {
            val playerDataFile = File(playerDataDirectory, "$playerId.yml")
            PlayerDataSerializer.toYaml(model).save(playerDataFile)
        } catch (e: Exception) {
            plugin.logger.severe("Error saving player data file: ${e.message}")
        }
    }

    private fun savePlayerDataToMySQL(playerId: UUID, model: PlayerDataModel) {
        mysqlHandler.getConnection()?.let { connection ->
            try {
                val statement = preparedStatements["replace"] ?: return@let

                statement.apply {
                    setString(1, playerId.toString())
                    setString(2, model.kit)
                    setInt(3, model.deaths)
                    setInt(4, model.kills)
                    setInt(5, model.killstreak)
                    setInt(6, model.maxKillstreak)
                    setInt(7, model.coins)
                    setDouble(8, model.kdRatio)
                    setString(9, model.ownedKits.joinToString(","))
                    setString(10, model.boosts.joinToString(","))
                    setString(11, serializeKitLayouts(model.kitLayouts))
                }.executeUpdate()
            } catch (e: Exception) {
                plugin.logger.severe("Error saving player data to MySQL: ${e.message}")
            }
        }
    }

    private fun serializeKitLayouts(kitLayouts: Map<String, Map<Int, Int>>): String {
        val result = StringBuilder()

        kitLayouts.entries.forEachIndexed { kitIndex, (kitName, layout) ->
            result.append(kitName)
            result.append(":")

            layout.entries.forEachIndexed { slotIndex, (origSlot, newSlot) ->
                result.append("$origSlot=$newSlot")
                if (slotIndex < layout.size - 1) {
                    result.append(",")
                }
            }

            if (kitIndex < kitLayouts.size - 1) {
                result.append(";")
            }
        }

        return result.toString()
    }

    fun clearCache(playerId: UUID) {
        if (dirtyPlayerData.remove(playerId)) {
            playerDataCache[playerId]?.let { playerData ->
                if (useMySQL) savePlayerDataToMySQL(playerId, playerData)
                else savePlayerDataToFile(playerId, playerData)
            }
        }
        playerDataCache.remove(playerId)
    }

    fun saveAll() = saveAllDirtyData()

    fun getTotalKills(): Int {
        if (useMySQL) {
            mysqlHandler.getConnection()?.let { connection ->
                try {
                    val statement = preparedStatements["sum_kills"] ?: return 0
                    statement.executeQuery().use { resultSet ->
                        if (resultSet.next()) {
                            return resultSet.getInt(1)
                        }
                    }
                } catch (e: Exception) {
                    plugin.logger.severe("Error getting total kills: ${e.message}")
                }
            }
            return 0
        } else {
            if (playerDataCache.isNotEmpty() && playerDataDirectory.listFiles()?.size == playerDataCache.size) {
                return playerDataCache.values.sumOf { it.kills }
            }

            var totalKills = 0
            playerDataDirectory.listFiles()?.forEach { file ->
                try {
                    val playerId = UUID.fromString(file.nameWithoutExtension)
                    val config = YamlConfiguration.loadConfiguration(file)
                    val model = PlayerDataSerializer.fromYaml(config, playerId)
                    totalKills += model.kills
                } catch (e: IllegalArgumentException) {
                }
            }
            return totalKills
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