package dev.marten_mrfcyt.knockbackffa.handlers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import io.papermc.paper.event.player.PlayerPickItemEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerDropItemEvent

class PlayerHandler(plugin: KnockBackFFA): Listener {
    val config = plugin.config
    @EventHandler
    fun allowDropping(event: PlayerDropItemEvent) {
        if (config.get("currentArena") == null) return
        if (config.get("allowDropping") == true) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun allowPickUp(event: EntityPickupItemEvent) {
        if (config.get("currentArena") == null) return
        if (config.get("allowPickUp") == true && event.entity is org.bukkit.entity.Player) {
            event.isCancelled = true
        }
    }
}