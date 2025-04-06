package dev.marten_mrfcyt.knockbackffa.utils

import dev.marten_mrfcyt.knockbackffa.utils.models.PlayerDataModel
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.sql.ResultSet
import java.util.UUID

object PlayerDataSerializer {
    // Convert from ResultSet to Model
    fun fromResultSet(rs: ResultSet, playerId: UUID): PlayerDataModel {
        return PlayerDataModel(
            playerId = playerId,
            kit = rs.getString("kit"),
            deaths = rs.getInt("deaths"),
            kills = rs.getInt("kills"),
            killstreak = rs.getInt("killstreak"),
            maxKillstreak = rs.getInt("max_killstreak"),
            coins = rs.getInt("coins"),
            kdRatio = rs.getDouble("kd_ratio"),
            ownedKits = rs.getString("owned_kits")?.splitToList() ?: emptyList(),
            boosts = rs.getString("boosts")?.splitToList() ?: emptyList(),
        )
    }

    fun fromYaml(config: YamlConfiguration, playerId: UUID): PlayerDataModel {
        val model = PlayerDataModel(playerId = playerId)

        model.kit = config.getString("kit")
        model.deaths = config.getInt("deaths")
        model.kills = config.getInt("kills")
        model.killstreak = config.getInt("killstreak")
        model.maxKillstreak = config.getInt("max-killstreak")
        model.coins = config.getInt("coins")
        model.kdRatio = config.getDouble("kd-ratio")
        model.ownedKits = config.getStringList("owned_kits")
        model.boosts = config.getStringList("boosts")

        return model
    }

    // Convert from Model to YamlConfiguration
    fun toYaml(model: PlayerDataModel): YamlConfiguration {
        val config = YamlConfiguration()

        // Basic properties
        config.set("kit", model.kit)
        config.set("deaths", model.deaths)
        config.set("kills", model.kills)
        config.set("killstreak", model.killstreak)
        config.set("max-killstreak", model.maxKillstreak)
        config.set("coins", model.coins)
        config.set("kd-ratio", model.kdRatio)
        config.set("owned_kits", model.ownedKits)
        config.set("boosts", model.boosts)

        // Kit layouts

        return config
    }


    // Helper for array fields
    private fun String.splitToList(): List<String> {
        return if (this.isNotEmpty()) this.split(",").map { it.trim() } else emptyList()
    }
}