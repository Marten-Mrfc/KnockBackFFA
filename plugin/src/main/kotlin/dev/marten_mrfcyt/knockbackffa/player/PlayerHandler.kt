package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.bypassMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.player.PlayerDropItemEvent

class PlayerHandler(plugin: KnockBackFFA) : Listener {
    val config = plugin.config

    private fun isBypassing(player: Player): Boolean {
        return bypassMode.getOrDefault(player, false)
    }

    // Item Dropping
    @EventHandler
    fun allowDropping(event: PlayerDropItemEvent) {
        if (config.get("currentArena") == null) return
        if (config.get("allowDropping") == false && !isBypassing(event.player)) {
            event.isCancelled = true
        }
    }

    // Item PickUp
    @EventHandler
    fun allowPickUp(event: EntityPickupItemEvent) {
        if (config.get("currentArena") == null) return
        if (config.get("allowPickUp") == false && event.entity is Player && !isBypassing(event.entity as Player)) {
            event.isCancelled = true
        }
    }

    // Block Breaking
    @EventHandler
    fun allowBlockBreaking(event: BlockBreakEvent) {
        if (config.get("currentArena") == null) return
        if (config.get("allowBlockBreaking") == false && !isBypassing(event.player)) {
            event.isCancelled = true
        }
    }

    // Damage
    @EventHandler
    fun allowDamage(event: EntityDamageEvent) {
        if (config.get("currentArena") == null) return
        if (config.get("allowDamage") == false && event.entity is Player && !isBypassing(event.entity as Player)) {
            event.damage = 0.0
        }
    }

    // Crafting
    @EventHandler
    fun allowCrafting(event: CraftItemEvent) {
        if (config.get("currentArena") == null) return
        if (config.get("allowCrafting") == false && event.whoClicked is Player && !isBypassing(event.whoClicked as Player)) {
            event.isCancelled = true
        }
    }
}