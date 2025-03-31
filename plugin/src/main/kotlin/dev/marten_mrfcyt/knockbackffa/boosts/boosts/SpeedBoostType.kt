package dev.marten_mrfcyt.knockbackffa.boosts.boosts

import dev.marten_mrfcyt.knockbackffa.boosts.models.BoostType
import dev.marten_mrfcyt.knockbackffa.boosts.models.ConfigurableProperty
import dev.marten_mrfcyt.knockbackffa.boosts.models.EffectBoost
import org.bukkit.Material
import org.bukkit.potion.PotionEffectType
import java.time.Duration

@BoostType("speed_boost")
object SpeedBoostType : EffectBoost(
    id = "speed_boost",
    enabled = true,
    name = "Speed Boost",
    description = listOf("Move faster around the arena", "Gain a significant speed advantage"),
    icon = Material.RABBIT_FOOT,
    effectDuration = Duration.ofMinutes(20),
    price = 30
) {
    @ConfigurableProperty(configKey = "speedAmplifier", defaultValue = "1")
    private var speedAmplifier = 1

    @ConfigurableProperty(configKey = "particles", defaultValue = "true")
    private var particles = false

    @ConfigurableProperty(configKey = "ambient", defaultValue = "true")
    private var ambientValue = true

    override val amplifier: Int
        get() = speedAmplifier

    override val showParticles: Boolean
        get() = particles

    override val ambient: Boolean
        get() = ambientValue

    override fun getPotionEffectType(): PotionEffectType {
        return PotionEffectType.SPEED
    }
}