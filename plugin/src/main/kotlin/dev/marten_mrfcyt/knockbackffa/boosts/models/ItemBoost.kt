package dev.marten_mrfcyt.knockbackffa.boosts.models

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import mlib.api.utilities.asMini
import mlib.api.utilities.message
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.time.Duration

abstract class ItemBoost(
    id: String,
    enabled: Boolean,
    name: String,
    description: List<String>,
    icon: Material,
    price: Int,
    plugin: Plugin = KnockBackFFA.instance
) : Boost(id, enabled, name, description, icon, price, plugin) {

    override fun isTimed(): Boolean = false

    override fun getDuration(): Duration? = null

    open val itemAmount: Int = 1

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60

        return if (hours > 0) {
            "${hours}h ${minutes}m ${seconds}s"
        } else if (minutes > 0) {
            "${minutes}m ${seconds}s"
        } else {
            "${seconds}s"
        }
    }

    protected open fun createBoostItem(): ItemStack {
        val item = ItemStack(icon, itemAmount)
        val meta = item.itemMeta ?: return item

        meta.displayName(name.asMini())
        val lore = description.toMutableList()

        getDuration()?.let {
            lore.add("<gray>Duration: <white>${formatDuration(it)}")
        }
        meta.lore(lore.map { it.asMini() })
        val key = NamespacedKey(plugin, "boost_id")
        meta.persistentDataContainer.set(key, PersistentDataType.STRING, id)

        item.itemMeta = meta
        return item
    }

    fun isBoostItem(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val key = NamespacedKey(plugin, "boost_id")
        return meta.persistentDataContainer.has(key, PersistentDataType.STRING) &&
                meta.persistentDataContainer.get(key, PersistentDataType.STRING) == id
    }

    override fun apply(player: Player): Boolean {
        val boostItem = createBoostItem()
        val result = player.inventory.addItem(boostItem)

        return if (result.isEmpty()) {
            onBoostApplied(player)
            true
        } else {
            player.message("<red>Your inventory is full! Cannot add ${name}.")
            false
        }
    }
    override fun onBoostRemoved(player: Player) {
    }

}