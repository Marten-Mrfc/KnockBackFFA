package dev.marten_mrfcyt.knockbackffa.arena.utils

import org.bukkit.Material

/**
 * Represents a setting for an arena with type safety and metadata
 */
sealed class ArenaSetting<T>(
    val key: String,
    val defaultValue: T,
    val displayName: String,
    val icon: Material,
    val description: String
) {
    /**
     * Global arena settings that apply to the entire arena
     */
    sealed class Global(
        key: String,
        defaultValue: Boolean,
        displayName: String,
        icon: Material,
        description: String
    ) : ArenaSetting<Boolean>(key, defaultValue, displayName, icon, description) {
        object AllowDropping : Global(
            "allowDropping", 
            false, 
            "Allow Item Dropping",
            Material.CHEST,
            "Players can drop items in the arena"
        )
        
        object AllowPickUp : Global(
            "allowPickUp", 
            false, 
            "Allow Item Pickup",
            Material.HOPPER,
            "Players can pick up items in the arena"
        )
        
        object AllowBlockBreaking : Global(
            "allowBlockBreaking", 
            false, 
            "Allow Block Breaking",
            Material.STONE_PICKAXE,
            "Players can break blocks in the arena"
        )
        
        object AllowBlockPlacing : Global(
            "allowBlockPlacing", 
            true,
            "Allow Block Placing",
            Material.BRICKS,
            "Players can place blocks in the arena"
        )
        
        object AllowDamage : Global(
            "allowDamage", 
            true, 
            "Allow Damage",
            Material.IRON_SWORD,
            "Players can take damage in the arena"
        )
        
        object AllowCrafting : Global(
            "allowCrafting", 
            false, 
            "Allow Crafting",
            Material.CRAFTING_TABLE,
            "Players can craft items in the arena"
        )
        
        object AllowInteraction : Global(
            "allowInteraction", 
            false, 
            "Allow Block Interaction",
            Material.LEVER,
            "Players can interact with blocks in the arena"
        )

        object RegenerateOnKill : Global(
            "regenerateOnKill",
            false,
            "Regenerate on kill",
            Material.DIAMOND_SWORD,
            "Players health is restored on kill otherwise always full"
        )

        companion object {
            val values = listOf(
                AllowDropping,
                AllowPickUp,
                AllowBlockBreaking,
                AllowBlockPlacing,
                AllowDamage,
                AllowCrafting,
                AllowInteraction,
                RegenerateOnKill
            )
        }
    }
    
    /**
     * Spawn region settings that only apply within the spawn area
     */
    sealed class Spawn(
        key: String,
        defaultValue: Boolean,
        displayName: String,
        icon: Material,
        description: String
    ) : ArenaSetting<Boolean>(key, defaultValue, displayName, icon, description) {
        object AllowDamage : Spawn(
            "spawnAllowDamage", 
            false, 
            "Allow Damage in Spawn",
            Material.GOLDEN_APPLE,
            "Players can take damage while in spawn"
        )
        
        object AllowBlockBreaking : Spawn(
            "spawnAllowBlockBreaking", 
            false, 
            "Allow Block Breaking in Spawn",
            Material.STONE_PICKAXE,
            "Players can break blocks while in spawn"
        )
        
        object AllowDropping : Spawn(
            "spawnAllowDropping", 
            false, 
            "Allow Item Dropping in Spawn",
            Material.CHEST,
            "Players can drop items while in spawn"
        )
        
        object AllowBuilding : Spawn(
            "spawnAllowBuilding", 
            false, 
            "Allow Building in Spawn",
            Material.BRICKS,
            "Players can place blocks while in spawn"
        )
        
        object AllowPickup : Spawn(
            "spawnAllowPickup", 
            false, 
            "Allow Item Pickup in Spawn",
            Material.HOPPER,
            "Players can pick up items while in spawn"
        )
        
        object AllowInteraction : Spawn(
            "spawnAllowInteraction", 
            false, 
            "Allow Block Interaction in Spawn",
            Material.LEVER,
            "Players can interact with blocks while in spawn"
        )

        companion object {
            val values = listOf(
                AllowDamage,
                AllowBlockBreaking,
                AllowDropping,
                AllowBuilding,
                AllowPickup,
                AllowInteraction
            )
        }
    }
    
    companion object {
        /**
         * Initialize default settings for a new arena
         * @return Map of settings with their default values
         */
        fun createDefaultSettings(): Map<String, Any> {
            val defaultSettings = mutableMapOf<String, Any>()
            
            try {
                // Safety check for Global values
                for (setting in Global.values) {
                    defaultSettings[setting.key] = setting.defaultValue
                }
                
                // Safety check for Spawn values
                for (setting in Spawn.values) {
                    defaultSettings[setting.key] = setting.defaultValue
                }
            } catch (e: Exception) {
                println("WARNING: Error creating arena default settings: ${e.message}")
                e.printStackTrace()
            }
            
            // Add some hardcoded defaults as a fallback
            if (defaultSettings.isEmpty()) {
                defaultSettings["allowDropping"] = false
                defaultSettings["allowPickUp"] = false
                defaultSettings["allowBlockBreaking"] = false
                defaultSettings["allowBlockPlacing"] = true
                defaultSettings["allowDamage"] = true
                defaultSettings["allowCrafting"] = false
                defaultSettings["allowInteraction"] = false
                defaultSettings["regenerateOnKill"] = false
                
                defaultSettings["spawnAllowDamage"] = false
                defaultSettings["spawnAllowBlockBreaking"] = false
                defaultSettings["spawnAllowDropping"] = false
                defaultSettings["spawnAllowBuilding"] = false
                defaultSettings["spawnAllowPickup"] = false
                defaultSettings["spawnAllowInteraction"] = false
            }
            
            return defaultSettings
        }
        
        /**
         * Get a setting from the arena settings map with type safety
         */
        inline fun <reified T : ArenaSetting<V>, V> getValue(
            settings: Map<String, Any>, 
            setting: T?
        ): V {
            return if (setting == null) {
                // Default fallback if setting is null
                false as V
            } else {
                settings[setting.key] as? V ?: setting.defaultValue
            }
        }
    }
}
