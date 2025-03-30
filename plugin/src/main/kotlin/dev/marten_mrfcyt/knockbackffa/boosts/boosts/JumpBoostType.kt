package dev.marten_mrfcyt.knockbackffa.boosts.boosts

import dev.marten_mrfcyt.knockbackffa.boosts.models.BoostType
import dev.marten_mrfcyt.knockbackffa.boosts.models.ConfigurableProperty
import dev.marten_mrfcyt.knockbackffa.boosts.models.EffectBoost
import org.bukkit.Material
import org.bukkit.potion.PotionEffectType
import java.time.Duration

@BoostType("jump_boost")
object JumpBoostType : EffectBoost(
    id = "jump_boost",
    enabled = true,
    name = "Jump Boost",
    description = listOf("Jump higher than normal", "Perfect for reaching platforms"),
    icon = Material.SLIME_BLOCK,
    effectDuration = Duration.ofMinutes(15),
    price = 20
) {
    @ConfigurableProperty(configKey = "jumpAmplifier", defaultValue = "1")
    private var jumpAmplifier = 1

    @ConfigurableProperty(configKey = "particles", defaultValue = "true")
    private var particles = false

    @ConfigurableProperty(configKey = "ambient", defaultValue = "true")
    private var ambientValue = true
    override val amplifier: Int
        get() = jumpAmplifier

    override val showParticles: Boolean
        get() = particles

    override val ambient: Boolean
        get() = ambientValue

    override fun getPotionEffectType(): PotionEffectType {
        return PotionEffectType.JUMP_BOOST
    }
}