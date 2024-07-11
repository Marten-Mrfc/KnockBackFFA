package dev.marten_mrfcyt.knockbackffa.kits.custom

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.checkCustomValue
import dev.marten_mrfcyt.knockbackffa.utils.getCustomValue
import dev.marten_mrfcyt.knockbackffa.utils.message
import lirand.api.extensions.inventory.set
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File

fun checkInfinite(source: Player, item: ItemStack) {
    val config = File("${KnockBackFFA.instance.dataFolder}/kits.yml")
    val kitConfig = YamlConfiguration.loadConfiguration(config)
    checkCustomValue(item.itemMeta, KnockBackFFA.instance, "modify", listOf("infinite")).let {
        val slot = (getCustomValue(item.itemMeta, KnockBackFFA.instance, "slot") as? Int) ?: 0
        val kitName = (getCustomValue(item.itemMeta, KnockBackFFA.instance, "kit_name") as? String) ?: return
        val amount = kitConfig.get("kit.$kitName.items.$slot.amount") as Int
        item.amount = amount
        source.inventory[slot] = item
    }
}