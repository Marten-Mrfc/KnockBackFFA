package dev.marten_mrfcyt.knockbackffa.boosts.boosts

import dev.marten_mrfcyt.knockbackffa.boosts.models.BoostType
import dev.marten_mrfcyt.knockbackffa.boosts.models.ConfigurableProperty
import dev.marten_mrfcyt.knockbackffa.boosts.models.ItemBoost
import mlib.api.utilities.message
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.util.Vector
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@BoostType("leap")
object LeapBoostType : ItemBoost(
    id = "leap",
    enabled = true,
    name = "Leap Boost",
    description = listOf("Right-click to launch yourself forward", "Great for escaping or chasing"),
    icon = Material.FEATHER,
    price = 1
), Listener {

    @ConfigurableProperty(configKey = "leapStrength", defaultValue = "1.5")
    private var leapStrength = 1.5

    @ConfigurableProperty(configKey = "cooldown", defaultValue = "10")
    private var cooldown = 10

    @ConfigurableProperty(configKey = "verticalBoost", defaultValue = "0.2")
    private var verticalBoost = 0.2

    @ConfigurableProperty(configKey = "sound", defaultValue = "ENTITY_FIREWORK_ROCKET_LAUNCH")
    private var leapSound = Sound.ENTITY_FIREWORK_ROCKET_LAUNCH

    @ConfigurableProperty(configKey = "soundVolume", defaultValue = "1.0")
    private var soundVolume = 1.0f

    @ConfigurableProperty(configKey = "soundPitch", defaultValue = "1.0")
    private var soundPitch = 1.0f

    private val cooldowns = ConcurrentHashMap<UUID, Long>()

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return

        if (!event.action.isRightClick || !isBoostItem(item)) return

        val lastUse = cooldowns[player.uniqueId] ?: 0L
        val currentTime = System.currentTimeMillis() / 1000

        if (currentTime - lastUse < cooldown) {
            val remaining = cooldown - (currentTime - lastUse)
            player.message("<red>You must wait ${remaining}s before using this again!")
            return
        }

        val direction = player.location.direction
        player.velocity = Vector(
            direction.x * leapStrength,
            verticalBoost,
            direction.z * leapStrength
        )

        player.playSound(player.location, leapSound, soundVolume, soundPitch)

        cooldowns[player.uniqueId] = currentTime

        if (player.gameMode != org.bukkit.GameMode.CREATIVE) {
            item.amount--
        }

        event.isCancelled = true
    }
}