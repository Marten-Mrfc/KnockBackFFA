package dev.marten_mrfcyt.knockbackffa.kits.custom

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.checkCustomValue
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

class JumpPad(private val plugin: KnockBackFFA) : Listener {
    @EventHandler
    fun placePressurePlateEvent(event: BlockPlaceEvent) {
        if (checkCustomValue(event.itemInHand.itemMeta, plugin, "modify", listOf("jumpPad"))) {
            if (event.block.type.name.uppercase().endsWith("_PRESSURE_PLATE")) {
                event.block.setMetadata("jumpPad", FixedMetadataValue(plugin, true))
                // Schedule the block to turn into air after 5 seconds
                object : BukkitRunnable() {
                    override fun run() { event.block.type = Material.AIR
                    }
                }.runTaskLater(plugin, 100L) // 5 seconds later
            }
        }
    }

    @EventHandler
    fun onPressurePlatePressEvent(event: PlayerInteractEvent) {
        if (event.action != Action.PHYSICAL) return
        val block = event.clickedBlock
        if (block != null && block.type.name.endsWith("_PRESSURE_PLATE") && block.hasMetadata("jumpPad")) {
            val player = event.player
            player.velocity = Vector(0, 1, 0) // Launch the player upwards
            object : BukkitRunnable() {
                override fun run() {
                    block.type = Material.AIR
                }
            }.runTaskLater(plugin, 20L) // 1 second later
        }
    }
}