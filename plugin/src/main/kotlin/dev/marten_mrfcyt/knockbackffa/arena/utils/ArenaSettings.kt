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
            val values: List<Global> = listOf(
                AllowDropping,
                AllowPickUp,
                AllowBlockBreaking,
                AllowBlockPlacing,
                AllowDamage,
                AllowCrafting,
                AllowInteraction,
                RegenerateOnKill
            )
            
            /**
             * Get all global settings as a non-nullable list
             */
            fun getAllSettings(): List<Global> = values
            
            /**
             * Find a setting by its key
             */
            fun findByKey(key: String): Global? = values.find { it.key == key }
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
            val values: List<Spawn> = listOf(
                AllowDamage,
                AllowBlockBreaking,
                AllowDropping,
                AllowBuilding,
                AllowPickup,
                AllowInteraction
            )
            
            /**
             * Get all spawn settings as a non-nullable list
             */
            fun getAllSettings(): List<Spawn> = values
            
            /**
             * Find a setting by its key
             */
            fun findByKey(key: String): Spawn? = values.find { it.key == key }
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
                // Add all global settings with their default values
                Global.getAllSettings().forEach { setting ->
                    defaultSettings[setting.key] = setting.defaultValue
                }
                
                // Add all spawn settings with their default values
                Spawn.getAllSettings().forEach { setting ->
                    defaultSettings[setting.key] = setting.defaultValue
                }
            } catch (e: Exception) {
                println("WARNING: Error creating arena default settings: ${e.message}")
                e.printStackTrace()
                
                // Add hardcoded defaults as a fallback
                defaultSettings.putAll(getHardcodedDefaults())
            }
            
            return defaultSettings
        }
        
        /**
         * Hardcoded fallback defaults in case of errors
         */
        private fun getHardcodedDefaults(): Map<String, Any> = mapOf(
            "allowDropping" to false,
            "allowPickUp" to false,
            "allowBlockBreaking" to false,
            "allowBlockPlacing" to true,
            "allowDamage" to true,
            "allowCrafting" to false,
            "allowInteraction" to false,
            "regenerateOnKill" to false,
            "spawnAllowDamage" to false,
            "spawnAllowBlockBreaking" to false,
            "spawnAllowDropping" to false,
            "spawnAllowBuilding" to false,
            "spawnAllowPickup" to false,
            "spawnAllowInteraction" to false
        )
        
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
