package dev.marten_mrfcyt.knockbackffa.boosts.boosts

import dev.marten_mrfcyt.knockbackffa.boosts.models.BoostType
import dev.marten_mrfcyt.knockbackffa.boosts.models.ConfigurableProperty
import dev.marten_mrfcyt.knockbackffa.boosts.models.ItemBoost
import mlib.api.utilities.message
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@BoostType("smoke_screen")
object SmokeScreenBoostType : ItemBoost(
    id = "smoke_screen",
    enabled = true,
    name = "Smoke Screen",
    description = listOf("Creates a dense smoke cloud around you", "Blinds nearby enemies"),
    icon = Material.CAMPFIRE,
    price = 3
), Listener {

    @ConfigurableProperty(configKey = "smokeRadius", defaultValue = "5.0")
    private var smokeRadius = 5.0

    @ConfigurableProperty(configKey = "smokeDuration", defaultValue = "5")
    private var smokeDuration = 5

    @ConfigurableProperty(configKey = "blindnessDuration", defaultValue = "100")
    private var blindnessDuration = 100

    @ConfigurableProperty(configKey = "cooldown", defaultValue = "30")
    private var cooldown = 30

    @ConfigurableProperty(configKey = "particleCount", defaultValue = "350")
    private var particleCount = 350

    @ConfigurableProperty(configKey = "particleType", defaultValue = "CAMPFIRE_COSY_SMOKE")
    private var particleType = Particle.CAMPFIRE_COSY_SMOKE

    @ConfigurableProperty(configKey = "sound", defaultValue = "ENTITY_DRAGON_FIREBALL_EXPLODE")
    private var soundEffect = Sound.ENTITY_DRAGON_FIREBALL_EXPLODE

    @ConfigurableProperty(configKey = "soundVolume", defaultValue = "1.0")
    private var soundVolume = 1.0f

    @ConfigurableProperty(configKey = "soundPitch", defaultValue = "1.0")
    private var soundPitch = 1.0f

    @ConfigurableProperty(configKey = "blindnessAmplifier", defaultValue = "0")
    private var blindnessAmplifier = 0

    @ConfigurableProperty(configKey = "maxCloudHeight", defaultValue = "2.0")
    private var maxCloudHeight = 2.0

    @ConfigurableProperty(configKey = "particleVelocityMultiplier", defaultValue = "0.05")
    private var particleVelocityMultiplier = 0.05

    @ConfigurableProperty(configKey = "particleYVelocityMultiplier", defaultValue = "0.1")
    private var particleYVelocityMultiplier = 0.1

    private val cooldowns = ConcurrentHashMap<UUID, Long>()

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return

        if (!isBoostItem(item)) return

        if (!event.action.isRightClick) return

        val lastUse = cooldowns[player.uniqueId] ?: 0L
        val currentTime = System.currentTimeMillis() / 1000

        if (currentTime - lastUse < cooldown) {
            val remaining = cooldown - (currentTime - lastUse)
            player.message("<red>Smoke Screen is on cooldown! ($remaining seconds left)")
            return
        }

        event.isCancelled = true

        createSmokeCloud(player)

        player.getNearbyEntities(smokeRadius, smokeRadius, smokeRadius).forEach { entity ->
            if (entity is Player && entity != player) {
                entity.addPotionEffect(PotionEffect(
                    PotionEffectType.BLINDNESS,
                    blindnessDuration,
                    blindnessAmplifier,
                    false,
                    false,
                    true
                ))
                entity.message("<red>You've been blinded by a smoke screen!")
            }
        }

        cooldowns[player.uniqueId] = currentTime

        if (item.amount > 1) {
            item.amount -= 1
        } else {
            player.inventory.setItemInMainHand(null)
        }
    }

    private fun createSmokeCloud(player: Player) {
        val location = player.location

        player.world.playSound(location, soundEffect, soundVolume, soundPitch)

        object : BukkitRunnable() {
            private var secondsElapsed = 0

            override fun run() {
                if (secondsElapsed >= smokeDuration) {
                    cancel()
                    return
                }

                for (i in 0 until particleCount) {
                    val offsetX = (Math.random() * 2 - 1) * smokeRadius
                    val offsetY = Math.random() * maxCloudHeight
                    val offsetZ = (Math.random() * 2 - 1) * smokeRadius

                    val x = location.x + offsetX
                    val y = location.y + offsetY
                    val z = location.z + offsetZ

                    val velocityX = (Math.random() - 0.5) * particleVelocityMultiplier
                    val velocityY = Math.random() * particleYVelocityMultiplier
                    val velocityZ = (Math.random() - 0.5) * particleVelocityMultiplier

                    location.world.spawnParticle(
                        particleType,
                        x, y, z,
                        0,
                        velocityX, velocityY, velocityZ,
                        0.1
                    )
                }

                secondsElapsed++
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }
}