package dev.marten_mrfcyt.knockbackffa.kits.custom

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.checkCustomValue
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.inventory.ItemStack

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
    if (checkCustomValue(item.itemMeta, KnockBackFFA.instance, "modify", listOf("delay"))) {
        player.setCooldown(Material.BOW, 300) // 300 ticks = 15 seconds
    }
}