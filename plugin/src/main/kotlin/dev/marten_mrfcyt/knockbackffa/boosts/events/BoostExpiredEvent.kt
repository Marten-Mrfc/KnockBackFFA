package dev.marten_mrfcyt.knockbackffa.boosts.events

import dev.marten_mrfcyt.knockbackffa.boosts.models.Boost
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class BoostExpiredEvent(
    val player: Player,
    val boost: Boost
) : Event() {
    companion object {
        private val HANDLERS = HandlerList()
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }
}