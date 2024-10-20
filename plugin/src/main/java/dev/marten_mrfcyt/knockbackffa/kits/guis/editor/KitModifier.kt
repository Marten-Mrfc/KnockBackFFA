package dev.marten_mrfcyt.knockbackffa.kits.guis.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.*
import lirand.api.extensions.inventory.set
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import java.io.File
import java.util.*

class KitModifier(private val plugin: KnockBackFFA) {
    val config = File("${plugin.dataFolder}/kits.yml")
    fun kitEditor(source: CommandSender, name: Component, lore: Component, kitName: String, new: Boolean = true) {
        val kitConfig = YamlConfiguration.loadConfiguration(config)
        if (source !is Player) {
            source.error("You must be a player to use this command!")
            return
        } else {
            if (new) {
                if (kitConfig.contains("kit.$kitName")) {
                    source.error("Kit with this name already exists!")
                    return
                } else {
                    with(kitConfig) {
                        set("kit.$kitName.show.DisplayName", name.notMini())
                        set("kit.$kitName.show.Lore", lore.notMini())
                        set("kit.$kitName.show.DisplayItem.item", Material.STICK.name)
                        set("kit.$kitName.show.DisplayItem.enchants", mapOf("KNOCKBACK" to 2))
                        save(config)
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
            val kitSection = kitConfig.getConfigurationSection("kit.$kitName.show")
            if (kitSection == null) {
                source.error("Kit $kitName is missing from kits.yml")
                return
            }                // Load the display name and lore
            val displayName = kitSection.getString("DisplayName")?.asMini()
            val kitLore = kitSection.getString("Lore")?.asMini()

            // Load the display item and enchantments
            val displayItemMaterial = kitSection.getConfigurationSection("DisplayItem")?.getString("item")
                ?.let { Material.getMaterial(it) }
            if (displayName == null || kitLore == null || displayItemMaterial == null) {
                source.error("Kit $kitName is missing required fields in kits.yml")
                return
            }
            val enchantments =
                kitSection.getConfigurationSection("DisplayItem")?.getConfigurationSection("enchants")

            // Create the ItemStack and ItemMeta
            val modifiedKit = ItemStack(displayItemMaterial)
            val modifiedKitMeta: ItemMeta = modifiedKit.itemMeta

            // Set the display name and lore
            modifiedKitMeta.displayName(displayName)
            val line = "<gray>------------------<reset>".asMini()
            val toplore = "<dark_purple>Drag an item onto me".asMini()
            val bottomlore = "<dark_purple>To change my DisplayIcon!".asMini()
            modifiedKitMeta.lore(listOf(kitLore, line, toplore, bottomlore))
            // Apply the enchantments
            val enchantmentRegistry = Registry.ENCHANTMENT

            enchantments?.getKeys(false)?.forEach { enchantmentKey ->
                // Creating an Adventure Key object
                val adventureKey = Key.key("minecraft", enchantmentKey.lowercase(Locale.getDefault()))

                // Convert Adventure Key to Bukkit NamespacedKey
                val namespacedKey = NamespacedKey(adventureKey.namespace(), adventureKey.value())

                val enchantment = enchantmentRegistry.get(namespacedKey)
                val level = enchantments.getInt(enchantmentKey)

                if (enchantment != null) {
                    modifiedKitMeta.addEnchant(enchantment, level, true)
                }
            }
            // Set the custom values
            setCustomValue(
                modifiedKitMeta,
                plugin,
                "6B69745F646973706C61795F6974656D",
                "kit_display_item_check"
            )
            setCustomValue(modifiedKitMeta, plugin, "kit_name", kitName)

            // Set the ItemMeta back to the ItemStack
            modifiedKit.itemMeta = modifiedKitMeta
            // Add the ItemStack to the inventory
            inventory[13] = modifiedKit
            // editor items
            // Edit DisplayName
            val editDisplayName = ItemStack(Material.NAME_TAG)
            val editDisplayNameMeta: ItemMeta = editDisplayName.itemMeta
            editDisplayNameMeta.displayName("<gray>Edit Display Name".asMini())
            setCustomValue(
                editDisplayNameMeta,
                plugin,
                "6B69745F646973706C61795F6E616D655F65646974",
                "kit_display_name_edit"
            )
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
            // Edit Items
            val editItems = ItemStack(Material.CHEST)
            val editItemsMeta: ItemMeta = editItems.itemMeta
            editItemsMeta.displayName("<gray>Edit Items".asMini())
            setCustomValue(editItemsMeta, plugin, "6B69745F646973706C61795F6974656D5F65646974", "kit_display_item_edit")
            setCustomValue(editItemsMeta, plugin, "kit_name", kitName)
            editItems.itemMeta = editItemsMeta
            inventory[2] = editItems
            // go back button
            val goBack = ItemStack(Material.BARRIER)
            val goBackMeta: ItemMeta = goBack.itemMeta
            goBackMeta.displayName("<gray>Go Back".asMini())
            setCustomValue(goBackMeta, plugin, "676F5F6261636B5F627574746F6E", "go_back_button")
            setCustomValue(goBackMeta, plugin, "menu", "kit_selector")
            goBack.itemMeta = goBackMeta
            inventory[8] = goBack
            // save all the data
            kitConfig.save(config)
            source.openInventory(inventory)
        }
    }

    fun editKitGUI(source: CommandSender, kitName: String) {
        val kitConfig = YamlConfiguration.loadConfiguration(config)
        if (source is Player) {
            val name = kitConfig.get("kit.$kitName.show.DisplayName")
            val inventorySize = 18
            val edittext = "<gray>Editing:</gray><white> $name".asMini()
            val inventory = Bukkit.createInventory(null, inventorySize, edittext)
            for (i in 0..17) {
                if (i < 8 || i > 8) {
                    val glassPane = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
                    val glassMeta: ItemMeta = glassPane.itemMeta
                    val toplore = "<dark_purple>Drag an item onto me".asMini()
                    val bottomlore = "<dark_purple>to change me completely!".asMini()
                    glassMeta.lore(listOf(toplore, bottomlore))
                    glassMeta.displayName("<gray>Click to edit slot</gray>".asMini().asComponent())
                    setCustomValue(glassMeta, plugin, "edit_kit_item", true)
                    setCustomValue(glassMeta, plugin, "kit_name", kitName)
                    glassPane.itemMeta = glassMeta
                    inventory[i] = glassPane
                }
            }
            // item loader
            val kitItemsSection = kitConfig.getConfigurationSection("kit.$kitName.items")
            kitItemsSection?.getKeys(false)?.forEach { slot ->
                val itemSection = kitItemsSection.getConfigurationSection(slot)
                if (itemSection != null) {
                    val item = loadItemData(itemSection, kitName, true)
                    val itemMeta = item?.itemMeta
                    if (itemMeta != null) {
                        setCustomValue(itemMeta, plugin, "edit_kit_item", true)
                        item.itemMeta = itemMeta
                        when (slot.toInt()) {
                            in 9..18 -> slot.toInt() - 9
                            in 0..8 -> slot.toInt() + 9
                            else -> return@forEach
                        }.let { inventory[it] = item }
                    }
                }
            }
            // go back button
            val goBack = ItemStack(Material.BARRIER)
            val goBackMeta: ItemMeta = goBack.itemMeta
            goBackMeta.displayName("<gray>Go Back".asMini())
            setCustomValue(goBackMeta, plugin, "676F5F6261636B5F627574746F6E", "go_back_button")
            setCustomValue(goBackMeta, plugin, "kit_name", kitName)
            setCustomValue(goBackMeta, plugin, "menu", "kit_editor")
            goBack.itemMeta = goBackMeta
            inventory[8] = goBack
            kitConfig.save(config)
            source.openInventory(inventory)
        } else {
            source.error("You must be a player to use this command!")
        }
    }
    fun loadItemData(itemSelector: ConfigurationSection?, kitName: String, gui: Boolean): ItemStack? {
        val itemName = itemSelector?.getString("name")?.asMini()
        val itemType = itemSelector?.getString("item")?.let { Material.getMaterial(it) }
        val itemAmount = itemSelector?.getInt("amount")
        val itemMetaModel = itemSelector?.getInt("meta.model")
        val itemMetaDurability = itemSelector?.getInt("meta.durability")
        val itemMetaUnbreakable = itemSelector?.getBoolean("meta.unbreakable")
        val itemMetaItemFlags = itemSelector?.getStringList("meta.itemFlags")?.map { ItemFlag.valueOf(it) }
        val enchantments = itemSelector?.getConfigurationSection("enchants")

        val itemStack = itemType?.let { ItemStack(it, itemAmount ?: 0) }
        val itemMeta: ItemMeta = itemStack?.itemMeta ?: return null
        itemMeta.lore(if(gui) {
            val line = "<gray>------------------<reset>".asMini()
            val toplore = "<dark_purple>Drag an item onto me".asMini()
            val bottomlore = "<dark_purple>to change me completely!".asMini()
            val lore = itemSelector.getStringList("lore").map { it.asMini() }
            lore.plus(line).plus(toplore).plus(bottomlore)
        } else {
            itemSelector.getStringList("lore").map { it.asMini() }
        })
        itemMeta.displayName(itemName)
        itemMeta.setCustomModelData(itemMetaModel)
        if (itemMeta is Damageable) {
            if (itemMetaDurability != null) {
                itemMeta.damage = itemMetaDurability
            }
        }
        if (itemMetaUnbreakable != null) {
            itemMeta.isUnbreakable = itemMetaUnbreakable
        }
        itemMetaItemFlags?.forEach { itemMeta.addItemFlags(it) }
        enchantments?.let { getEnchantments(it, itemMeta) }
        setCustomValue(itemMeta, plugin, "kit_name", kitName)
        itemStack.itemMeta = itemMeta

        return itemStack
    }

    // delete kit
    fun deleteKit(source: CommandSender, kitName: String) {
        val kitConfig = YamlConfiguration.loadConfiguration(config)
        if (source is Player) {
            if (kitConfig.contains("kit.$kitName")) {
                kitConfig.set("kit.$kitName", null)
                kitConfig.save(config)
                source.message("Kit $kitName has been deleted!")
            } else {
                source.error("Kit $kitName does not exist!")
            }
        } else {
            source.error("You must be a player to use this command!")
        }
    }
}

fun getEnchantments(enchantments: ConfigurationSection?, itemMeta: ItemMeta) {
    enchantments?.getKeys(false)?.forEach { enchantmentKey ->
        val namespacedKey = NamespacedKey.minecraft(enchantmentKey.lowercase(Locale.getDefault()))
        val enchantment = Enchantment.getByKey(namespacedKey)
        val level = enchantments.getInt(enchantmentKey)

        if (enchantment != null) {
            itemMeta.addEnchant(enchantment, level, true)
        }
    }
}