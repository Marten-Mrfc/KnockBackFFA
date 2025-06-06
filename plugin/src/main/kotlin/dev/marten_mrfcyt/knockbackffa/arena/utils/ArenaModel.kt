package dev.marten_mrfcyt.knockbackffa.arena.utils

import org.bukkit.Location
import org.bukkit.Material

data class ArenaModel(
    val name: String,
    val spawnRegion: Pair<Location, Location>? = null,
    val spawnpoint: Location,
    val killBlock: Material = Material.VOID_AIR,
    val settings: Map<String, Any> = ArenaSetting.createDefaultSettings()
) {
    fun isInSpawnRegion(location: Location): Boolean {
        if (spawnRegion == null || location.world != spawnRegion.first.world) return false

        val min = Location(
            spawnRegion.first.world,
            minOf(spawnRegion.first.x, spawnRegion.second.x),
            minOf(spawnRegion.first.y, spawnRegion.second.y),
            minOf(spawnRegion.first.z, spawnRegion.second.z)
        )

        val max = Location(
            spawnRegion.first.world,
            maxOf(spawnRegion.first.x, spawnRegion.second.x),
            maxOf(spawnRegion.first.y, spawnRegion.second.y),
            maxOf(spawnRegion.first.z, spawnRegion.second.z)
        )

        return location.x >= min.x && location.x <= max.x &&
                location.y >= min.y && location.y <= max.y &&
                location.z >= min.z && location.z <= max.z
    }
      /**
     * Get a setting value with type safety
     */
    fun <T> getSetting(setting: ArenaSetting<T>?): T {
        if (setting == null) {
            // Return a sensible default for null settings
            @Suppress("UNCHECKED_CAST")
            return false as T
        }
        
        return try {
            val value = settings[setting.key]
            if (value != null) {
                @Suppress("UNCHECKED_CAST")
                value as T
            } else {
                setting.defaultValue
            }
        } catch (e: Exception) {
            // In case of any error, return the setting's default value
            setting.defaultValue
        }
    }
    
    /**
     * Check if a global setting is enabled
     */
    fun isGlobalSettingEnabled(setting: ArenaSetting.Global): Boolean {
        return getSetting(setting)
    }
    
    /**
     * Check if a spawn setting is enabled
     */
    fun isSpawnSettingEnabled(setting: ArenaSetting.Spawn): Boolean {
        return getSetting(setting)
    }
}