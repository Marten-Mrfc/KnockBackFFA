// src/main/kotlin/dev/marten_mrfcyt/knockbackffa/kits/managers/KitManager.kt
package dev.marten_mrfcyt.knockbackffa.kits.managers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.models.Kit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.logging.Level

class KitManager(private val plugin: KnockBackFFA) {
    private val configFile: File = File(plugin.dataFolder, "kits.yml")
    internal val cachedKits = mutableMapOf<String, Kit>()

    init {
        if (!configFile.exists()) {
            plugin.saveResource("kits.yml", false)
        }
        loadAllKits()
    }

    private fun loadAllKits() {
        val kitConfig = YamlConfiguration.loadConfiguration(configFile)
        val kitSection = kitConfig.getConfigurationSection("kit") ?: return

        kitSection.getKeys(false).forEach { kitName ->
            try {
                val kit = Kit.load(kitName)
                if (kit != null) {
                    cachedKits[kitName] = kit
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "Failed to load kit: $kitName", e)
            }
        }
    }

    fun getKit(kitName: String): Kit{
        return cachedKits[kitName] ?: try {
            val kit = Kit.load(kitName)
            if (kit != null) {
                cachedKits[kitName] = kit
            }
            kit
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to load kit: $kitName", e)
            null
        } ?: throw IllegalArgumentException("Kit not found: $kitName")
    }

    fun getAllKitNames(): List<String> {
        val kitConfig = YamlConfiguration.loadConfiguration(configFile)
        return kitConfig.getConfigurationSection("kit")?.getKeys(false)?.toList() ?: emptyList()
    }

    fun applyKit(player: Player, kitName: String) {
        getKit(kitName)?.applyTo(player)
    }

    fun createKit(kitName: String, displayName: String, description: String): Kit {
        val kit = Kit(
            name = kitName,
            displayName = displayName,
            description = description
        )

        // Save the kit
        kit.save()

        // Update the cache
        cachedKits[kitName] = kit

        return kit
    }

    fun deleteKit(kitName: String) {
        val kitConfig = YamlConfiguration.loadConfiguration(configFile)
        kitConfig.set("kit.$kitName", null)
        kitConfig.save(configFile)

        // Remove from cache
        cachedKits.remove(kitName)
    }

    // Method to update the cached kit (used internally)
    internal fun updateKitCache(kitName: String, kit: Kit) {
        cachedKits[kitName] = kit
    }

    // Add to KitManager.kt
    fun reloadKits() {
        cachedKits.clear()
        if (!configFile.exists()) {
            plugin.saveResource("kits.yml", false)
        }
        loadAllKits()
        plugin.logger.info("Reloaded ${cachedKits.size} kits")
    }
}