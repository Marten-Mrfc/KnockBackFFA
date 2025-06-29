package dev.marten_mrfcyt.knockbackffa.utils

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.models.PlayerDataModel
import dev.marten_mrfcyt.knockbackffa.utils.mysql.MySQLHandler
import dev.marten_mrfcyt.knockbackffa.utils.mysql.StorageConfig
import mlib.api.utilities.debug
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
        debug("Initializing PlayerData with storage type: ${storageConfig.storageType}")
        if (useMySQL) {
            initializeDatabase()
        }
        startPeriodicSaving()
        debug( "PlayerData initialization complete")
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
                    "select" to "SELECT * FROM player_data WHERE player_id = ?",                    "replace" to """
                        REPLACE INTO player_data (player_id, kit, deaths, kills, assists, killstreak, max_killstreak,
                        coins, kd_ratio, damage_dealt, owned_kits, boosts, kit_layouts, boost_timings) 
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    "sum_kills" to "SELECT SUM(kills) FROM player_data"
                )

                statements.forEach { (name, sql) ->
                    preparedStatements[name] = connection.prepareStatement(sql)
                }
            } ?: throw IllegalStateException("Database connection is null")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to prepare SQL statements", e)
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
                throw IllegalStateException("Failed to check or migrate database", e)
            }
        } ?: plugin.logger.severe("Cannot check database structure: connection is null")
    }    private fun getExpectedColumns(): Map<String, String> {
        return mapOf(
            "player_id" to "VARCHAR(36) NOT NULL PRIMARY KEY",
            "kit" to "VARCHAR(255)",
            "deaths" to "INT DEFAULT 0",
            "kills" to "INT DEFAULT 0",
            "assists" to "INT DEFAULT 0",
            "killstreak" to "INT DEFAULT 0",
            "max_killstreak" to "INT DEFAULT 0",
            "coins" to "INT DEFAULT 0",
            "kd_ratio" to "DOUBLE DEFAULT 0",
            "damage_dealt" to "DOUBLE DEFAULT 0",
            "owned_kits" to "TEXT",
            "boosts" to "TEXT",
            "kit_layouts" to "TEXT",
            "boost_timings" to "TEXT"
        )
    }
    private fun createPlayerDataTable() {
        try {
            debug( "Creating player_data table if it doesn't exist")
            mysqlHandler.getConnection()?.createStatement()?.use { statement ->                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_data (
                        player_id VARCHAR(36) NOT NULL,
                        kit VARCHAR(255),
                        deaths INT DEFAULT 0,
                        kills INT DEFAULT 0,
                        assists INT DEFAULT 0,
                        killstreak INT DEFAULT 0,
                        max_killstreak INT DEFAULT 0,
                        coins INT DEFAULT 0,
                        kd_ratio DOUBLE DEFAULT 0,
                        damage_dealt DOUBLE DEFAULT 0,
                        owned_kits TEXT,
                        boosts TEXT,
                        kit_layouts TEXT,
                        boost_timings TEXT,
                        PRIMARY KEY (player_id)
                    )
                """.trimIndent())
                mlib.api.utilities.debug("player_data table created or verified")
            } ?: throw IllegalStateException("Database connection is null")
        } catch (e: Exception) {
            mlib.api.utilities.debug("Error creating player_data table: ${e.message}")
        }
    }

    private fun startPeriodicSaving() {
        object : BukkitRunnable() {
            override fun run() = saveAllDirtyData()
        }.runTaskTimerAsynchronously(plugin, saveInterval, saveInterval)
    }


    fun getPlayerDataModel(playerId: UUID): PlayerDataModel {
        return playerDataCache.computeIfAbsent(playerId) {
            if (useMySQL) getPlayerDataModelFromMySQL(playerId)
            else getPlayerDataModelFromFile(playerId)
        }
    }
    private fun getPlayerDataModelFromFile(playerId: UUID): PlayerDataModel {
        val playerDataFile = File(playerDataDirectory, "$playerId.yml")
        debug( "Loading player data for $playerId from file")
        return if (!playerDataFile.exists()) {
            playerDataFile.createNewFile()
            debug( "Created new player data file for $playerId")
            PlayerDataModel(playerId)
        } else {
            val config = YamlConfiguration.loadConfiguration(playerDataFile)
            debug( "Loaded existing player data for $playerId from file")
            PlayerDataSerializer.fromYaml(config, playerId)
        }
    }

    private fun getPlayerDataModelFromMySQL(playerId: UUID): PlayerDataModel {
        debug("Loading player data for $playerId from MySQL")
            mysqlHandler.getConnection()?.let { _ ->
                try {
                    preparedStatements["select"]?.let { statement ->
                        statement.setString(1, playerId.toString())
                        statement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                debug("Found existing player data in MySQL for $playerId")
                                return PlayerDataSerializer.fromResultSet(resultSet, playerId)
                            }
                        }
                    }
                    debug("No data found in MySQL for $playerId, creating new model")
                } catch (_: Exception) {
                    debug("Error loading player data from MySQL for $playerId")
                }
        }
        return PlayerDataModel(playerId)

    }
    fun savePlayerDataModel(playerId: UUID, model: PlayerDataModel) {
        playerDataCache[playerId] = model
        dirtyPlayerData.add(playerId)
        debug("Marked player data for $playerId as dirty (pending save)")
    }

    fun savePlayerDataToFile(playerId: UUID, model: PlayerDataModel) {
        try {
            val playerDataFile = File(playerDataDirectory, "$playerId.yml")
            debug("Saving player data for $playerId to file")
            PlayerDataSerializer.toYaml(model).save(playerDataFile)
            debug("Successfully saved player data for $playerId to file")
        } catch (e: Exception) {
            throw IllegalStateException("Error saving player data to file", e)
        }
    }    fun savePlayerDataToMySQL(playerId: UUID, model: PlayerDataModel) {
        mysqlHandler.getConnection()?.let { _ ->
            try {
                preparedStatements["replace"]?.apply {
                    setString(1, playerId.toString())
                    setString(2, model.kit)
                    setInt(3, model.deaths)
                    setInt(4, model.kills)
                    setInt(5, model.assists)
                    setInt(6, model.killstreak)
                    setInt(7, model.maxKillstreak)
                    setInt(8, model.coins)
                    setDouble(9, model.kdRatio)
                    setDouble(10, model.damageDealt)
                    setString(11, model.ownedKits.joinToString(","))
                    setString(12, model.boosts.joinToString(","))
                    setString(13, serializeKitLayouts(model.kitLayouts))
                    setString(14, PlayerDataSerializer.serializeBoostTimings(model.boostTimings))
                    executeUpdate()
                }
            } catch (e: Exception) {
                throw IllegalStateException("Error saving player data to MySQL", e)
            }
        }
    }
    fun saveAllDirtyData() {
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
    fun serializeKitLayouts(kitLayouts: Map<String, Map<Int, Int>>): String {
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
    
    fun saveAllSync() {
        if (dirtyPlayerData.isEmpty()) {
            debug( "No dirty player data to save")
            return
        }
        
        val dataToSave = HashSet(dirtyPlayerData)
        dirtyPlayerData.removeAll(dataToSave)
        
        debug( "Saving data for ${dataToSave.size} players synchronously")
        
        dataToSave.forEach { playerId ->
            val playerData = playerDataCache[playerId] ?: return@forEach
            if (useMySQL) {
                debug("Saving data for player $playerId to MySQL")
                savePlayerDataToMySQL(playerId, playerData)
            } else {
                debug( "Saving data for player $playerId to file")
                savePlayerDataToFile(playerId, playerData)
            }
        }
    }

    fun getTotalKills(): Int {
        if (useMySQL) {
            return getMySQLTotalKills()
        } else {
            return getFileTotalKills()
        }
    }
    fun getMySQLTotalKills(): Int {
        mysqlHandler.getConnection()?.let { _ ->
            try {
                preparedStatements["sum_kills"]?.executeQuery()?.use { resultSet ->
                    if (resultSet.next()) {
                        return resultSet.getInt(1)
                    }
                }
            } catch (e: Exception) {
                throw IllegalStateException("Error fetching total kills from MySQL", e)
            }
        }
        return 0
    }

    fun getFileTotalKills(): Int {
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
        
        fun getInstanceIfInitialized(): PlayerData? {
            return instance
        }
    }
}