// src/main/kotlin/dev/marten_mrfcyt/knockbackffa/kits/models/Kit.kt
package dev.marten_mrfcyt.knockbackffa.kits.models

import KitItem
import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import mlib.api.utilities.message
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import kotlin.apply
import kotlin.collections.set
import kotlin.text.clear

class Kit(
    val name: String,
    var displayName: String,
    var description: String,
    var displayIcon: Material = Material.STICK,
    var price: Int = 0,  // Add this line
    private val _items: MutableMap<Int, KitItem> = mutableMapOf(),
    private val _boosts: MutableList<String> = mutableListOf()
) {
    // Read-only view of items
    val items: Map<Int, KitItem> get() = _items.toMap()
    val boosts: List<String> get() = _boosts.toList()

    // Apply kit to player
    fun applyTo(player: Player) {
        player.inventory.clear()
        _items.forEach { (slot, kitItem) ->
            val builtItem = kitItem.build(KnockBackFFA.instance)
            player.inventory.setItem(slot, builtItem)
        }

        KnockBackFFA.instance.playerBoostManager.removeAllKitBoosts(player)
        KnockBackFFA.instance.playerBoostManager.handleKitChange(player, name)
    }


    // Get item at slot
    fun getItem(slot: Int): KitItem? = _items[slot]

    // Set item at slot
    fun setItem(slot: Int, item: KitItem) {
        _items[slot] = item
    }

    // Remove item at slot
    fun removeItem(slot: Int) {
        _items.remove(slot)
    }

    private fun removeBoosts(player: Player) {
        _boosts.forEach { boostId ->
            try {
                val boost = KnockBackFFA.instance.boostManager.getBoost(boostId)
                boost.remove(player)
            } catch (e: Exception) {
                KnockBackFFA.instance.logger.warning("Failed to remove boost $boostId from kit $name: ${e.message}")
            }
        }
    }

    private fun applyBoosts(player: Player) {
        _boosts.forEach { boostId ->
            try {
                val boost = KnockBackFFA.instance.boostManager.getBoost(boostId)
                boost.apply(player)
            } catch (e: Exception) {
                KnockBackFFA.instance.logger.warning("Failed to apply boost $boostId from kit $name: ${e.message}")
            }
        }
    }
    fun addBoost(boostId: String) {
        if (!_boosts.contains(boostId)) {
            _boosts.add(boostId)
            save()
        }
    }

    fun removeBoost(boostId: String) {
        _boosts.remove(boostId)
        save()
    }
    // Save kit to disk
    fun save() {
        val plugin = KnockBackFFA.instance
        val configFile = File(plugin.dataFolder, "kits.yml")
        val kitConfig = YamlConfiguration.loadConfiguration(configFile)

        // Save basic kit info
        kitConfig.set("kit.$name.show.DisplayName", displayName)
        kitConfig.set("kit.$name.show.Lore", description)
        kitConfig.set("kit.$name.show.DisplayItem.item", displayIcon.name)
        kitConfig.set("kit.$name.show.Price", price)  // Add this line

        // Save all items
        _items.forEach { (slot, kitItem) ->
            saveItemToConfig(kitConfig, slot, kitItem)
        }
        kitConfig.set("kit.$name.boosts", _boosts)

        // Save file
        kitConfig.save(configFile)

        // Update cache
        KnockBackFFA.kitManager.updateKitCache(name, this)
    }

    // Helper method for saving an item
    private fun saveItemToConfig(config: YamlConfiguration, slot: Int, item: KitItem) {
        val path = "kit.$name.items.$slot"

        config.set("$path.item", item.material)
        config.set("$path.name", item.name)
        config.set("$path.amount", item.amount)

        if (item.lore.isNotEmpty()) {
            config.set("$path.lore", item.lore)
        }

        // Save enchantments
        if (item.enchantments.isNotEmpty()) {
            val enchantsSection = config.createSection("$path.enchants")
            item.enchantments.forEach { (enchant, level) ->
                enchantsSection.set(enchant, level)
            }
        }

        // Save metadata
        if (item.metadata.isNotEmpty()) {
            val metaSection = config.createSection("$path.meta")
            item.metadata.forEach { (key, value) ->
                metaSection.set(key, value)
            }
        }

        // Save modifiers
        if (item.modifiers.isNotEmpty()) {
            val modifiersSection = config.createSection("$path.modifiers")
            item.modifiers.forEach { (key, value) ->
                modifiersSection.set(key, value)
            }
        }
    }

    // Modify a specific item's modifier
    fun setModifier(slot: Int, modifierId: String, enabled: Boolean, args: Map<String, Any>? = null) {
        val item = _items[slot] ?: return

        // Create a mutable copy of modifiers
        val modifiers = item.modifiers.toMutableMap()

        // Set the modifier state
        modifiers[modifierId] = enabled

        // Add any args
        args?.forEach { (key, value) ->
            modifiers[key] = value
        }

        // Create new KitItem with updated modifiers
        _items[slot] = item.copy(modifiers = modifiers)

        // Save changes to disk
        save()
    }

    companion object {
        // Load a kit from disk
        fun load(kitName: String): Kit? {
            val plugin = KnockBackFFA.instance
            val configFile = File(plugin.dataFolder, "kits.yml")
            val kitConfig = YamlConfiguration.loadConfiguration(configFile)

            if (!kitConfig.contains("kit.$kitName")) {
                return null
            }
            val boostsList = kitConfig.getStringList("kit.$kitName.boosts").toMutableList()

            val items = mutableMapOf<Int, KitItem>()
            val itemsSection = kitConfig.getConfigurationSection("kit.$kitName.items")

            itemsSection?.getKeys(false)?.forEach { slotStr ->
                val slot = slotStr.toInt()
                val section = itemsSection.getConfigurationSection(slotStr) ?: return@forEach

                val item = KitItem.fromConfig(section, kitName, slot)
                items[slot] = item
            }

            return Kit(
                name = kitName,
                displayName = kitConfig.getString("kit.$kitName.show.DisplayName") ?: kitName,
                description = kitConfig.getString("kit.$kitName.show.Lore") ?: "",
                displayIcon = kitConfig.getString("kit.$kitName.show.DisplayItem.item")?.let { Material.matchMaterial(it) } ?: Material.STICK,
                price = kitConfig.getInt("kit.$kitName.show.Price", 0),  // Add this line
                _items = items,
                _boosts = boostsList
            )
        }
    }
}