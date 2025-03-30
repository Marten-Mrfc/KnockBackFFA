package dev.marten_mrfcyt.knockbackffa.boosts.models

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.time.Duration
import java.util.UUID

abstract class Boost(
    val id: String,
    val enabled: Boolean,
    val name: String,
    val description: List<String>,
    val icon: Material,
    val price: Int,
    protected val plugin: Plugin = KnockBackFFA.instance
) {
    open fun apply(player: Player): Boolean {
        val manager = KnockBackFFA.instance.playerBoostManager
        val success = if (isTimed() && getDuration() != null) {
            manager.addBoost(player.uniqueId, id, getDuration()!!, skipApply = true)
            true
        } else {
            false
        }

        if (success) {
            onBoostApplied(player)
        }

        return success
    }
    open fun remove(player: Player) {
        onBoostRemoved(player)
    }

    protected open fun onBoostApplied(player: Player) {
    }

    open fun onBoostRemoved(player: Player) {
    }

    abstract fun isTimed(): Boolean

    abstract fun getDuration(): Duration?

    open fun isActive(playerUUID: UUID): Boolean {
        return KnockBackFFA.instance.playerBoostManager.hasActiveBoost(playerUUID, id)
    }
}