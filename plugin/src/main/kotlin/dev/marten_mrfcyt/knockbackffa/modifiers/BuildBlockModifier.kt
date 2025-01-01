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
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
@Modify("buildBlock")
object BuildBlockModifier : ModifyObject(
    id = "buildBlock",
    name = "<white>Build Block Modifier",
    description = listOf("A building block that disappears over time", "Must start with WHITE_, e.g. WHITE_WOOL"),
    icon = Material.WHITE_WOOL,
    plugin = KnockBackFFA.instance
), Listener {
    override fun handle(player: Player, item: ItemStack, args: Map<String, Any>) {
        val itemInHand = item.type.name
        val baseItem = itemInHand.removePrefix("WHITE_").takeIf { itemInHand.startsWith("WHITE_") } ?: return
        val colors = listOf("WHITE", "YELLOW", "ORANGE", "RED", "AIR")
        val block = args["block"] as? org.bukkit.block.Block ?: return

        object : BukkitRunnable() {
            var counter = 0
            override fun run() {
                if (counter >= colors.size) {
                    cancel()
                    return
                }
                block.type = Material.getMaterial("${colors[counter]}_$baseItem") ?: Material.AIR
                counter++
            }
        }.runTaskTimer(plugin, 0L, 15L)
    }

    @EventHandler
    fun onBlockPlaceEvent(event: BlockPlaceEvent) {
        val args = mapOf("block" to event.block)
        ModifyHandler().handleEvent(event, event.player, event.itemInHand, args, id)
        ModifyHandler().handleEvent(event, event.player, event.itemInHand, args, "infinite")
    }
}