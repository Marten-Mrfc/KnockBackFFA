package dev.marten_mrfcyt.knockbackffa.kits.modifiers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.Modify
import dev.marten_mrfcyt.knockbackffa.kits.ModifyHandler
import dev.marten_mrfcyt.knockbackffa.kits.ModifyObject
import mlib.api.utilities.getCustomValue
import mlib.api.utilities.message
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.configuration.file.YamlConfiguration
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
import java.io.File

@Modify("jumpPad")
object JumpPadModifier : ModifyObject(
    id = "jumpPad",
    name = "<white>Jump Pad Modifier",
    description = listOf("A pressure plate that launches you upwards", "item must be a pressure plate"),
    icon = Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
    args = listOf(
        "velocity" to Int::class.java,
        "delay" to Int::class.java
    ),
    plugin = KnockBackFFA.instance
), Listener {
    override fun handle(player: Player, item: ItemStack, args: Map<String, Any>) {
        val config = File("${plugin.dataFolder}/kits.yml")
        val kitConfig = YamlConfiguration.loadConfiguration(config)
        val block = args["block"] as? Block ?: return
        val kitName = getCustomValue(item.itemMeta, plugin, "kit_name") as? String ?: return
        val slot = getCustomValue(item.itemMeta, plugin, "slot") as? Int ?: return
        block.setMetadata("jumpPad", FixedMetadataValue(plugin, true))
        block.setMetadata("kit_name", FixedMetadataValue(plugin, kitName))
        block.setMetadata("slot", FixedMetadataValue(plugin, slot))
        val delay = kitConfig.get("kit.$kitName.items.$slot.modifiers.delay") ?: player.message("Invalid delay")
        if (delay !is Int) return player.message("Invalid delay")
        object : BukkitRunnable() {
            override fun run() {
                block.type = Material.AIR
            }
        }.runTaskLater(plugin, delay * 20L)
    }

    @EventHandler
    fun placePressurePlateEvent(event: BlockPlaceEvent) {
        val item = event.itemInHand
        val args = mapOf("block" to event.block)
        ModifyHandler().handleEvent(event.player, item, args, id)
    }

    @EventHandler
    fun onPressurePlatePressEvent(event: PlayerInteractEvent) {
        val config = File("${plugin.dataFolder}/kits.yml")
        val kitConfig = YamlConfiguration.loadConfiguration(config)
        if (event.action != Action.PHYSICAL) return
        val block = event.clickedBlock ?: return

        if (block.type.name.endsWith("_PRESSURE_PLATE") && block.hasMetadata("jumpPad")) {
            val player = event.player
            val kitName = block.getMetadata("kit_name").firstOrNull()?.asString() ?: return
            val slot = block.getMetadata("slot").firstOrNull()?.asInt() ?: return

            val velocity = kitConfig.getInt("kit.$kitName.items.$slot.modifiers.velocity", 1)

            player.velocity = Vector(0, velocity, 0)
        }
    }
}