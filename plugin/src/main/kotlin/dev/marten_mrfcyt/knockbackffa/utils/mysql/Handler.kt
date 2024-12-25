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
    }

    fun disconnect() {
        if(connection == null) return
        plugin.logger.info("Disconnecting from MySQL database...")
        try {
            connection?.close()
            plugin.logger.info("Disconnected from MySQL database!")
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to disconnect from MySQL database!")
            e.printStackTrace()
        }
    }

    fun getConnection(): Connection? {
        return connection
    }
}
