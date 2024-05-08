package dev.marten_mrfcyt.knockbackffa.kits.custom

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.checkCustomValue
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent

class BuildBlocks(private val plugin: KnockBackFFA) : Listener {
    @EventHandler
    fun onBlockPlaceEvent(event: BlockPlaceEvent) {
      if (checkCustomValue(event.itemInHand.itemMeta, plugin, "is_placeBlock", true)) {
        event.isCancelled = true
      }
    }
}