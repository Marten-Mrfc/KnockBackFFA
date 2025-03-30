package dev.marten_mrfcyt.knockbackffa.boosts.boosts

import dev.marten_mrfcyt.knockbackffa.boosts.models.BoostType
import dev.marten_mrfcyt.knockbackffa.boosts.models.ConfigurableProperty
import dev.marten_mrfcyt.knockbackffa.boosts.models.ItemBoost
import mlib.api.utilities.message
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.util.Vector
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@BoostType("grappling_hook")
object GrapplingHookBoostType : ItemBoost(
    id = "grappling_hook",
    enabled = true,
    name = "Grappling Hook",
    description = listOf("Cast your hook and pull yourself", "towards any surface or block"),
    icon = Material.FISHING_ROD,
    price = 10
), Listener {

    @ConfigurableProperty(configKey = "cooldown", defaultValue = "8")
    private var cooldown = 8

    @ConfigurableProperty(configKey = "pullStrength", defaultValue = "2.0")
    private var pullStrength = 2.0

    @ConfigurableProperty(configKey = "maxYVelocity", defaultValue = "0.8")
    private var maxYVelocity = 0.8

    @ConfigurableProperty(configKey = "upwardBoost", defaultValue = "0.2")
    private var upwardBoost = 0.2

    @ConfigurableProperty(configKey = "sound", defaultValue = "ENTITY_BAT_TAKEOFF")
    private var hookSound = Sound.ENTITY_BAT_TAKEOFF

    @ConfigurableProperty(configKey = "soundVolume", defaultValue = "1.0")
    private var soundVolume = 1.0f

    @ConfigurableProperty(configKey = "soundPitch", defaultValue = "1.2")
    private var soundPitch = 1.2f

    private val cooldowns = ConcurrentHashMap<UUID, Long>()

    @EventHandler
    fun onPlayerFish(event: PlayerFishEvent) {
        val player = event.player
        val item = player.inventory.itemInMainHand

        if (!isBoostItem(item)) return

        if (event.state == PlayerFishEvent.State.REEL_IN || event.state == PlayerFishEvent.State.IN_GROUND) {
            val lastUse = cooldowns[player.uniqueId] ?: 0L
            val currentTime = System.currentTimeMillis() / 1000

            if (currentTime - lastUse < cooldown) {
                val remaining = cooldown - (currentTime - lastUse)
                player.message("<red>Grappling Hook is on cooldown! ($remaining seconds left)")
                return
            }

            // Get hook and player locations
            val hookLoc = event.hook.location
            val playerLoc = player.location

            // Calculate vector between player and hook
            val vector = hookLoc.toVector().subtract(playerLoc.toVector())

            // Normalize and scale the vector for desired pull strength
            val velocity = vector.normalize().multiply(pullStrength)

            // Set a reasonable maximum y velocity to prevent extreme vertical movement
            if (velocity.y > maxYVelocity) velocity.y = maxYVelocity

            // Apply velocity to player
            player.velocity = velocity

            // Add a small upward boost to help with movement
            player.velocity = player.velocity.add(Vector(0.0, upwardBoost, 0.0))

            // Play sound and add cooldown
            player.world.playSound(player.location, hookSound, soundVolume, soundPitch)
            cooldowns[player.uniqueId] = currentTime

            // Prevent item damage
            event.hook.remove()
            event.isCancelled = true
        }
    }
}