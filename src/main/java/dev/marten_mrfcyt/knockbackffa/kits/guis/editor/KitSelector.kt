package dev.marten_mrfcyt.knockbackffa.kits.guis.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.io.File
import java.util.*
import kotlin.math.ceil

class KitSelector(private val plugin: KnockBackFFA) {
    fun editKitSelector(source: CommandSender) {
        if (source is Player) {
            val config = File("${plugin.dataFolder}/kits.yml")
            val kitConfig = YamlConfiguration.loadConfiguration(config)
            val kits = kitConfig.getConfigurationSection("kit")?.getKeys(false)
            val kitCount = kits?.size ?: 0
            val inventorySize = ceil((kitCount + 1) / 9.0).toInt() * 9
            val inventory =
                Bukkit.createInventory(null, inventorySize, "<gray>Please select or create kit</gray>".asMini())

            // Load all kits from kits.yml
            kits?.forEach { kit ->
                val kitSection = kitConfig.getConfigurationSection("kit.$kit.show")
                if (kitSection == null) {
                    source.sendMessage("Kit $kit is missing from kits.yml")
                    return
                }
                val displayName = kitSection.getString("DisplayName")?.asMini()
                val lore = kitSection.getString("Lore")?.asMini()
                val displayItemMaterial = kitSection.getConfigurationSection("DisplayItem")?.getString("item")
                    ?.let { Material.getMaterial(it) }
                val enchantments =
                    kitSection.getConfigurationSection("DisplayItem")?.getConfigurationSection("enchants")
                if (displayName == null || lore == null || displayItemMaterial == null) {
                    source.sendMessage("Kit $kit is missing required fields in kits.yml")
                    return
                }
                val item = ItemStack(displayItemMaterial)// Create the item with the specified material
                val meta: ItemMeta = item.itemMeta

                meta.displayName(displayName)
                meta.lore(listOf(lore))
                setCustomValue(meta, plugin, "6F70656E5F6B69745F656469746F72", "open_kit_editor")
                setCustomValue(meta, plugin, "kit_name", kit)
                // Apply enchantments
                enchantments?.getKeys(false)?.forEach { enchantmentKey ->
                    val namespacedKey = NamespacedKey.minecraft(enchantmentKey.lowercase(Locale.getDefault()))
                    val enchantment = Registry.ENCHANTMENT.get(namespacedKey)
                    val level = enchantments.getInt(enchantmentKey)
                    if (enchantment != null) {
                        meta.addEnchant(enchantment, level, true)
                    }
                }

                item.itemMeta = meta
                inventory.addItem(item)
            }

            source.openInventory(inventory)
        } else {
            source.sendMessage("You must be a player to use this command!")
        }
    }
}