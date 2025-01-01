package dev.marten_mrfcyt.knockbackffa.modifiers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.annotations.Modify
import dev.marten_mrfcyt.knockbackffa.handlers.ModifyHandler
import dev.marten_mrfcyt.knockbackffa.handlers.ModifyObject
import dev.marten_mrfcyt.knockbackffa.utils.getCustomValue
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import java.io.File

@Modify("onKill")
object OnKillModifier : ModifyObject(
    id = "onKill",
    name = "<white>On Kill Modifier",
    description = listOf("Restores item amount on kill"),
    icon = Material.DIAMOND_SWORD,
    plugin = KnockBackFFA.instance
), Listener {
    override fun handle(player: Player, item: ItemStack, args: Map<String, Any>) {
        val config = File("${plugin.dataFolder}/kits.yml")
        val kitConfig = YamlConfiguration.loadConfiguration(config)
        val slot = (args["slot"] as? Int) ?: return
        val kitName = (args["kit_name"] as? String) ?: return
        val amount = kitConfig.getInt("kit.$kitName.items.$slot.amount")
        item.amount = amount
        player.inventory.setItem(slot, item)
    }

    @EventHandler
    fun onKill(event: PlayerDeathEvent) {
        val source = event.entity.killer ?: return
        for (item in source.inventory.contents) {
            if (item == null) continue
            val itemMeta = item.itemMeta ?: continue
            val slot = getCustomValue(itemMeta, plugin, "slot") as? Int ?: continue
            val kitName = getCustomValue(itemMeta, plugin, "kit_name") as? String ?: continue
            val args = mapOf(
                "slot" to slot,
                "kit_name" to kitName
            )
            ModifyHandler().handleEvent(event, source, item, args, id)
        }
    }
}