package dev.marten_mrfcyt.knockbackffa.kits.custom

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.checkCustomValue
import dev.marten_mrfcyt.knockbackffa.utils.getCustomValue
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.io.File

class ItemCooldownListener : Listener {

    @EventHandler
    fun bowShootEvent(event: EntityShootBowEvent) {
        val player = event.entity as Player
        val item = player.inventory.itemInMainHand
        onItemCooldown(player, item)
        checkInfinite(player, event.consumable as ItemStack)
    }
}

fun onItemCooldown(player: Player, item: ItemStack) {
    // Check if delay modifier is enabled
    if (checkCustomValue(item.itemMeta, KnockBackFFA.instance, "modify", listOf("delay"))) {
        // Retrieve the delay amount from the item's metadata
        val config = File("${KnockBackFFA.instance.dataFolder}/kits.yml")
        val kitConfig = YamlConfiguration.loadConfiguration(config)
        val slot = (getCustomValue(item.itemMeta, KnockBackFFA.instance, "slot") as? Int) ?: 0
        val kitName = (getCustomValue(item.itemMeta, KnockBackFFA.instance, "kit_name") as? String) ?: return
        val delayAmount = kitConfig.get("kit.$kitName.items.$slot.modifiers.amount") as Int
        // Set the cooldown on the bow based on the delay amount
        player.setCooldown(Material.BOW, delayAmount)
    }
}
