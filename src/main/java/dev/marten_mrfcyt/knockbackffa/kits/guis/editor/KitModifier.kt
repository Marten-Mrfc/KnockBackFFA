package dev.marten_mrfcyt.knockbackffa.kits.guis.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.*
import lirand.api.extensions.inventory.set
import net.kyori.adventure.text.Component
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

class KitModifier(private val plugin: KnockBackFFA) {
    fun openNewKitGUI(source: CommandSender, name: Component, lore: Component, kitName: String, new: Boolean = true) {
        if (source is Player) {
            val config = File("${plugin.dataFolder}/kits.yml")
            val kitConfig = YamlConfiguration.loadConfiguration(config)
            if (new) {
                if (kitConfig.contains("kit.$kitName")) {
                    source.error("Kit with this name already exists!")
                    return
                } else {
                    with(kitConfig) {
                        set("kit.$kitName.DisplayName", name.notMini())
                        set("kit.$kitName.Lore", lore.notMini())
                        set("kit.$kitName.DisplayItem.item", Material.STICK.name)
                        set("kit.$kitName.DisplayItem.enchants", mapOf("KNOCKBACK" to 2))
                    }
                }
            }
            // Create the inventory
            val inventorySize = 18
            val edittext = "<gray>Editing:</gray><white> ".asMini()
            val inventory = Bukkit.createInventory(null, inventorySize, edittext.append(name))
            val glassPane = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
            val glassMeta: ItemMeta = glassPane.itemMeta
            glassMeta.displayName("".asMini().asComponent())
            setCustomValue(glassMeta, plugin, "is_draggable", false)
            glassPane.itemMeta = glassMeta
            for (i in 9..17) {
                inventory[i] = glassPane
            }

            // Create a ItemStack to represent the kit
            val kitSection = kitConfig.getConfigurationSection("kit.$kitName")
            if (kitSection != null) {
                // Load the display name and lore
                val displayName = kitSection.getString("DisplayName")?.asMini()
                val kitLore = kitSection.getString("Lore")?.asMini()

                // Load the display item and enchantments
                val displayItemMaterial = kitSection.getConfigurationSection("DisplayItem")?.getString("item")?.let { Material.getMaterial(it) }
                val enchantments = kitSection.getConfigurationSection("DisplayItem")?.getConfigurationSection("enchants")

                // Create the ItemStack and ItemMeta
                if (displayName != null && kitLore != null && displayItemMaterial != null) {
                    val modifiedKit = ItemStack(displayItemMaterial)
                    val modifiedKitMeta: ItemMeta = modifiedKit.itemMeta

                    // Set the display name and lore
                    modifiedKitMeta.displayName(displayName)
                    val line = "<gray>------------------<reset>".asMini()
                    val toplore = "<dark_purple>Drag an item onto me".asMini()
                    val bottomlore = "<dark_purple>To change my DisplayIcon!".asMini()
                    modifiedKitMeta.lore(listOf(kitLore, line, toplore, bottomlore))
                    // Apply the enchantments
                    enchantments?.getKeys(false)?.forEach { enchantmentKey ->
                        val namespacedKey = NamespacedKey.minecraft(enchantmentKey.lowercase(Locale.getDefault()))
                        val enchantment = Registry.ENCHANTMENT.get(namespacedKey)
                        val level = enchantments.getInt(enchantmentKey)
                        if (enchantment != null) {
                            modifiedKitMeta.addEnchant(enchantment, level, true)
                        }
                    }
                    // Set the custom values
                    setCustomValue(modifiedKitMeta, plugin, "6B69745F646973706C61795F6974656D", "kit_display_item_check")
                    setCustomValue(modifiedKitMeta, plugin, "kit_name", kitName)

                    // Set the ItemMeta back to the ItemStack
                    modifiedKit.itemMeta = modifiedKitMeta
                    source.message("Setting item: $modifiedKit")
                    // Add the ItemStack to the inventory
                    inventory[13] = modifiedKit
                }
            }
            // editor items
            // Edit DisplayName
            val editDisplayName = ItemStack(Material.NAME_TAG)
            val editDisplayNameMeta: ItemMeta = editDisplayName.itemMeta
            editDisplayNameMeta.displayName("<gray>Edit Display Name".asMini())
            setCustomValue(editDisplayNameMeta, plugin, "6B69745F646973706C61795F6E616D655F65646974", "kit_display_name_edit")
            setCustomValue(editDisplayNameMeta, plugin, "kit_name", kitName)
            editDisplayName.itemMeta = editDisplayNameMeta
            inventory[0] = editDisplayName
            // edit Lore
            val editLore = ItemStack(Material.BOOK)
            val editLoreMeta: ItemMeta = editLore.itemMeta
            editLoreMeta.displayName("<gray>Edit Lore".asMini())
            setCustomValue(editLoreMeta, plugin, "6B69745F646973706C61795F6C6F72655F65646974", "kit_display_lore_edit")
            setCustomValue(editLoreMeta, plugin, "kit_name", kitName)
            editLore.itemMeta = editLoreMeta
            inventory[1] = editLore
            // save all the data
            kitConfig.save(config)
            source.openInventory(inventory)
        } else {
            source.sendMessage("You must be a player to use this command!")
        }
    }
}