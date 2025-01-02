package dev.marten_mrfcyt.knockbackffa.modifiers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.annotations.Modify
import dev.marten_mrfcyt.knockbackffa.handlers.ModifyHandler
import dev.marten_mrfcyt.knockbackffa.handlers.ModifyObject
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import org.bukkit.inventory.ItemStack
import org.bukkit.event.block.Action

@Modify("jumpPad")
object JumpPadModifier : ModifyObject(
    id = "jumpPad",
    name = "<white>Jump Pad Modifier",
    description = listOf("A pressure plate that launches you upwards", "item must be a pressure plate"),
    icon = Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
    plugin = KnockBackFFA.instance
), Listener {
    override fun handle(player: Player, item: ItemStack, args: Map<String, Any>) {
        val block = args["block"] as? org.bukkit.block.Block ?: return
        block.setMetadata("jumpPad", FixedMetadataValue(plugin, true))
        object : BukkitRunnable() {
            override fun run() {
                block.type = Material.AIR
            }
        }.runTaskLater(plugin, 100L) // 5 seconds later
    }

    @EventHandler
    fun placePressurePlateEvent(event: BlockPlaceEvent) {
        val args = mapOf("block" to event.block)
        ModifyHandler().handleEvent(event.player, event.itemInHand, args, id)
    }

    @EventHandler
    fun onPressurePlatePressEvent(event: PlayerInteractEvent) {
        if (event.action != Action.PHYSICAL) return
        val block = event.clickedBlock ?: return
        if (block.type.name.endsWith("_PRESSURE_PLATE") && block.hasMetadata("jumpPad")) {
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