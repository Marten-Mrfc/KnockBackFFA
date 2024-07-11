package dev.marten_mrfcyt.knockbackffa.handlers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import io.papermc.paper.event.player.PlayerPickItemEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerDropItemEvent

class PlayerHandler(plugin: KnockBackFFA): Listener {
    val config = plugin.config
    // Item Dropping
    @EventHandler
    fun allowDropping(event: PlayerDropItemEvent) {
        if (config.get("currentArena") == null) return
        if (config.get("allowDropping") == false) {
            event.isCancelled = true
        }
    }
    //Item PickUp
    @EventHandler
    fun allowPickUp(event: EntityPickupItemEvent) {
        if (config.get("currentArena") == null) return
        if (config.get("allowPickUp") == false && event.entity is org.bukkit.entity.Player) {
            event.isCancelled = true
        }
    }
    // BlockBreaking
    @EventHandler
    fun allowBlockBreaking(event: BlockBreakEvent) {
        if (config.get("currentArena") == null) return
        if (config.get("allowBlockBreaking") == false) {
            event.isCancelled = true
        }
    }
    // Damage
    @EventHandler
    fun allowDamage(event: EntityDamageEvent) {
        if (config.get("currentArena") == null) return
        if (config.get("allowDamage") == false) {
            event.isCancelled = true
        }
    }
}