package dev.marten_mrfcyt.knockbackffa.kits.modifiers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.Modify
import dev.marten_mrfcyt.knockbackffa.kits.ModifyHandler
import dev.marten_mrfcyt.knockbackffa.kits.ModifyObject
import dev.marten_mrfcyt.knockbackffa.utils.getCustomValue
import dev.marten_mrfcyt.knockbackffa.utils.setCustomValue
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
    args = listOf("velocity" to Int::class.java),
    plugin = KnockBackFFA.instance
), Listener {
    override fun handle(player: Player, item: ItemStack, args: Map<String, Any>) {
        val block = args["block"] as? Block ?: return
        block.setMetadata("jumpPad", FixedMetadataValue(plugin, true))
        plugin.logger.info("Jump pad placed by ${player.name}")
        object : BukkitRunnable() {
            override fun run() {
                block.type = Material.AIR
                plugin.logger.info("Jump pad removed after timeout")
            }
        }.runTaskLater(plugin, 100L)
    }

    @EventHandler
    fun placePressurePlateEvent(event: BlockPlaceEvent) {
        plugin.logger.info("Block placed: ${event.block.type}")
        val item = event.itemInHand
        val kitName = getCustomValue(item.itemMeta, plugin, "kit_name")
        plugin.logger.info("Kit name: $kitName")

        val args = mapOf("block" to event.block)
        ModifyHandler().handleEvent(event.player, item, args, id)
        event.blockPlaced.setMetadata("kit_name", FixedMetadataValue(plugin, kitName))
    }

    @EventHandler
    fun onPressurePlatePressEvent(event: PlayerInteractEvent) {
        if (event.action != Action.PHYSICAL) return
        val block = event.clickedBlock ?: return

        plugin.logger.info("Pressure plate interaction")
        plugin.logger.info("Block type: ${block.type}")
        plugin.logger.info("Has jumpPad meta: ${block.hasMetadata("jumpPad")}")

        if (block.type.name.endsWith("_PRESSURE_PLATE") && block.hasMetadata("jumpPad")) {
            val player = event.player
            val kitName = block.getMetadata("kit_name").firstOrNull()?.asString()

            plugin.logger.info("Player: ${player.name}")
            plugin.logger.info("Kit name from metadata: $kitName")

            if (kitName == null) {
                plugin.logger.warning("No kit name found in metadata")
                return
            }

            val config = File("${plugin.dataFolder}/kits.yml")
            val kitConfig = YamlConfiguration.loadConfiguration(config)
            val slot = 7 // Using fixed slot 7 for jump pads
            val velocity = kitConfig.getInt("kit.$kitName.items.$slot.modifiers.velocity", 1)

            plugin.logger.info("Applying velocity: $velocity")
            player.velocity = Vector(0, velocity, 0)

            object : BukkitRunnable() {
                override fun run() {
                    block.type = Material.AIR
                    plugin.logger.info("Jump pad removed after use")
                }
            }.runTaskLater(plugin, 20L)
        }
    }
}