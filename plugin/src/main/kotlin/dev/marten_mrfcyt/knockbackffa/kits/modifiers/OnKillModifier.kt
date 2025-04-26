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
        val config = File("${plugin.dataFolder}/kits.yml")
        val kitConfig = YamlConfiguration.loadConfiguration(config)
        val slot = (args["slot"] as? Int) ?: return
        val kitName = (args["kit_name"] as? String) ?: return
        val amount = kitConfig.getInt("kit.$kitName.items.$slot.amount")

        val resolvedSlot = KitSlotResolver.resolveSlot(player, slot)

        item.amount = amount
        player.inventory.setItem(resolvedSlot, item)
    }

    @EventHandler
    fun onKill(event: PlayerDeathEvent) {
        val source = event.entity.killer ?: return
        for (item in source.inventory.contents) {
            if (item == null) continue
            val playerDataInstance = PlayerData.getInstance(plugin)
            val slot = source.inventory.contents.indexOfFirst {
                it?.isSimilar(item) == true
            }
            if (slot == -1) return
            val playerDataModel = playerDataInstance.getPlayerDataModel(event.player.uniqueId)
            val kitName = playerDataModel.kit ?: return
            val args = mapOf(
                "slot" to slot,
                "kit_name" to kitName
            )
            KnockBackFFA.instance.modifierManager.handleEvent(source, item, args, id)
        }
    }
}