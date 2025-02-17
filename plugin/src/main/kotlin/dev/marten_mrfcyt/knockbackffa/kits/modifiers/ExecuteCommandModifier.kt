package dev.marten_mrfcyt.knockbackffa.kits.modifiers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.Modify
import dev.marten_mrfcyt.knockbackffa.kits.ModifyHandler
import dev.marten_mrfcyt.knockbackffa.kits.ModifyObject
import dev.marten_mrfcyt.knockbackffa.utils.getCustomValue
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

@Modify("executeCommand")
object ExecuteCommandModifier : ModifyObject(
    id = "executeCommand",
    name = "<white>Execute Command Modifier",
    description = listOf("Executes a command on kill"),
    icon = Material.COMMAND_BLOCK,
    args = listOf("command" to String::class.java),
    plugin = KnockBackFFA.instance
), Listener {
    override fun handle(player: Player, item: ItemStack, args: Map<String, Any>) {
        val config = File("${plugin.dataFolder}/kits.yml")
        val kitConfig = YamlConfiguration.loadConfiguration(config)
        val slot = (args["slot"] as? Int) ?: return
        val kitName = (args["kit_name"] as? String) ?: return
        val command = kitConfig.getString("kit.$kitName.items.$slot.modifiers.command") ?: return
        val parsedCommand = PlaceholderAPI.setPlaceholders(player, command)
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand)
    }

    @EventHandler
    fun onKillExecute(event: PlayerDeathEvent) {
        val source = event.entity.killer ?: return
        for (item in source.inventory.contents) {
            if (item == null) continue
            val itemMeta = item.itemMeta ?: continue
            val kitName = getCustomValue(itemMeta, plugin, "kit_name") as? String ?: continue
            val slot = getCustomValue(itemMeta, plugin, "slot") as? Int ?: continue
            val args = mapOf(
                "slot" to slot,
                "kit_name" to kitName
            )
            ModifyHandler().handleEvent(source, item, args, id)
        }
    }
}