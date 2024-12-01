package dev.marten_mrfcyt.knockbackffa.kits.custom

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.checkCustomValue
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.scheduler.BukkitRunnable

class BuildBlocks(private val plugin: KnockBackFFA) : Listener {
    @EventHandler
    fun onBlockPlaceEvent(event: BlockPlaceEvent) {
        if (checkCustomValue(event.itemInHand.itemMeta, plugin, "modify", listOf("placeBlock"))) {
            val itemInHand = event.itemInHand.type.name
            val baseItem = if (itemInHand.startsWith("WHITE_")) itemInHand.removePrefix("WHITE_") else return
            val colors = listOf("WHITE", "YELLOW", "ORANGE", "RED", "AIR")
            object : BukkitRunnable() {
                var counter = 0
                override fun run() {
                    if (counter >= colors.size) {
                        this.cancel()
                        return
                    }
                    val color = colors[counter]
                    val material = if (color == "AIR") Material.AIR else Material.getMaterial("${color}_$baseItem")
                    if (material != null) {
                        event.block.type = material
                    }
                    counter++
                }
            }.runTaskTimer(plugin, 0L, 15L) // 15L = 0.75 seconds
        }
        checkInfinite(event.player, event.itemInHand)
    }
}