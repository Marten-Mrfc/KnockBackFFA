package dev.marten_mrfcyt.knockbackffa.kits.modifiers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.models.KitModifier
import dev.marten_mrfcyt.knockbackffa.kits.models.ModifyObject
import mlib.api.utilities.getCustomValue
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File

@KitModifier("infinite")
object InfiniteModifier : ModifyObject(
    id = "infinite",
    name = "<white>Infinite Modifier",
    description = listOf("Makes the item infinite"),
    icon = Material.BEDROCK,
    plugin = KnockBackFFA.instance
) {
    override fun handle(player: Player, item: ItemStack, args: Map<String, Any>) {
        val config = File("${KnockBackFFA.instance.dataFolder}/kits.yml")
        val kitConfig = YamlConfiguration.loadConfiguration(config)

        val slot = (getCustomValue(item.itemMeta, KnockBackFFA.instance, "slot") as? Int) ?: 0
        val kitName = (getCustomValue(item.itemMeta, KnockBackFFA.instance, "kit_name") as? String) ?: return
        val amount = kitConfig.get("kit.$kitName.items.$slot.amount") as Int
        item.amount = amount
        player.inventory.setItem(slot, item)
    }
}