package dev.marten_mrfcyt.knockbackffa.utils.mysql

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class MySQLHandler(private val config: StorageConfig, private val plugin: KnockBackFFA) {
    private var connection: Connection? = null

    fun connect() {
        plugin.logger.info("Connecting to MySQL database...")
        try {
            val connectionWithoutDB = DriverManager.getConnection(
                "jdbc:mysql://${config.mysqlHost}:${config.mysqlPort}/",
                config.mysqlUser,
                config.mysqlPassword
            )

            val statement = connectionWithoutDB.createStatement()
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS ${config.mysqlDatabase}")
            statement.close()
            connectionWithoutDB.close()

            connection = DriverManager.getConnection(
                "jdbc:mysql://${config.mysqlHost}:${config.mysqlPort}/${config.mysqlDatabase}",
                config.mysqlUser,
                config.mysqlPassword
            )
            plugin.logger.info("Connected to MySQL database!")
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to connect to MySQL database!")
            e.printStackTrace()
        }
    }    fun disconnect() {
        if(connection == null) return
        plugin.logger.info("Disconnecting from MySQL database...")
        try {
            if (!connection!!.isClosed) {
                connection?.close()
                plugin.logger.info("Disconnected from MySQL database!")
            } else {
                plugin.logger.info("MySQL connection was already closed.")
            }
            connection = null
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to disconnect from MySQL database!")
            e.printStackTrace()
        } catch (e: NoClassDefFoundError) {
            plugin.logger.warning("H2 class not found during database disconnect: ${e.message}")
            plugin.logger.warning("This is likely due to a missing dependency - check that H2 database is in your classpath")
            connection = null
        } catch (e: Exception) {
            plugin.logger.severe("Unexpected error during database disconnect: ${e.message}")
            e.printStackTrace()
            connection = null
        }
    }fun getConnection(): Connection? {
        try {
            if (connection == null || connection!!.isClosed) {
                connect()
            }
            if (connection != null && !connection!!.isValid(5)) {
                plugin.logger.warning("MySQL connection is invalid, attempting to reconnect...")
                connect()
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to check/restore database connection: ${e.message}")
            e.printStackTrace()
        } catch (e: NoClassDefFoundError) {
            plugin.logger.warning("H2 class not found during connection check: ${e.message}")
            plugin.logger.warning("This error indicates missing dependencies - make sure H2 database is in your classpath")
        } catch (e: Exception) {
            plugin.logger.severe("Unexpected error checking database connection: ${e.message}")
            e.printStackTrace()
        }
        return connection
    }
}