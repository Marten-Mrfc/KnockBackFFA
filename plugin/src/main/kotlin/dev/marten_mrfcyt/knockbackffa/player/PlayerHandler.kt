package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.bypassMode
import dev.marten_mrfcyt.knockbackffa.arena.currentArena
import dev.marten_mrfcyt.knockbackffa.arena.utils.ArenaSetting
import mlib.api.utilities.debug
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerPickupArrowEvent
import org.bukkit.inventory.EquipmentSlot
import kotlin.compareTo
import kotlin.to

class PlayerHandler(private val plugin: KnockBackFFA) : Listener {
    private fun isBypassing(player: Player): Boolean {
        val bypassing = bypassMode.getOrDefault(player, false)
        if (bypassing) {
            debug(plugin, "Player ${player.name} is bypassing restrictions")
        }
        return bypassing
    }
    
    private fun isInSpawnRegion(player: Player): Boolean {
        val current = currentArena ?: return false
        return current.isInSpawnRegion(player.location)
    }
    
    /**
     * Handler for item dropping
     * Priority is LOW to let the SpawnRegionHandler override if needed
     */
    @EventHandler(priority = EventPriority.LOW)
    fun allowDropping(event: PlayerDropItemEvent) {
        val current = currentArena ?: return
        
        // If in spawn region, let the SpawnRegionHandler handle it with higher priority
        if (isInSpawnRegion(event.player)) return
        
        // Use type-safe setting access with null safety
        val allowDropping = try {
            current.getSetting(ArenaSetting.Global.AllowDropping)
        } catch (e: Exception) {
            debug(plugin, "Error getting AllowDropping setting: ${e.message}")
            false // Default to false on error
        }
        
        if (!allowDropping && !isBypassing(event.player)) {
            debug(plugin, "Prevented ${event.player.name} from dropping ${event.itemDrop.itemStack.type}")
            event.isCancelled = true
        }
    }
    
    /**
     * Handler for item pickup
     * Priority is LOW to let the SpawnRegionHandler override if needed
     */
    @EventHandler(priority = EventPriority.LOW)
    fun allowPickUp(event: EntityPickupItemEvent) {
        if (event.entity !is Player) return
        
        val player = event.entity as Player
        val current = currentArena ?: return
        
        // If in spawn region, let the SpawnRegionHandler handle it with higher priority
        if (isInSpawnRegion(player)) return
        
        // Use type-safe setting access
        val allowPickUp = current.getSetting(ArenaSetting.Global.AllowPickUp)
        
        if (!allowPickUp && !isBypassing(player)) {
            debug(plugin, "Prevented ${player.name} from picking up ${event.item.itemStack.type}")
            event.isCancelled = true
        }
    }
    @EventHandler(priority = EventPriority.HIGH)
    fun handleArrowPickUp(event: PlayerPickupArrowEvent) {
        val current = currentArena ?: return
        val player = event.player
        if (isInSpawnRegion(player)) return

        val allowPickup = current.getSetting(ArenaSetting.Global.AllowPickUp)

        if (!allowPickup && !isBypassing(player)) {
            debug(plugin, "Prevented ${player.name} from picking up ${event.item.itemStack.type} in spawn region")
            event.isCancelled = true
        }
    }
    /**
     * Handler for block breaking
     * Priority is LOW to let the SpawnRegionHandler override if needed
     */
    @EventHandler(priority = EventPriority.LOW)
    fun allowBlockBreaking(event: BlockBreakEvent) {
        val current = currentArena ?: return
        
        // If block is in spawn region, let the SpawnRegionHandler handle it with higher priority
        if (current.isInSpawnRegion(event.block.location)) return
        
        // Use type-safe setting access
        val allowBlockBreaking = current.getSetting(ArenaSetting.Global.AllowBlockBreaking)
        
        if (!allowBlockBreaking && !isBypassing(event.player)) {
            debug(plugin, "Prevented ${event.player.name} from breaking ${event.block.type} at ${event.block.location}")
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerHungerDrain(event: PlayerMoveEvent) {
        if (event.from.blockX == event.to.blockX &&
            event.from.blockY == event.to.blockY &&
            event.from.blockZ == event.to.blockZ
        ) return
        val player = event.player
        val current = currentArena ?: return
        val regenerateOnKill = current.getSetting(ArenaSetting.Global.RegenerateOnKill)
        if (!regenerateOnKill) {
            player.health = 20.0
            player.foodLevel = 20
            player.saturation = 20f
            debug(plugin, "Reset ${player.name}'s health to 20, move")
        }
    }
    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerKillOtherPlayer(event: PlayerDeathEvent) {
        val player = event.player.killer ?: return
        val current = currentArena ?: return
        val regenerateOnKill = current.getSetting(ArenaSetting.Global.RegenerateOnKill)
        if (regenerateOnKill) {
            player.health = 20.0
            player.foodLevel = 20
            player.saturation = 20f
            debug(plugin, "Reset ${player.name}'s health to 20, kill")
        }
    }

    /**
     * Handler for block placing
     * Priority is LOW to let the SpawnRegionHandler override if needed
     */
    @EventHandler(priority = EventPriority.LOW)
    fun allowBlockPlacing(event: BlockPlaceEvent) {
        val current = currentArena ?: return
        
        // If block is in spawn region, let the SpawnRegionHandler handle it with higher priority
        if (current.isInSpawnRegion(event.block.location)) return
        
        // Use type-safe setting access
        val allowBlockPlacing = current.getSetting(ArenaSetting.Global.AllowBlockPlacing)
        
        if (!allowBlockPlacing && !isBypassing(event.player)) {
            debug(plugin, "Prevented ${event.player.name} from placing ${event.block.type} at ${event.block.location}")
            event.isCancelled = true
        }
    }
    
    /**
     * Handler for player damage
     * Priority is LOW to let the SpawnRegionHandler override if needed
     */
    @EventHandler(priority = EventPriority.LOW)
    fun allowDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        
        val player = event.entity as Player
        val current = currentArena ?: return
        
        // If player is in spawn region, let the SpawnRegionHandler handle it with higher priority
        if (isInSpawnRegion(player)) return
        
        // Use type-safe setting access
        val allowDamage = current.getSetting(ArenaSetting.Global.AllowDamage)
        
        if (!allowDamage && !isBypassing(player)) {
            event.damage = 0.0
            debug(plugin, "Prevented damage to ${player.name} from ${event.cause.name}, amount: ${event.damage}")
            event.isCancelled = true
        }
    }
    
    /**
     * Handler for crafting
     */
    @EventHandler
    fun allowCrafting(event: CraftItemEvent) {
        if (event.whoClicked !is Player) return
        
        val player = event.whoClicked as Player
        val current = currentArena ?: return
        
        // Use type-safe setting access
        val allowCrafting = current.getSetting(ArenaSetting.Global.AllowCrafting)
        
        if (!allowCrafting && !isBypassing(player)) {
            debug(plugin, "Prevented ${player.name} from crafting ${event.recipe.result.type}")
            event.isCancelled = true
        }
    }
    
    /**
     * Handler for block interaction
     */
    @EventHandler
    fun allowInteraction(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.clickedBlock == null) return
        if (!event.clickedBlock!!.type.isInteractable) return
        val current = currentArena ?: return
        
        // If block is in spawn region, check spawn-specific setting
        if (current.isInSpawnRegion(event.clickedBlock!!.location)) {
            val spawnAllowInteraction = current.getSetting(ArenaSetting.Spawn.AllowInteraction)
            if (!spawnAllowInteraction && !isBypassing(event.player)) {
                debug(plugin, "Prevented ${event.player.name} from interacting with ${event.clickedBlock!!.type} in spawn")
                event.isCancelled = true
            }
            return
        }
        
        // Use type-safe setting access for global setting
        val allowInteraction = current.getSetting(ArenaSetting.Global.AllowInteraction)
        
        if (!allowInteraction && !isBypassing(event.player)) {
            debug(plugin, "Prevented ${event.player.name} from interacting with ${event.clickedBlock!!.type}")
            event.isCancelled = true
        }
    }
}