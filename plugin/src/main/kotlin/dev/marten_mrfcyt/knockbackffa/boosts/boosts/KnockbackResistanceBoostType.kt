package dev.marten_mrfcyt.knockbackffa.boosts.boosts

import dev.marten_mrfcyt.knockbackffa.boosts.models.BoostType
import dev.marten_mrfcyt.knockbackffa.boosts.models.ConfigurableProperty
import dev.marten_mrfcyt.knockbackffa.boosts.models.EffectBoost
import mlib.api.utilities.message
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@BoostType("knockback_resistance")
object KnockbackResistanceBoostType : EffectBoost(
    id = "knockback_resistance",
    enabled = true,
    name = "Knockback Resistance",
    description = listOf("Resist knockback from attacks", "Stay grounded during fights"),
    icon = Material.NETHERITE_BOOTS,
    effectDuration = Duration.ofMinutes(10),
    price = 25
), Listener {

    @ConfigurableProperty(configKey = "resistancePercentage", defaultValue = "70")
    private var resistancePercentage = 50

    @ConfigurableProperty(configKey = "particles", defaultValue = "true")
    private var particles = false

    @ConfigurableProperty(configKey = "ambient", defaultValue = "true")
    private var ambientValue = true

    @ConfigurableProperty(configKey = "glowEffect", defaultValue = "true")
    private var glowEffect = true

    // Store players who have the boost active
    private val activeBoosts = ConcurrentHashMap<UUID, Boolean>()

    // These values aren't used by our custom effect, but are required by the EffectBoost class
    override val amplifier: Int = 0
    override val showParticles: Boolean get() = particles
    override val ambient: Boolean get() = ambientValue

    override fun getPotionEffectType(): PotionEffectType {
        // We're using DAMAGE_RESISTANCE just for visual effect, not for actual resistance mechanics
        return PotionEffectType.GLOWING
    }

    override fun apply(player: Player): Boolean {
        // Add to our tracking map
        activeBoosts[player.uniqueId] = true
        player.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.baseValue = resistancePercentage / 100.0
            if (glowEffect) {
                player.addPotionEffect(PotionEffect(
                    PotionEffectType.GLOWING,
                    Int.MAX_VALUE,
                    0,
                    ambient,
                    false,
                    true
                ))
        }

        player.message("<green>You now have Knockback Resistance!")
        return true
    }

    override fun removeCustomEffect(player: Player) {
        // Remove from our tracking map
        activeBoosts.remove(player.uniqueId)

        // Remove visual effects
        player.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.baseValue = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.defaultValue ?: 0.0
        if (glowEffect) {
            player.removePotionEffect(PotionEffectType.GLOWING)
        }

        player.message("<red>Your Knockback Resistance has expired!")
    }
}