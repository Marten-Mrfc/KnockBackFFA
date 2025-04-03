package dev.marten_mrfcyt.knockbackffa.boosts.boosts

import dev.marten_mrfcyt.knockbackffa.boosts.models.*
import mlib.api.utilities.message
import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.event.*
import org.bukkit.event.entity.*
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@BoostType("knockback_explosion")
object KnockBackExplosionBoostType : ItemBoost(
    id = "knockback_explosion",
    enabled = true,
    name = "KnockBack Explosion",
    description = listOf("Throw to stick to surfaces", "Explodes to knock back nearby players"),
    icon = Material.MAGMA_CREAM,
    price = 5
), Listener {

    @ConfigurableProperty(configKey = "knockbackRadius", defaultValue = "4.0")
    private var knockbackRadius = 8.0

    @ConfigurableProperty(configKey = "knockbackStrength", defaultValue = "2.0")
    private var knockbackStrength = 3.0

    @ConfigurableProperty(configKey = "fuseTime", defaultValue = "3")
    private var fuseTime = 3

    @ConfigurableProperty(configKey = "cooldown", defaultValue = "15")
    private var cooldown = 15

    @ConfigurableProperty(configKey = "particleCount", defaultValue = "50")
    private var particleCount = 50

    @ConfigurableProperty(configKey = "explosionSound", defaultValue = "ENTITY_GENERIC_EXPLODE")
    private var explosionSound = Sound.ENTITY_GENERIC_EXPLODE

    @ConfigurableProperty(configKey = "throwSound", defaultValue = "ENITY_WIND_CHARGE_THROW")
    private var throwSound = Sound.ENTITY_WIND_CHARGE_THROW

    @ConfigurableProperty(configKey = "explosionParticle", defaultValue = "EXPLOSION_LARGE")
    private var explosionParticle = Particle.EXPLOSION

    @ConfigurableProperty(configKey = "warningParticle", defaultValue = "FLAME")
    private var warningParticle = Particle.FLAME

    @ConfigurableProperty(configKey = "upwardKnockback", defaultValue = "0.3")
    private var upwardKnockback = 0.3

    @ConfigurableProperty(configKey = "message_cooldown", defaultValue = "<red>KnockBack Explosion is on cooldown! (<seconds> seconds left)")
    private var cooldownMessage = "<red>KnockBack Explosion is on cooldown! (<seconds> seconds left)"

    @ConfigurableProperty(configKey = "message_knockback", defaultValue = "<red>You were knocked back by an explosion!")
    private var knockbackMessage = "<red>You were knocked back by an explosion!"

    private val cooldowns = ConcurrentHashMap<UUID, Long>()
    private const val METADATA_KEY = "knockback_explosion_owner"
    private val pendingLaunches = mutableSetOf<UUID>()

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return

        if (!isBoostItem(item) || !event.action.isRightClick || !canUseBoost(player, true)) return

        event.isCancelled = true
        pendingLaunches.add(player.uniqueId)

        launchKnockbackProjectile(player)
        applyCooldown(player)

        // Consume item
        if (item.amount > 1) item.amount -= 1 else player.inventory.setItemInMainHand(null)

        object : BukkitRunnable() {
            override fun run() { pendingLaunches.remove(player.uniqueId) }
        }.runTaskLater(plugin, 1L)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerThrow(event: ProjectileLaunchEvent) {
        val projectile = event.entity
        if (projectile !is WindCharge) return

        val shooter = projectile.shooter as? Player ?: return

        if (pendingLaunches.contains(shooter.uniqueId)) {
            projectile.setMetadata(METADATA_KEY, FixedMetadataValue(plugin, shooter.uniqueId.toString()))
            return
        }

        val item = shooter.inventory.itemInMainHand
        if (!isBoostItem(item)) return

        if (!canUseBoost(shooter, false)) {
            event.isCancelled = true
            return
        }

        projectile.setMetadata(METADATA_KEY, FixedMetadataValue(plugin, shooter.uniqueId.toString()))
        shooter.world.playSound(shooter.location, throwSound, 1.0f, 1.0f)
        applyCooldown(shooter)
    }

    private fun canUseBoost(player: Player, showMessage: Boolean = true): Boolean {
        val lastUse = cooldowns[player.uniqueId] ?: 0L
        val currentTime = System.currentTimeMillis() / 1000
        val timeRemaining = cooldown - (currentTime - lastUse)

        if (timeRemaining > 0) {
            if (showMessage) {
                player.message(cooldownMessage.replace("<seconds>", timeRemaining.toString()))
            }
            return false
        }
        return true
    }

    private fun applyCooldown(player: Player) {
        cooldowns[player.uniqueId] = System.currentTimeMillis() / 1000
    }

    private fun launchKnockbackProjectile(player: Player): WindCharge {
        val windCharge = player.launchProjectile(WindCharge::class.java)
        windCharge.setMetadata(METADATA_KEY, FixedMetadataValue(plugin, player.uniqueId.toString()))
        windCharge.velocity = player.location.direction.multiply(1.5)
        player.world.playSound(player.location, throwSound, 1.0f, 1.0f)
        return windCharge
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity as? WindCharge ?: return
        if (!projectile.hasMetadata(METADATA_KEY)) return

        val location = projectile.location
        val ownerIdString = projectile.getMetadata(METADATA_KEY)[0].asString()
        val ownerId = UUID.fromString(ownerIdString)
        event.isCancelled = true

        object : BukkitRunnable() {
            private var secondsLeft = fuseTime

            override fun run() {
                if (secondsLeft <= 0) {
                    explode(location, ownerId)
                    cancel()
                    return
                }

                location.world.spawnParticle(warningParticle, location, 10, 0.1, 0.1, 0.1, 0.0)
                location.world.playSound(location, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f)
                secondsLeft--
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }

    private fun explode(location: Location, ownerId: UUID) {
        location.world.spawnParticle(explosionParticle, location, particleCount, 0.5, 0.5, 0.5, 0.1)
        location.world.playSound(location, explosionSound, 2.0f, 1.0f)

        location.world.getNearbyEntities(location, knockbackRadius, knockbackRadius, knockbackRadius)
            .filter { it.uniqueId != ownerId }
            .forEach { entity ->
                val direction = entity.location.toVector().subtract(location.toVector())
                if (direction.lengthSquared() > 0) {
                    direction.normalize()
                    val knockback = direction.multiply(knockbackStrength).setY(upwardKnockback)
                    entity.velocity = entity.velocity.add(knockback)
                    entity.message(knockbackMessage)
                }
            }
    }
}