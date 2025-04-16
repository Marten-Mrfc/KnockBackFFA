package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object KitSlotResolver {
    /**
     * Resolves the actual inventory slot based on the player's custom layout
     *
     * @param player The player
     * @param originalSlot The original slot from the kit definition
     * @return The actual slot where the item is located, or originalSlot if no custom layout
     */
    fun resolveSlot(player: Player, originalSlot: Int): Int {
        val plugin = KnockBackFFA.instance
        val playerData = PlayerData.getInstance(plugin).getPlayerDataModel(player.uniqueId)
        val kitName = playerData.kit ?: return originalSlot

        return playerData.kitLayouts[kitName]?.get(originalSlot) ?: originalSlot
    }

    /**
     * Finds the kit item in the player's inventory based on custom layout
     *
     * @param player The player
     * @param kitName The kit name
     * @param originalSlot The original slot from the kit definition
     * @return The ItemStack at the resolved slot, or null if not found
     */
    fun findKitItem(player: Player, kitName: String, originalSlot: Int): ItemStack? {
        val plugin = KnockBackFFA.instance
        val playerData = PlayerData.getInstance(plugin).getPlayerDataModel(player.uniqueId)

        val resolvedSlot = playerData.kitLayouts[kitName]?.get(originalSlot) ?: originalSlot
        return player.inventory.getItem(resolvedSlot)
    }
}