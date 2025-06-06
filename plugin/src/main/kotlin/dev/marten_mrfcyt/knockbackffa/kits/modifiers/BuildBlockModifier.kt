package dev.marten_mrfcyt.knockbackffa.kits.modifiers

import dev.marten_mrfcyt.knockbackffa.kits.models.ModifyObject
import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.KitSlotResolver
import dev.marten_mrfcyt.knockbackffa.kits.models.KitModifier
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.util.logging.Level

@KitModifier("buildBlock")
object BuildBlockModifier : ModifyObject(
    id = "buildBlock",
    name = "<white>Build Block Modifier",
    description = listOf("A building block that disappears over time", "Must start with WHITE_, e.g. WHITE_WOOL"),
    icon = Material.WHITE_WOOL,
    plugin = KnockBackFFA.instance
), Listener {
    override fun handle(player: Player, item: ItemStack, args: Map<String, Any>) {
        try {
            val itemInHand = item.type.name
            val baseItem = itemInHand.removePrefix("WHITE_").takeIf { itemInHand.startsWith("WHITE_") }

            if (baseItem == null) {
                plugin.logger.warning("[BuildBlockModifier] Invalid item type: $itemInHand for player ${player.name}. Must start with WHITE_")
                return
            }

            val colors = listOf("WHITE", "YELLOW", "ORANGE", "RED", "AIR")
            val block = args["block"] as? Block

            if (block == null) {
                plugin.logger.warning("[BuildBlockModifier] Missing block argument for player ${player.name}")
                return
            }


            object : BukkitRunnable() {
                var counter = 0
                override fun run() {
                    if (counter >= colors.size) {
                        cancel()
                        return
                    }

                    val material = if (counter < colors.size - 1) {
                        Material.getMaterial("${colors[counter]}_$baseItem")
                    } else {
                        Material.AIR
                    }

                    if (material == null) {
                        plugin.logger.warning("[BuildBlockModifier] Failed to get material ${colors[counter]}_$baseItem")
                        cancel()
                        return
                    }

                    block.type = material
                    counter++
                }
            }.runTaskTimer(plugin, 0L, 15L)
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "[BuildBlockModifier] Error in handle method", e)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockPlaceEvent(event: BlockPlaceEvent) {
        if (event.isCancelled) return
        try {
            val player = event.player
            val playerDataInstance = PlayerData.getInstance(plugin)
            val playerDataModel = playerDataInstance.getPlayerDataModel(player.uniqueId)
            val kitName = playerDataModel.kit

            if (kitName == null) {
                plugin.logger.fine("[BuildBlockModifier] No active kit for player ${player.name}")
                return
            }

            val hand = event.hand
            val currentSlot = if (hand == EquipmentSlot.OFF_HAND) 40 else player.inventory.heldItemSlot

            val originalKitSlot = KitSlotResolver.resolveNewSlot(player, currentSlot)


            val args = mapOf(
                "block" to event.block,
                "slot" to originalKitSlot,
                "kit_name" to kitName
            )
            val kit = KnockBackFFA.kitManager.getKit(kitName)
            val amount = kit.items[originalKitSlot]?.amount
            
            // Check if the item has the buildblock modifier enabled
            val hasModifier = kit.items[originalKitSlot]?.modifiers?.filter { it.value == true }?.keys?.contains(id) ?: false
            if (!hasModifier) {
                return
            }
            
            println("${kit.items[originalKitSlot]?.modifiers?.filter { it.value == true }?.keys}" + "${kit.items[originalKitSlot]?.modifiers?.filter { it.value == true }?.values}")
            if (amount == null) {
                plugin.logger.warning("[BuildBlockModifier] Could not find original kit item amount for slot $originalKitSlot in kit $kitName")
                return
            }
            KnockBackFFA.instance.modifierManager.handleEvent(player, event.itemInHand, args, id)
            // Reset the item amount to the original kit amount based on which hand was used
            val item = if (hand == EquipmentSlot.OFF_HAND) {
                player.inventory.itemInOffHand
            } else {
                player.inventory.getItem(currentSlot)
            }

            if (item != null) {
                item.amount = amount

                if (hand == EquipmentSlot.OFF_HAND) {
                    player.inventory.setItemInOffHand(item)
                } else {
                    player.inventory.setItem(currentSlot, item)
                }
            } else {
                plugin.logger.warning("[BuildBlockModifier] Could not get item in player's ${"slot $currentSlot"}")
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "[BuildBlockModifier] Error processing block place event", e)
        }
    }
}