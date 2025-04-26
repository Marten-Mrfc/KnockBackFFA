package dev.marten_mrfcyt.knockbackffa.kits.modifiers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.KitSlotResolver
import dev.marten_mrfcyt.knockbackffa.kits.managers.KitManager
import dev.marten_mrfcyt.knockbackffa.kits.models.KitModifier
import dev.marten_mrfcyt.knockbackffa.kits.models.ModifyObject
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.util.logging.Level

@KitModifier("delay")
object DelayModifier : ModifyObject(
    id = "delay",
    name = "<white>Delay Modifier",
    description = listOf("Adds a delay to an item", "item must be a bow"),
    icon = Material.CLOCK,
    args = listOf("amount" to Int::class.java),
    plugin = KnockBackFFA.instance
), Listener {
    override fun handle(player: Player, item: ItemStack, args: Map<String, Any>) {
        try {
            val config = File("${plugin.dataFolder}/kits.yml")
            val kitConfig = YamlConfiguration.loadConfiguration(config)
            val slot = (args["slot"] as? Int)
            val kitName = (args["kit_name"] as? String)

            if (slot == null || kitName == null) {
                KnockBackFFA.instance.logger.warning("[DelayModifier] Missing required args. slot: $slot, kitName: $kitName")
                return
            }

            val delay = kitConfig.getInt("kit.$kitName.items.$slot.modifiers.amount", 20)
            KnockBackFFA.instance.logger.info("[DelayModifier] Setting cooldown for ${player.name} on item ${item.type} to ${delay * 20} ticks")
            player.setCooldown(item.type, delay * 20)
        } catch (e: Exception) {
            KnockBackFFA.instance.logger.log(Level.SEVERE, "[DelayModifier] Error applying delay to item", e)
        }
    }

    @EventHandler
    fun onBowShoot(event: EntityShootBowEvent) {
        try {
            KnockBackFFA.instance.logger.info("[DelayModifier] Bow shoot event triggered")
            if (event.entity !is Player) return

            val player = event.entity as Player
            val item = event.bow ?: return
            KnockBackFFA.instance.logger.info("[DelayModifier] Bow shoot event for player ${player.name} with item ${item.type}")
            // Find bow's slot in player inventory
            val slot = player.inventory.contents.indexOfFirst {
                it?.isSimilar(item) == true
            }

            if (slot == -1) {
                KnockBackFFA.instance.logger.info("[DelayModifier] Could not find bow in player inventory for ${player.name}")
                return
            }

            val playerDataInstance = PlayerData.getInstance(plugin)
            val playerDataModel = playerDataInstance.getPlayerDataModel(player.uniqueId)
            val kitName = playerDataModel.kit

            if (kitName == null) {
                KnockBackFFA.instance.logger.info("[DelayModifier] No active kit for player ${player.name}")
                return
            }

            // Get the original kit slot
            val originalKitSlot = KitSlotResolver.resolveNewSlot(player, slot)

            val args = mutableMapOf(
                "slot" to originalKitSlot,
                "kit_name" to kitName
            )

            // Get the kit manager and the player's current kit
            val kitManager = KitManager(plugin)
            val kit = kitManager.getKit(kitName)
            // Always restore arrows on the next tick
            object : BukkitRunnable() {
                override fun run() {
                    try {
                        // Find all arrow slots in the kit
                        val arrowSlots = kit.items.entries
                            .filter { it.value.material.toString().endsWith("ARROW") }
                            .map { it.key }

                        if (arrowSlots.isEmpty()) {
                            KnockBackFFA.instance.logger.fine("[DelayModifier] No arrows found in kit configuration for $kitName")
                            return
                        }

                        // Restore arrows in all arrow slots
                        for (arrowKitSlot in arrowSlots) {
                            val arrowInventorySlot = KitSlotResolver.resolveSlot(player, arrowKitSlot)
                            val kitArrowItem = kit.items[arrowKitSlot]

                            if (kitArrowItem != null) {
                                val arrowItem = kitArrowItem.build(KnockBackFFA.instance)

                                player.inventory.setItem(arrowInventorySlot, arrowItem)

                                KnockBackFFA.instance.logger.fine("[DelayModifier] Restored ${arrowItem.displayName()} for ${player.name} to slot $arrowInventorySlot with amount ${arrowItem.amount}")
                            }
                        }

                        // Update player inventory
                        player.updateInventory()
                    } catch (e: Exception) {
                        KnockBackFFA.instance.logger.log(Level.SEVERE, "[DelayModifier] Error restoring arrows", e)
                    }
                }
            }.runTaskLater(KnockBackFFA.instance, 1L)
            KnockBackFFA.instance.modifierManager.handleEvent(player, item, args, id)

        } catch (e: Exception) {
            KnockBackFFA.instance.logger.log(Level.SEVERE, "[DelayModifier] Error processing bow shoot event", e)
        }
    }
}