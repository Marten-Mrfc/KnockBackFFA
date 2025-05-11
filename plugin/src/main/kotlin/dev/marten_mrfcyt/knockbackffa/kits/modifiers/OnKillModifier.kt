package dev.marten_mrfcyt.knockbackffa.kits.modifiers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.KitSlotResolver
import dev.marten_mrfcyt.knockbackffa.kits.models.KitModifier
import dev.marten_mrfcyt.knockbackffa.kits.models.ModifyObject
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import mlib.api.utilities.getCustomValue
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import java.io.File

@KitModifier("onKill")
object OnKillModifier : ModifyObject(
    id = "onKill",
    name = "<white>On Kill Modifier",
    description = listOf("Restores item amount on kill"),
    icon = Material.DIAMOND_SWORD,
    plugin = KnockBackFFA.instance
), Listener {
    override fun handle(player: Player, item: ItemStack, args: Map<String, Any>) {
        try {
            val config = File("${plugin.dataFolder}/kits.yml")
            val kitConfig = YamlConfiguration.loadConfiguration(config)
            val slot = (args["slot"] as? Int) ?: return
            val kitName = (args["kit_name"] as? String) ?: return
            val amount = kitConfig.getInt("kit.$kitName.items.$slot.amount", 1)
            
            val resolvedSlot = KitSlotResolver.resolveSlot(player, slot)
            
            plugin.logger.info("[OnKillModifier] Restoring item at slot $slot (resolved to $resolvedSlot) for player ${player.name} to amount $amount")
            
            item.amount = amount
            player.inventory.setItem(resolvedSlot, item)
            player.updateInventory()
        } catch (e: Exception) {
            plugin.logger.warning("[OnKillModifier] Error handling item restore: ${e.message}")
        }
    }

    @EventHandler
    fun onKill(event: PlayerDeathEvent) {
        try {
            val killer = event.entity.killer ?: return
            
            val playerDataInstance = PlayerData.getInstance(plugin)
            val playerDataModel = playerDataInstance.getPlayerDataModel(killer.uniqueId)
            val kitName = playerDataModel.kit ?: return
            
            plugin.logger.info("[OnKillModifier] Player ${killer.name} killed ${event.entity.name}, checking inventory items")
            
            // Process main inventory slots
            for (slot in 0 until killer.inventory.size) {
                val item = killer.inventory.getItem(slot) ?: continue
                
                // Convert inventory slot to kit slot
                val kitSlot = KitSlotResolver.resolveNewSlot(killer, slot)
                
                val args = mapOf(
                    "slot" to kitSlot,
                    "kit_name" to kitName
                )
                
                KnockBackFFA.instance.modifierManager.handleEvent(killer, item, args, id)
            }
        } catch (e: Exception) {
            plugin.logger.warning("[OnKillModifier] Error processing kill event: ${e.message}")
        }
    }
}