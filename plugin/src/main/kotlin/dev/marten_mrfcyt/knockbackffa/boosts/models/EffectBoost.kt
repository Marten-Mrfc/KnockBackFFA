package dev.marten_mrfcyt.knockbackffa.boosts.models

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.plugin.Plugin
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

abstract class EffectBoost(
    id: String,
    enabled: Boolean,
    name: String,
    description: List<String>,
    icon: Material,
    val effectDuration: Duration,
    price: Int,
    plugin: Plugin = KnockBackFFA.instance
) : Boost(id, enabled, name, description, icon, price, plugin), Listener {

    private val activeEffects = ConcurrentHashMap<UUID, Boolean>()
    private val pendingApplications = ConcurrentHashMap<UUID, Boolean>()

    protected open val amplifier: Int = 1

    protected open val showParticles: Boolean = true

    protected open val ambient: Boolean = true

    protected open val applyDelay: Long = 10L // 10 ticks = 0.5 seconds

    protected abstract fun getPotionEffectType(): PotionEffectType

    protected open fun applyCustomEffect(player: Player) {
        // Default implementation - child classes can override for custom behavior
    }

    protected open fun removeCustomEffect(player: Player) {
        // Default implementation - child classes can override for custom behavior
    }

    protected fun applyPotionEffect(player: Player) {
        val effectType = getPotionEffectType()
        player.addPotionEffect(PotionEffect(
            effectType,
            Int.MAX_VALUE,
            amplifier,
            ambient,
            showParticles,
            true
        ))
    }

    protected fun removePotionEffect(player: Player) {
        try {
            player.removePotionEffect(getPotionEffectType())
        } catch (e: Exception) {
            plugin.logger.warning("Failed to remove potion effect from ${player.name}: ${e.message}")
        }
    }

    override fun onBoostApplied(player: Player) {
        pendingApplications[player.uniqueId] = true

        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (player.isOnline && pendingApplications.containsKey(player.uniqueId)) {
                removePotionEffect(player)

                applyPotionEffect(player)

                applyCustomEffect(player)

                activeEffects[player.uniqueId] = true
                pendingApplications.remove(player.uniqueId)
            }
        }, applyDelay)
    }

    override fun onBoostRemoved(player: Player) {
        pendingApplications.remove(player.uniqueId)
        activeEffects.remove(player.uniqueId)

        removePotionEffect(player)

        removeCustomEffect(player)
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (isActive(player.uniqueId)) {
            pendingApplications[player.uniqueId] = true
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (isActive(player.uniqueId)) {
            pendingApplications[player.uniqueId] = true
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                if (player.isOnline) {
                    applyPotionEffect(player)
                    applyCustomEffect(player)
                    pendingApplications.remove(player.uniqueId)
                }
            }, applyDelay)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val playerId = event.player.uniqueId
        pendingApplications.remove(playerId)
        activeEffects.remove(playerId)
    }

    override fun isActive(playerUUID: UUID): Boolean {
        return super.isActive(playerUUID) ||
                activeEffects.containsKey(playerUUID) ||
                pendingApplications.containsKey(playerUUID)
    }

    override fun isTimed(): Boolean = true

    override fun getDuration(): Duration = effectDuration
}