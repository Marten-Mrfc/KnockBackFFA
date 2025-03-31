package dev.marten_mrfcyt.knockbackffa.boosts.managers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.boosts.models.Boost
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class BoostConfigHandler(private val plugin: KnockBackFFA) {
    private val boostConfigFile = File(plugin.dataFolder, "boosts.yml")
    private lateinit var boostConfig: YamlConfiguration
    private val propertyApplier = BoostPropertyApplier(plugin)

    fun loadConfig() {
        if (!boostConfigFile.exists()) {
            plugin.saveResource("boosts.yml", false)
        }
        boostConfig = YamlConfiguration.loadConfiguration(boostConfigFile)
    }

    fun updateBoostConfig(boost: Boost, key: String, value: Any) {
        val configPath = "boosts.${boost.id}.$key"

        boostConfig.set(configPath, value)
        try {
            boostConfig.save(boostConfigFile)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save boost configuration: ${e.message}")
        }
    }

    fun applyConfigToBoost(boost: Boost) {
        val configSection = boostConfig.getConfigurationSection("boosts.${boost.id}")
        if (configSection != null) {
            propertyApplier.applyConfigValues(boost, configSection)
        }
    }
}