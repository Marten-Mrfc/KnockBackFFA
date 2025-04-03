package dev.marten_mrfcyt.knockbackffa.boosts.models

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import mlib.api.utilities.message
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.time.Duration

// SOON
abstract class StatsBoost(
    id: String,
    enabled: Boolean,
    name: String,
    description: List<String>,
    icon: Material,
    val statsDuration: Duration,
    price: Int,
    plugin: Plugin = KnockBackFFA.instance
) : Boost(id, enabled, name, description, icon, price, plugin) {

    override fun isTimed(): Boolean = true

    override fun getDuration(): Duration = statsDuration

    abstract fun getMultiplier(): Double

    override fun onBoostApplied(player: Player) {
        player.message("<green>Your $name multiplier (${getMultiplier()}x) has been activated for ${formatDuration(statsDuration)}!")
    }

    override fun onBoostRemoved(player: Player) {
        player.message("<red>Your $name multiplier has expired!")
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}