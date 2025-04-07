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
            initializeDatabase()
        }
        startPeriodicSaving()
    }

    private fun initializeDatabase() {
        mysqlHandler.connect()
        createPlayerDataTable()
        checkAndMigrateDatabase()
        prepareStatements()
    }

    private fun prepareStatements() {
        try {
            mysqlHandler.getConnection()?.let { connection ->
                if (!connection.isValid(3)) {
                    plugin.logger.warning("Connection is invalid, reconnecting...")
                    mysqlHandler.connect()
                }

                val statements = mapOf(
                    "select" to "SELECT * FROM player_data WHERE player_id = ?",
                    "replace" to """
                        REPLACE INTO player_data (player_id, kit, deaths, kills, killstreak, max_killstreak,
                        coins, kd_ratio, owned_kits, boosts, kit_layouts, boost_timings) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    "sum_kills" to "SELECT SUM(kills) FROM player_data"
                )

                statements.forEach { (name, sql) ->
                    preparedStatements[name] = connection.prepareStatement(sql)
                }
            } ?: throw IllegalStateException("Database connection is null")
        } catch (e: Exception) {
            logError("Failed to prepare statements", e)
        }
    }

    private fun checkAndMigrateDatabase() {
        if (!useMySQL) return

        mysqlHandler.getConnection()?.let { connection ->
            try {
                val metaData = connection.metaData
                val expectedColumns = getExpectedColumns()

                // Check which columns exist
                val existingColumns = mutableSetOf<String>()
                metaData.getColumns(null, null, "player_data", null).use { resultSet ->
                    while (resultSet.next()) {
                        existingColumns.add(resultSet.getString("COLUMN_NAME").lowercase())
                    }
                }

                // Add any missing columns
                connection.createStatement().use { statement ->
                    var migrationsPerformed = false

                    expectedColumns.forEach { (columnName, columnType) ->
                        if (columnName != "player_id" && !existingColumns.contains(columnName.lowercase())) {
                            plugin.logger.info("Adding missing '$columnName' column to database...")
                            statement.executeUpdate("ALTER TABLE player_data ADD COLUMN $columnName $columnType")
                            migrationsPerformed = true
                        }
                    }

                    if (migrationsPerformed) {
                        plugin.logger.info("Database migrations completed successfully")
                    } else {
                        plugin.logger.info("Database schema is up to date")
                    }
                }
            } catch (e: Exception) {
                logError("Failed to migrate database", e)
            }
        } ?: plugin.logger.severe("Cannot check database structure: connection is null")
    }

    private fun getExpectedColumns(): Map<String, String> {
        return mapOf(
            "player_id" to "VARCHAR(36) NOT NULL PRIMARY KEY",
            "kit" to "VARCHAR(255)",
            "deaths" to "INT DEFAULT 0",
            "kills" to "INT DEFAULT 0",
            "killstreak" to "INT DEFAULT 0",
            "max_killstreak" to "INT DEFAULT 0",
            "coins" to "INT DEFAULT 0",
            "kd_ratio" to "DOUBLE DEFAULT 0",
            "owned_kits" to "TEXT",
            "boosts" to "TEXT",
            "kit_layouts" to "TEXT",
            "boost_timings" to "TEXT"
        )
    }

    private fun createPlayerDataTable() {
        try {
            mysqlHandler.getConnection()?.createStatement()?.use { statement ->
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
                        boost_timings TEXT,
                        PRIMARY KEY (player_id)
                    )
                """.trimIndent())
            } ?: throw IllegalStateException("Database connection is null")
        } catch (e: Exception) {
            logError("Failed to create player_data table", e)
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

        dataToSave.forEach { playerId ->
            val playerData = playerDataCache[playerId] ?: return@forEach
            if (useMySQL) {
                savePlayerDataToMySQL(playerId, playerData)
            } else {
                savePlayerDataToFile(playerId, playerData)
            }
        }
    }

    fun getPlayerDataModel(playerId: UUID): PlayerDataModel {
        return playerDataCache.computeIfAbsent(playerId) {
            if (useMySQL) getPlayerDataModelFromMySQL(playerId)
            else getPlayerDataModelFromFile(playerId)
        }
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
        mysqlHandler.getConnection()?.let { _ ->
            try {
                preparedStatements["select"]?.let { statement ->
                    statement.setString(1, playerId.toString())
                    statement.executeQuery().use { resultSet ->
                        if (resultSet.next()) {
                            return PlayerDataSerializer.fromResultSet(resultSet, playerId)
                        }
                    }
                }
            } catch (e: Exception) {
                logError("Error loading player data", e)
            }
        }
        return PlayerDataModel(playerId)
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
            logError("Error saving player data file", e)
        }
    }

    private fun savePlayerDataToMySQL(playerId: UUID, model: PlayerDataModel) {
        mysqlHandler.getConnection()?.let { _ ->
            try {
                preparedStatements["replace"]?.apply {
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
                    setString(12, PlayerDataSerializer.serializeBoostTimings(model.boostTimings))
                    executeUpdate()
                }
            } catch (e: Exception) {
                logError("Error saving player data to MySQL", e)
            }
        }
    }

    private fun serializeKitLayouts(kitLayouts: Map<String, Map<Int, Int>>): String {
        return kitLayouts.entries.joinToString(";") { (kitName, layout) ->
            "$kitName:" + layout.entries.joinToString(",") { (origSlot, newSlot) ->
                "$origSlot=$newSlot"
            }
        }
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
            return getMySQLTotalKills()
        } else {
            return getFileTotalKills()
        }
    }

    private fun getMySQLTotalKills(): Int {
        mysqlHandler.getConnection()?.let { _ ->
            try {
                preparedStatements["sum_kills"]?.executeQuery()?.use { resultSet ->
                    if (resultSet.next()) {
                        return resultSet.getInt(1)
                    }
                }
            } catch (e: Exception) {
                logError("Error getting total kills", e)
            }
        }
        return 0
    }

    private fun getFileTotalKills(): Int {
        if (playerDataCache.isNotEmpty() && playerDataDirectory.listFiles()?.size == playerDataCache.size) {
            return playerDataCache.values.sumOf { it.kills }
        }

        var totalKills = 0
        playerDataDirectory.listFiles()?.forEach { file ->
            try {
                val playerId = UUID.fromString(file.nameWithoutExtension)
                val config = YamlConfiguration.loadConfiguration(file)
                totalKills += PlayerDataSerializer.fromYaml(config, playerId).kills
            } catch (_: IllegalArgumentException) {
            }
        }
        return totalKills
    }

    private fun logError(message: String, e: Exception) {
        plugin.logger.severe("$message: ${e.message}")
        e.printStackTrace()
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