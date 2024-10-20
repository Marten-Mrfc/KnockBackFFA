package dev.marten_mrfcyt.knockbackffa.kits.guis.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.asMini
import dev.marten_mrfcyt.knockbackffa.utils.error
import dev.marten_mrfcyt.knockbackffa.utils.isBelowVersion
import dev.marten_mrfcyt.knockbackffa.utils.setCustomValue
import lirand.api.extensions.inventory.set
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.io.File

class ItemModifier(private val plugin: KnockBackFFA) {
    val config = File("${plugin.dataFolder}/kits.yml")
    fun editKitItem(source: CommandSender, kitName: String, slot: Int) {
        val kitConfig = YamlConfiguration.loadConfiguration(config)
        if (source is Player) {
            val inventorySize = 18
            val edittext = "<gray>Editing slot:</gray><white> $slot".asMini()
            val inventory = Bukkit.createInventory(null, inventorySize, edittext)
            for (i in 0..17) {
                if (i < 8 || i > 8) {
                    val glassPane = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
                    val glassMeta: ItemMeta = glassPane.itemMeta
                    glassMeta.displayName("".asMini())
                    setCustomValue(glassMeta, plugin, "is_draggable", false)
                    glassPane.itemMeta = glassMeta
                    inventory[i] = glassPane
                }
            }
            // show item
            val kitItem = kitConfig.getConfigurationSection("kit.$kitName.items.$slot")

            if (kitItem != null) {
                val item = KitModifier(plugin).loadItemData(kitItem, kitName, true)
                val itemMeta = item?.itemMeta
                if (itemMeta != null) {
                    setCustomValue(itemMeta, plugin, "slot", slot)
                    setCustomValue(itemMeta, plugin, "edit_kit_item", true)
                    item.itemMeta = itemMeta
                }
                inventory[13] = item
            }
            // edit DisplayName
            val editDisplayName = ItemStack(Material.NAME_TAG)
            val editDisplayNameMeta: ItemMeta = editDisplayName.itemMeta
            editDisplayNameMeta.displayName("<dark_gray>Edit Display Name".asMini())
            setCustomValue(editDisplayNameMeta, plugin, "656469745F6B69745F6974656D5F446973706C61794E616D65", "edit_kit_item_DisplayName")
            setCustomValue(editDisplayNameMeta, plugin, "kit_name", kitName)
            setCustomValue(editDisplayNameMeta, plugin, "slot", slot)
            editDisplayName.itemMeta = editDisplayNameMeta
            inventory[0] = editDisplayName
            // edit Lore
            val editLore = ItemStack(Material.PAPER)
            val editLoreMeta: ItemMeta = editLore.itemMeta
            editLoreMeta.displayName("<dark_gray>Edit Lore".asMini())
            setCustomValue(editLoreMeta, plugin, "656469745F6B69745F6974656D5F6C6F7265", "edit_kit_item_lore")
            setCustomValue(editLoreMeta, plugin, "kit_name", kitName)
            setCustomValue(editLoreMeta, plugin, "slot", slot)
            editLore.itemMeta = editLoreMeta
            inventory[1] = editLore
            // modifiers
            // Is place block
            val isPlaceBlock = ItemStack(Material.GRASS_BLOCK)
            val isPlaceBlockMeta: ItemMeta = isPlaceBlock.itemMeta
            if(kitConfig.getBoolean("kit.$kitName.items.$slot.modifiers.placeBlock", false)){
                if (isBelowVersion("1.20.5")) {
                    try {
                        // Use reflection to safely access the DURABILITY enchantment
                        val durabilityEnchantment = Enchantment::class.java.getField("DURABILITY").get(null) as? Enchantment
                        durabilityEnchantment?.let {
                            isPlaceBlockMeta.addEnchant(it, 1, true)
                        } ?: run {
                            // Handle the case where DURABILITY enchantment is not available
                            println("DURABILITY enchantment is not available.")
                        }
                    } catch (e: NoSuchFieldException) {
                        // If DURABILITY is not found, log or handle as needed
                        println("DURABILITY enchantment not found: ${e.message}")
                    } catch (e: Exception) {
                        // Handle other potential exceptions
                        println("Error adding DURABILITY enchantment: ${e.message}")
                    }
                } else {
                    isPlaceBlockMeta.addEnchant(Enchantment.UNBREAKING, 1, true)
                }

                isPlaceBlockMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                isPlaceBlockMeta.lore(listOf("<gray>When this item is placed, it will be removed.".asMini(), "<green>Enabled".asMini()))
            } else {
                isPlaceBlockMeta.lore(listOf("<gray>When this item is placed, it will be removed.".asMini(), "<red>Disabled".asMini()))
            }
            isPlaceBlockMeta.displayName("<dark_gray>Is Place Block".asMini())
            setCustomValue(isPlaceBlockMeta, plugin, "6D6F646966696572", "modifier")
            setCustomValue(isPlaceBlockMeta, plugin, "modify", "placeBlock")
            setCustomValue(isPlaceBlockMeta, plugin, "kit_name", kitName)
            setCustomValue(isPlaceBlockMeta, plugin, "slot", slot)
            isPlaceBlock.itemMeta = isPlaceBlockMeta
            inventory[9] = isPlaceBlock
            // Is infinite
            val isInfinite = ItemStack(Material.BEDROCK)
            val isInfiniteMeta: ItemMeta = isInfinite.itemMeta
            if(kitConfig.getBoolean("kit.$kitName.items.$slot.modifiers.infinite", false)){
                if(kitConfig.getBoolean("kit.$kitName.items.$slot.modifiers.onKill", false)){
                    if (isBelowVersion("1.20.5")) {
                        try {
                            // Use reflection to safely access the DURABILITY enchantment
                            val durabilityEnchantment = Enchantment::class.java.getField("DURABILITY").get(null) as? Enchantment
                            durabilityEnchantment?.let {
                                isInfiniteMeta.addEnchant(it, 1, true)
                            } ?: run {
                                // Handle the case where DURABILITY enchantment is not available
                                println("DURABILITY enchantment is not available.")
                            }
                        } catch (e: NoSuchFieldException) {
                            // If DURABILITY is not found, log or handle as needed
                            println("DURABILITY enchantment not found: ${e.message}")
                        } catch (e: Exception) {
                            // Handle other potential exceptions
                            println("Error adding DURABILITY enchantment: ${e.message}")
                        }
                    } else {
                        isInfiniteMeta.addEnchant(Enchantment.UNBREAKING, 1, true)
                    }
                    isInfiniteMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    isInfiniteMeta.lore(listOf("<gray>When you kill a player, get the item again".asMini(), "<green>Enabled".asMini()))
                } else {
                    isInfiniteMeta.lore(listOf("<gray>When you kill a player, get the item again".asMini(), "<red>Disabled".asMini()))
                }
                isInfiniteMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                isInfiniteMeta.lore(listOf("<gray>If Item < 64: set item to 64.".asMini(), "<green>Enabled".asMini()))
            } else {
                isInfiniteMeta.lore(listOf("<gray>If Item < 64: set item to 64.".asMini(), "<red>Disabled".asMini()))
            }
            isInfiniteMeta.displayName("<dark_gray>Is Infinite".asMini())
            setCustomValue(isInfiniteMeta, plugin, "6D6F646966696572", "modifier")
            setCustomValue(isInfiniteMeta, plugin, "modify", "infinite")
            setCustomValue(isInfiniteMeta, plugin, "kit_name", kitName)
            setCustomValue(isInfiniteMeta, plugin, "slot", slot)
            isInfinite.itemMeta = isInfiniteMeta
            inventory[10] = isInfinite
            // On kill
            val onKill = ItemStack(Material.DIAMOND_SWORD)
            val onKillMeta: ItemMeta = onKill.itemMeta
            if(kitConfig.getBoolean("kit.$kitName.items.$slot.modifiers.onKill", false)){
                if (isBelowVersion("1.20.5")) {
                    try {
                        // Use reflection to safely access the DURABILITY enchantment
                        val durabilityEnchantment = Enchantment::class.java.getField("DURABILITY").get(null) as? Enchantment
                        durabilityEnchantment?.let {
                            onKillMeta.addEnchant(it, 1, true)
                        } ?: run {
                            // Handle the case where DURABILITY enchantment is not available
                            println("DURABILITY enchantment is not available.")
                        }
                    } catch (e: NoSuchFieldException) {
                        // If DURABILITY is not found, log or handle as needed
                        println("DURABILITY enchantment not found: ${e.message}")
                    } catch (e: Exception) {
                        // Handle other potential exceptions
                        println("Error adding DURABILITY enchantment: ${e.message}")
                    }
                } else {
                    onKillMeta.addEnchant(Enchantment.UNBREAKING, 1, true)
                }
                onKillMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                onKillMeta.lore(listOf("<gray>When you kill a player, get the item again".asMini(), "<green>Enabled".asMini()))
            } else {
                onKillMeta.lore(listOf("<gray>When you kill a player, get the item again".asMini(), "<red>Disabled".asMini()))
            }
            onKillMeta.displayName("<dark_gray>On Kill".asMini())
            setCustomValue(onKillMeta, plugin, "6D6F646966696572", "modifier")
            setCustomValue(onKillMeta, plugin, "modify", "onKill")
            setCustomValue(onKillMeta, plugin, "kit_name", kitName)
            setCustomValue(onKillMeta, plugin, "slot", slot)
            onKill.itemMeta = onKillMeta
            inventory[11] = onKill
            // delete button
            val deleteButton = ItemStack(Material.RED_CONCRETE)
            val deleteButtonMeta: ItemMeta = deleteButton.itemMeta
            deleteButtonMeta.displayName("<red>Delete Item".asMini())
            setCustomValue(deleteButtonMeta, plugin, "64656C6574655F6974656D", "delete_item")
            setCustomValue(deleteButtonMeta, plugin, "kit_name", kitName)
            setCustomValue(deleteButtonMeta, plugin, "slot", slot)
            deleteButton.itemMeta = deleteButtonMeta
            inventory[7] = deleteButton
            // go back button
            val goBack = ItemStack(Material.BARRIER)
            val goBackMeta: ItemMeta = goBack.itemMeta
            goBackMeta.displayName("<gray>Go Back".asMini())
            setCustomValue(goBackMeta, plugin, "kit_name", kitName)
            setCustomValue(goBackMeta, plugin, "676F5F6261636B5F627574746F6E", "go_back_button")
            setCustomValue(goBackMeta, plugin, "menu", "edit_kit_gui")
            goBack.itemMeta = goBackMeta
            inventory[8] = goBack
            kitConfig.save(config)
            source.openInventory(inventory)
        } else {
            source.error("You must be a player to use this command!")
        }
    }
 }