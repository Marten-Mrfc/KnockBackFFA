package dev.marten_mrfcyt.knockbackffa.kits.modifiers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.KitSlotResolver
import dev.marten_mrfcyt.knockbackffa.kits.models.KitModifier
import dev.marten_mrfcyt.knockbackffa.kits.managers.ModifierManager
import dev.marten_mrfcyt.knockbackffa.kits.models.ModifyObject
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import me.clip.placeholderapi.PlaceholderAPI
import mlib.api.utilities.getCustomValue
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

@KitModifier("executeCommand")
object ExecuteCommandModifier : ModifyObject(
    id = "executeCommand",
    name = "<white>Execute Command Modifier",
    description = listOf("Executes a command on kill"),
    icon = Material.COMMAND_BLOCK,
    args = listOf("command" to String::class.java),
    plugin = KnockBackFFA.instance
), Listener {
    override fun handle(player: Player, item: ItemStack, args: Map<String, Any>) {
        try {
            val config = File("${plugin.dataFolder}/kits.yml")
            val kitConfig = YamlConfiguration.loadConfiguration(config)
            val slot = (args["slot"] as? Int) ?: return
            val kitName = (args["kit_name"] as? String) ?: return
            val victim = args["victim"] as? Player
            
            val command = kitConfig.getString("kit.$kitName.items.$slot.modifiers.command") ?: return
            
            // Replace placeholders
            val parsedCommand = if (victim != null) {
                PlaceholderAPI.setPlaceholders(player, command)
                    .replace("%victim%", victim.name)
                    .replace("%victim_uuid%", victim.uniqueId.toString())
            } else {
                PlaceholderAPI.setPlaceholders(player, command)
            }
            
            plugin.logger.info("[ExecuteCommandModifier] Executing command: $parsedCommand for player ${player.name}")
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand)
        } catch (e: Exception) {
            plugin.logger.warning("[ExecuteCommandModifier] Error executing command: ${e.message}")
        }
    }

    @EventHandler
    fun onKillExecute(event: PlayerDeathEvent) {
        try {
            val killer = event.entity.killer ?: return
            val victim = event.entity
            
            val playerDataInstance = PlayerData.getInstance(plugin)
            val playerDataModel = playerDataInstance.getPlayerDataModel(killer.uniqueId)
            val kitName = playerDataModel.kit ?: return
            
            // Process main inventory slots
            for (slot in 0 until killer.inventory.size) {
                val item = killer.inventory.getItem(slot) ?: continue
                
                // Convert inventory slot to kit slot
                val kitSlot = KitSlotResolver.resolveNewSlot(killer, slot)
                
                val args = mapOf(
                    "slot" to kitSlot,
                    "kit_name" to kitName,
                    "victim" to victim
                )
                
                KnockBackFFA.instance.modifierManager.handleEvent(killer, item, args, id)
            }
        } catch (e: Exception) {
            plugin.logger.warning("[ExecuteCommandModifier] Error processing kill event: ${e.message}")
        }
    }
}