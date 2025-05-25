package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.models.KitItem
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.ItemStack
import mlib.api.utilities.message
import java.util.UUID
import java.util.logging.Level

class KitLayoutManager(private val plugin: KnockBackFFA) : Listener {
    companion object {
        private val loadingKits = mutableSetOf<UUID>()

        fun markKitLoading(playerId: UUID) {
            loadingKits.add(playerId)
        }

        fun unmarkKitLoading(playerId: UUID) {
            loadingKits.remove(playerId)
        }

        fun isKitLoading(playerId: UUID): Boolean {
            return loadingKits.contains(playerId)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        if (isKitLoading(player.uniqueId)) {
            return
        }
        saveEntireLayout(player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onItemHeld(event: PlayerItemHeldEvent) {
        if (isKitLoading(event.player.uniqueId)) {
            return
        }

        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            saveEntireLayout(event.player)
        }, 1L)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onSwapHandItems(event: PlayerSwapHandItemsEvent) {
        if (isKitLoading(event.player.uniqueId)) {
            return
        }

        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            saveEntireLayout(event.player)
        }, 1L)
    }

    private fun saveEntireLayout(player: Player) {
        try {
            val playerDataInstance = PlayerData.getInstance(plugin)
            val playerData = playerDataInstance.getPlayerDataModel(player.uniqueId)

            val activeKitName = playerData.kit ?: "default"
            val kit = KnockBackFFA.kitManager.getKit(activeKitName)

            val inventory = player.inventory
            val finalLayout = mutableMapOf<Int, Int>() // originalKitSlot -> currentInventorySlot

            val foundKitItemsInfo = mutableListOf<Triple<Int, KitItem, Int>>() // Stores (originalSlotInKit, kitItemDefinition, currentActualInventorySlot)

            // Pass 1: Find all kit items and their current locations in the player's inventory
            // Main inventory slots are 0-35. Off-hand is slot 40.
            val inventorySlotsToSearch = (0..35).toList() + 40

            for ((origSlotInKit, kitItemDefinition) in kit.items) {
                val originalKitItemStack = kitItemDefinition.build(plugin)
                var foundActualSlot = -1

                for (currentInvSlot in inventorySlotsToSearch) {
                    val itemInPlayerInventory = inventory.getItem(currentInvSlot)
                    if (itemInPlayerInventory != null && itemInPlayerInventory.type != Material.AIR) {
                        if (areSimilarItems(originalKitItemStack, itemInPlayerInventory)) {
                            // Check if this inventory slot has already been claimed by another kit item found earlier in this pass
                            if (foundKitItemsInfo.none { it.third == currentInvSlot }) {
                                foundActualSlot = currentInvSlot
                                break // Found the item in a unique inventory slot
                            } else {
                                mlib.api.utilities.debug(plugin, "[KitLayoutManager] Slot $currentInvSlot for item (orig: $origSlotInKit) is already claimed by another found kit item. Skipping this instance.")
                            }
                        }
                    }
                }

                if (foundActualSlot != -1) {
                    foundKitItemsInfo.add(Triple(origSlotInKit, kitItemDefinition, foundActualSlot))
                } else {
                    mlib.api.utilities.debug(plugin, "[KitLayoutManager] Kit item (orig: $origSlotInKit, type: ${kitItemDefinition.material}) not found in player ${player.name}'s inventory.")
                }
            }

            // Determine which original kit slots correspond to items that are now missing
            val allOriginalSlots = kit.items.keys
            val foundOriginalSlots = foundKitItemsInfo.map { it.first }.toSet()
            val missingOriginalSlots = allOriginalSlots - foundOriginalSlots

            // Pass 2: Populate finalLayout, correcting positions if they conflict with default slots of missing items
            for ((origSlotOfFoundItem, _, actualSlotOfFoundItem) in foundKitItemsInfo) {
                val isStealingDefaultSlotOfMissingItem = missingOriginalSlots.contains(actualSlotOfFoundItem)

                if (isStealingDefaultSlotOfMissingItem && actualSlotOfFoundItem != origSlotOfFoundItem) { // Added check to ensure it's not already in its own default slot that happens to be a missing slot
                    mlib.api.utilities.debug(plugin, "[KitLayoutManager] Item from original kit slot $origSlotOfFoundItem (now in inv slot $actualSlotOfFoundItem) conflicts with default slot for a missing item. Attempting correction for ${player.name}.")
                    val itemBeingMoved = inventory.getItem(actualSlotOfFoundItem)

                    if (itemBeingMoved != null && itemBeingMoved.type != Material.AIR) {
                        inventory.clear(actualSlotOfFoundItem) // Vacate the conflicting slot

                        val targetOriginalSlot = origSlotOfFoundItem // Item's own default slot
                        val itemInTargetOriginalSlot = inventory.getItem(targetOriginalSlot)

                        if (itemInTargetOriginalSlot == null || itemInTargetOriginalSlot.type == Material.AIR) {
                            inventory.setItem(targetOriginalSlot, itemBeingMoved)
                            finalLayout[origSlotOfFoundItem] = targetOriginalSlot // Layout reflects it's in its default spot
                            mlib.api.utilities.debug(plugin, "[KitLayoutManager] Corrected for ${player.name}: Moved conflicting item from $actualSlotOfFoundItem to its default slot $targetOriginalSlot.")
                        } else {
                            mlib.api.utilities.debug(plugin, "[KitLayoutManager] Corrected for ${player.name}: Conflicting item from $actualSlotOfFoundItem. Its default slot $targetOriginalSlot is also occupied. Adding to general inventory.")
                            val remaining = player.inventory.addItem(itemBeingMoved) // Add to general inventory
                            if (remaining.isNotEmpty()) {
                                player.world.dropItemNaturally(player.location, remaining.values.first())
                                plugin.logger.warning("[KitLayoutManager] Corrected for ${player.name}: Inventory full after trying to re-add conflicting item. Item dropped: ${remaining.values.first().type}")
                            }
                            // Item's position is now managed by addItem, not reliably part of a "layout" if it went here.
                            // So, we don't add it to finalLayout with a specific slot if it was re-added generally or dropped.
                        }
                        player.message(TranslationManager.translate("kit.layout.item_slot_reverted"))
                        player.updateInventory() // Ensure client sees the change
                    } else {
                         plugin.logger.warning("[KitLayoutManager] Conflict detected at inv slot $actualSlotOfFoundItem for ${player.name}, but item there was null/air. No correction performed for this item.")
                    }
                    // If a correction was made, the original conflicting mapping (origSlotOfFoundItem -> actualSlotOfFoundItem) is NOT saved.
                    // finalLayout will either contain the corrected position or nothing for this item if it was re-added/dropped.
                } else {
                    // This mapping is safe, or the item is in its own default slot which happens to be a missing slot (no move needed)
                    if (isStealingDefaultSlotOfMissingItem && actualSlotOfFoundItem == origSlotOfFoundItem) {
                        mlib.api.utilities.debug(plugin, "[KitLayoutManager] Item for ${player.name} in original slot $origSlotOfFoundItem is safe, but this slot is also a default for a missing item. No change to item's position.")
                    }
                    finalLayout[origSlotOfFoundItem] = actualSlotOfFoundItem
                }
            }
            updateLayoutInPlayerData(player.uniqueId, activeKitName, finalLayout)

        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "[KitLayoutManager] Error saving entire kit layout", e)
        }
    }

    private fun areSimilarItems(item1: ItemStack, item2: ItemStack): Boolean {
        return try {

            item1.isSimilar(item2)
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "[KitLayoutManager] Error comparing items", e)
            false
        }
    }

    private fun updateLayoutInPlayerData(playerId: UUID, kitName: String, layout: Map<Int, Int>) {
        try {
            val playerDataInstance = PlayerData.getInstance(plugin)
            val playerData = playerDataInstance.getPlayerDataModel(playerId)
            val player = plugin.server.getPlayer(playerId)

            val updatedLayouts = playerData.kitLayouts.toMutableMap()
            val existingLayout = updatedLayouts[kitName] ?: emptyMap()

            if (player != null) {
                layout.forEach { (original, new) ->
                    val existingMapping = existingLayout[original]
                    if (existingMapping != new) {
                        player.message(TranslationManager.translate("kit.layout.changed",
                            "slot" to "$original",
                            "new_slot" to "$new"))
                    }
                }
            }
            if(layout == existingLayout) {
                mlib.api.utilities.debug(plugin, "[KitLayoutManager] No changes to layout for kit '$kitName' for player ${player?.name ?: playerId}. Not updating.")
                return
            }
            if (layout.isEmpty()) {
                updatedLayouts.remove(kitName)
            } else {
                updatedLayouts[kitName] = layout
            }

            playerData.kitLayouts = updatedLayouts
            playerDataInstance.savePlayerDataModel(playerId, playerData)
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "[KitLayoutManager] Error updating player data layout", e)
        }
    }
}