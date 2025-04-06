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
            kitLayouts = rs.getString("kit_layouts")?.parseKitLayoutsString() ?: emptyMap()
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
        val layoutsSection = config.getConfigurationSection("kit_layouts")
        if (layoutsSection != null) {
            model.kitLayouts = parseKitLayouts(layoutsSection)
        }

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
        model.kitLayouts.forEach { (kitName, layout) ->
            layout.forEach { (originalSlot, newSlot) ->
                config.set("kit_layouts.$kitName.$originalSlot", newSlot)
            }
        }

        return config
    }

    // Helper for parsing kit layouts from database string
    private fun String.parseKitLayoutsString(): Map<String, Map<Int, Int>> {
        val result = mutableMapOf<String, MutableMap<Int, Int>>()
        if (isEmpty()) return result

        // Split by kit (format: "kitName:0=1,2=3;kitName2:4=5,6=7")
        split(";").forEach { kitEntry ->
            val kitParts = kitEntry.split(":", limit = 2)
            if (kitParts.size != 2) return@forEach

            val kitName = kitParts[0]
            val layoutMap = mutableMapOf<Int, Int>()

            // Process slot mappings
            kitParts[1].split(",").forEach { slotMapping ->
                val slotParts = slotMapping.split("=", limit = 2)
                if (slotParts.size != 2) return@forEach

                try {
                    val originalSlot = slotParts[0].toInt()
                    val newSlot = slotParts[1].toInt()
                    layoutMap[originalSlot] = newSlot
                } catch (e: NumberFormatException) {
                    // Skip invalid entries
                }
            }

            if (layoutMap.isNotEmpty()) {
                result[kitName] = layoutMap
            }
        }

        return result
    }

    // Helper for parsing kit layouts from config
    private fun parseKitLayouts(section: ConfigurationSection): Map<String, Map<Int, Int>> {
        val result = mutableMapOf<String, MutableMap<Int, Int>>()

        section.getKeys(false).forEach { kitName ->
            val kitSection = section.getConfigurationSection(kitName) ?: return@forEach
            val layoutMap = mutableMapOf<Int, Int>()

            kitSection.getKeys(false).forEach { slotKey ->
                try {
                    val originalSlot = slotKey.toInt()
                    val newSlot = kitSection.getInt(slotKey)
                    layoutMap[originalSlot] = newSlot
                } catch (e: NumberFormatException) {
                    // Skip invalid entries
                }
            }

            result[kitName] = layoutMap
        }

        return result
    }

    // Helper for array fields
    private fun String.splitToList(): List<String> {
        return if (this.isNotEmpty()) this.split(",").map { it.trim() } else emptyList()
    }
}