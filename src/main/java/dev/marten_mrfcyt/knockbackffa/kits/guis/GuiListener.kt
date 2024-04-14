package dev.marten_mrfcyt.knockbackffa.kits.guis

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.guis.editor.KitModifier
import dev.marten_mrfcyt.knockbackffa.utils.checkCustomValue
import dev.marten_mrfcyt.knockbackffa.utils.getCustomValue
import dev.marten_mrfcyt.knockbackffa.utils.message
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import java.io.File
import java.util.*

class GuiListener(private val plugin: KnockBackFFA) : Listener {
    val config = File("${plugin.dataFolder}/kits.yml")
    private val kitConfig = YamlConfiguration.loadConfiguration(config)

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val clickedInventory = event.clickedInventory
        val clickedItem = event.currentItem ?: return
        if (checkCustomValue(clickedItem.itemMeta, plugin, "is_draggable", false)) {
            event.isCancelled = true
        } else {
            if (event.whoClicked is Player) {
                val source = event.whoClicked as Player
                source.message("You clicked on $clickedItem")
            }
            // Check if the clicked inventory is the correct one
            if (clickedInventory != null && clickedInventory == event.whoClicked.openInventory.topInventory) {
                // Check if the clicked item is a kit display item
                if (checkCustomValue(
                        clickedItem.itemMeta,
                        plugin,
                        "6B69745F646973706C61795F6974656D",
                        "kit_display_item_check"
                    )
                ) {
                    println("Custom value check passed for kit_display_item")
                    event.isCancelled = true
                    if (event.whoClicked is Player) {
                        val source = event.whoClicked as Player
                        source.message("Custom value check passed for kit_display_item")
                        val itemInHand = event.cursor
                        // Check if the item in hand is not null and not air
                        if (itemInHand.type != Material.AIR) {
                            println("Item in hand is not null and not air")
                            source.message("Item in hand is not null and not air")
                            val itemType = itemInHand.type.name
                            println("Item type: $itemType")
                            val itemEnchants = itemInHand.enchantments.map { it.key.key to it.value }.toMap()

                            // Get the kit section for the clicked item
                            val kitName = getCustomValue(clickedItem.itemMeta, plugin, "kit_display_item") as String
                            val kitSection = kitConfig.getConfigurationSection("kit.$kitName")

                            // Set the new item and enchantments
                            println("Current DisplayItem: ${kitSection?.getConfigurationSection("DisplayItem")?.getString("item")}")
                            kitSection?.getConfigurationSection("DisplayItem")?.set("item", itemType)
                            kitSection?.getConfigurationSection("DisplayItem")?.getConfigurationSection("enchants")
                                ?.let { enchantsSection ->
                                    enchantsSection.getKeys(false)
                                        .forEach { enchantsSection.set(it, null) } // Clear existing enchantments
                                    itemEnchants.forEach { (enchant, level) ->
                                        val enchantKey =
                                            enchant.toString().split(":").last().uppercase(Locale.getDefault()) // Get only the key part after the namespace and convert it to uppercase
                                        enchantsSection.set(
                                            enchantKey,
                                            level
                                        )
                                    }
                                }
                            println("New item and enchantments set: $itemType, $itemEnchants")
                            source.message("New item and enchantments set")
                        }
                        kitConfig.save(config)
                        println("Kit config saved")
                        source.message("Kit config saved")
                    }
                }
                if (checkCustomValue(
                        clickedItem.itemMeta,
                        plugin,
                        "6F70656E5F6B69745F656469746F72",
                        "open_kit_editor"
                    )
                ) {
                    event.isCancelled = true
                    if (event.whoClicked is Player) {
                        val source = event.whoClicked as Player
                        val name = clickedItem.itemMeta.displayName()
                        val lore = clickedItem.itemMeta.lore()?.get(0)
                        if (name != null) {
                            if (lore != null) {
                                KitModifier(plugin).openNewKitGUI(source, name, lore, false)
                            }
                        }
                    }
                }
            }
        }
    }
}