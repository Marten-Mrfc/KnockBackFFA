package dev.marten_mrfcyt.knockbackffa.utils.mysql

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA

class StorageConfig(plugin: KnockBackFFA) {
    val storageType: String
    val mysqlHost: String
    val mysqlPort: Int
    val mysqlDatabase: String
    val mysqlUser: String
    val mysqlPassword: String

    init {
        val config = plugin.config
        storageType = config.getString("storage.type", "file")!!
        mysqlHost = config.getString("storage.mysql.host", "localhost")!!
        mysqlPort = config.getInt("storage.mysql.port", 3306)
        mysqlDatabase = config.getString("storage.mysql.database", "knockbackffa")!!
        mysqlUser = config.getString("storage.mysql.user", "root")!!
        mysqlPassword = config.getString("storage.mysql.password", "")!!
    }
}