package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
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
        // Delayed save to allow the swap to complete
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            saveEntireLayout(event.player)
        }, 1L)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onSwapHandItems(event: PlayerSwapHandItemsEvent) {
        if (isKitLoading(event.player.uniqueId)) {
            return
        }
        // Delayed save to allow the swap to complete
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            saveEntireLayout(event.player)
        }, 1L)
    }

    private fun saveEntireLayout(player: Player) {
        try {
            val playerDataInstance = PlayerData.getInstance(plugin)
            val playerData = playerDataInstance.getPlayerDataModel(player.uniqueId)

            val activeKit = playerData.kit
            if (activeKit == null) {
                plugin.logger.warning("[KitLayoutManager] Player ${player.name} has no active kit, ignoring layout save")
                return
            }

            val kit = KnockBackFFA.kitManager.getKit(activeKit)

            // Map to store the current layout (originalSlot -> currentSlot)
            val currentLayout = mutableMapOf<Int, Int>()

            // Get all items from the player's inventory
            val inventory = player.inventory

            // Process each original kit item and find where it is in the player's inventory
            for ((origSlot, kitItem) in kit.items) {
                val kitItemStack = kitItem.build(plugin)

                // Find this item in player's inventory
                var foundSlot = -1
                for (slot in 0 until 36) { // Check main inventory (0-35)
                    val inventoryItem = inventory.getItem(slot)
                    if (inventoryItem != null && inventoryItem.type != Material.AIR &&
                        areSimilarItems(inventoryItem, kitItemStack)) {
                        foundSlot = slot
                        break
                    }
                }

                // Check offhand (40)
                if (foundSlot == -1) {
                    val offhandItem = inventory.getItem(40)
                    if (offhandItem != null && offhandItem.type != Material.AIR &&
                        areSimilarItems(offhandItem, kitItemStack)) {
                        foundSlot = 40
                    }
                }

                // If found at a different slot than original, record the mapping
                if (foundSlot != -1 && foundSlot != origSlot) {
                    currentLayout[origSlot] = foundSlot
                }
            }

            // Save the layout
            updateLayoutInPlayerData(player.uniqueId, activeKit, currentLayout)

        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "[KitLayoutManager] Error saving entire kit layout", e)
        }
    }

    private fun areSimilarItems(item1: ItemStack, item2: ItemStack): Boolean {
        return try {
            // Custom similarity check logic could be added here
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

            // Check for changes to notify player
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