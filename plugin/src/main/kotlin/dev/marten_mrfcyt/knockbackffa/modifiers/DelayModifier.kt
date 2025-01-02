package dev.marten_mrfcyt.knockbackffa.modifiers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.annotations.Modify
import dev.marten_mrfcyt.knockbackffa.handlers.ModifyHandler
import dev.marten_mrfcyt.knockbackffa.handlers.ModifyObject
import dev.marten_mrfcyt.knockbackffa.utils.asMini
import dev.marten_mrfcyt.knockbackffa.utils.getCustomValue
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

@Modify("delay")
object DelayModifier : ModifyObject(
    id = "delay",
    name = "<white>Delay Modifier",
    description = listOf("Adds a delay to an item", "item must be a bow"),
    icon = Material.CLOCK,
    args = listOf("amount" to Int::class.java),
    plugin = KnockBackFFA.instance
), Listener {
    override fun handle(player: Player, item: ItemStack, args: Map<String, Any>) {
        val config = File("${plugin.dataFolder}/kits.yml")
        val kitConfig = YamlConfiguration.loadConfiguration(config)
        val slot = (args["slot"] as? Int) ?: return
        val kitName = (args["kit_name"] as? String) ?: return
        val delay = kitConfig.getInt("kit.$kitName.items.$slot.modifiers.amount", 20)
        player.setCooldown(item.type, delay)
    }

    @EventHandler
    fun onBowShoot(event: EntityShootBowEvent) {
        val player = event.entity as Player
        val item = event.bow ?: return
        val slot = getCustomValue(item.itemMeta, plugin, "slot") as? Int ?: return
        val kitName = getCustomValue(item.itemMeta, plugin, "kit_name") as? String ?: return
        val args = mapOf(
            "slot" to slot,
            "kit_name" to kitName
        )
        ModifyHandler().handleEvent(event, player, item, args, id)
        ModifyHandler().handleEvent(event, player, event.consumable, args, "infinite")
    }
}