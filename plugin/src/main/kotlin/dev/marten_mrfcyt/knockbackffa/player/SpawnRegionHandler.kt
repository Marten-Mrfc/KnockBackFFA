package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.arena.currentArena
import dev.marten_mrfcyt.knockbackffa.arena.utils.ArenaSetting
import dev.marten_mrfcyt.knockbackffa.bypassMode
import mlib.api.utilities.debug
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerPickupArrowEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * Handles interactions specifically within the spawn region
 * Uses HIGH priority to override the standard PlayerHandler
 */
class SpawnRegionHandler(private val plugin: KnockBackFFA) : Listener {
    
    private fun isBypassing(player: Player): Boolean {
        val bypassing = bypassMode.getOrDefault(player, false)
        if (bypassing) {
            debug(plugin, "Player ${player.name} is bypassing spawn restrictions")
        }
        return bypassing
    }
    
    private fun isInSpawnRegion(player: Player): Boolean {
        val current = currentArena ?: return false
        return current.isInSpawnRegion(player.location)
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    fun handleDamageInSpawn(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        
        val player = event.entity as Player
        val current = currentArena ?: return
        
        if (current.isInSpawnRegion(player.location)) {
            // Get setting with null safety
            val allowDamage = try {
                current.getSetting(ArenaSetting.Spawn.AllowDamage)
            } catch (e: Exception) {
                debug(plugin, "Error getting Spawn.AllowDamage setting: ${e.message}")
                false // Default to false on error
            }
            
            if (!allowDamage && !isBypassing(player)) {
                debug(plugin, "Prevented damage to ${player.name} in spawn region")
                event.isCancelled = true
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    fun handleBlockBreakingInSpawn(event: BlockBreakEvent) {
        val current = currentArena ?: return
        
        if (current.isInSpawnRegion(event.block.location)) {
            val allowBlockBreaking = current.getSetting(ArenaSetting.Spawn.AllowBlockBreaking)
            
            if (!allowBlockBreaking && !isBypassing(event.player)) {
                debug(plugin, "Prevented ${event.player.name} from breaking ${event.block.type} in spawn region")
                event.isCancelled = true
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    fun handleBlockPlacingInSpawn(event: BlockPlaceEvent) {
        val current = currentArena ?: return
        
        if (current.isInSpawnRegion(event.block.location)) {
            val allowBuilding = current.getSetting(ArenaSetting.Spawn.AllowBuilding)
            
            if (!allowBuilding && !isBypassing(event.player)) {
                debug(plugin, "Prevented ${event.player.name} from placing ${event.block.type} in spawn region")
                event.isCancelled = true
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    fun handleItemDroppingInSpawn(event: PlayerDropItemEvent) {
        val current = currentArena ?: return
        
        if (current.isInSpawnRegion(event.player.location)) {
            val allowDropping = current.getSetting(ArenaSetting.Spawn.AllowDropping)
            
            if (!allowDropping && !isBypassing(event.player)) {
                debug(plugin, "Prevented ${event.player.name} from dropping ${event.itemDrop.itemStack.type} in spawn region")
                event.isCancelled = true
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    fun handleItemPickupInSpawn(event: EntityPickupItemEvent) {
        if (event.entity !is Player) return
        
        val player = event.entity as Player
        val current = currentArena ?: return
        
        if (current.isInSpawnRegion(player.location)) {
            val allowPickup = current.getSetting(ArenaSetting.Spawn.AllowPickup)
            
            if (!allowPickup && !isBypassing(player)) {
                debug(plugin, "Prevented ${player.name} from picking up ${event.item.itemStack.type} in spawn region")
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun handleArrowPickUpInSpawn(event: PlayerPickupArrowEvent) {
        val current = currentArena ?: return
        val player = event.player

        if (current.isInSpawnRegion(player.location)) {
            val allowPickup = current.getSetting(ArenaSetting.Spawn.AllowPickup)

            if (!allowPickup && !isBypassing(player)) {
                debug(plugin, "Prevented ${player.name} from picking up ${event.item.itemStack.type} in spawn region")
                event.isCancelled = true
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    fun handleInteractionInSpawn(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.clickedBlock == null) return
        
        val current = currentArena ?: return
        
        if (current.isInSpawnRegion(event.clickedBlock!!.location)) {
            val allowInteraction = current.getSetting(ArenaSetting.Spawn.AllowInteraction)
            
            if (!allowInteraction && !isBypassing(event.player)) {
                debug(plugin, "Prevented ${event.player.name} from interacting with ${event.clickedBlock!!.type} in spawn region")
                event.isCancelled = true
            }
        }
    }
}
