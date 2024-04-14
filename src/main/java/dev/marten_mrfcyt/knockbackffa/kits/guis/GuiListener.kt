package dev.marten_mrfcyt.knockbackffa.kits.guis

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.guis.editor.KitModifier
import dev.marten_mrfcyt.knockbackffa.utils.asMini
import dev.marten_mrfcyt.knockbackffa.utils.checkCustomValue
import dev.marten_mrfcyt.knockbackffa.utils.error
import dev.marten_mrfcyt.knockbackffa.utils.getCustomValue
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
        val clickedItem = event.currentItem ?: return
        val source = event.whoClicked as? Player ?: return
        val clickedInventory = event.clickedInventory

        when {
            checkCustomValue(clickedItem.itemMeta, plugin, "is_draggable", false) -> {
                event.isCancelled = true
            }
            clickedInventory != null && clickedInventory == event.whoClicked.openInventory.topInventory -> {
                when {
                    checkCustomValue(clickedItem.itemMeta, plugin, "6B69745F646973706C61795F6974656D", "kit_display_item_check") -> {
                        event.isCancelled = true
                        val itemInHand = event.cursor
                        val kitName = getCustomValue(clickedItem.itemMeta, plugin, "kit_display_item") as String
                        val name = kitConfig.getConfigurationSection("kit.$kitName")?.getString("DisplayName")
                        val kitLore = kitConfig.getConfigurationSection("kit.$kitName")?.getString("Lore")
                        if (itemInHand.type != Material.AIR) {
                            val itemType = itemInHand.type.name
                            val itemEnchants = itemInHand.enchantments.map { it.key.key to it.value }.toMap()

                            kitConfig.getConfigurationSection("kit.$kitName")?.apply {
                                getConfigurationSection("DisplayItem")?.set("item", itemType)
                                getConfigurationSection("DisplayItem")?.getConfigurationSection("enchants")?.let { enchantsSection ->
                                    enchantsSection.getKeys(false).forEach { enchantsSection.set(it, null) }
                                    itemEnchants.forEach { (enchant, level) ->
                                        val enchantKey = enchant.toString().split(":").last().uppercase(Locale.getDefault())
                                        enchantsSection.set(enchantKey, level)
                                    }
                                }
                            } ?: source.error("Kit section could not be found.")
                        }
                        kitConfig.save(config)
                        if (kitLore != null && name != null) {
                            KitModifier(plugin).openNewKitGUI(source, name.asMini(), kitLore.asMini(), false)
                        }

                    }
                    checkCustomValue(clickedItem.itemMeta, plugin, "6F70656E5F6B69745F656469746F72", "open_kit_editor") -> {
                        event.isCancelled = true
                        clickedItem.itemMeta?.let { itemMeta ->
                            val name = itemMeta.displayName()
                            val lore = itemMeta.lore()?.get(0)
                            if (name != null && lore != null) {
                                KitModifier(plugin).openNewKitGUI(source, name, lore, false)
                            } else {
                                source.error("Name or lore could not be found.")
                            }
                        }
                    }
                }
            }
        }
    }
}