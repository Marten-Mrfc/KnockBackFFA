package dev.marten_mrfcyt.knockbackffa.kits.guis

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.guis.editor.ItemModifier
import dev.marten_mrfcyt.knockbackffa.kits.guis.editor.KitModifier
import dev.marten_mrfcyt.knockbackffa.kits.guis.editor.KitSelector
import dev.marten_mrfcyt.knockbackffa.utils.*
import io.papermc.paper.event.player.AsyncChatEvent
import lirand.api.extensions.inventory.customModel
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import java.io.File
import java.util.*

class GuiListener(private val plugin: KnockBackFFA) : Listener {
    private val editKitMap = HashMap<UUID, Pair<Boolean, ItemMeta?>>()
    val config = File("${plugin.dataFolder}/kits.yml")
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val clickedItem = event.currentItem ?: return
        val source = event.whoClicked as? Player ?: return
        val clickedInventory = event.clickedInventory
        if (clickedItem.itemMeta != null) {
        println(clickedItem.toString())
            when {
                checkCustomValue(clickedItem.itemMeta, plugin, "is_draggable", false) -> {
                    event.isCancelled = true
                }

                clickedInventory != null && clickedInventory == event.whoClicked.openInventory.topInventory -> {
                    when {
                        checkCustomValue(
                            clickedItem.itemMeta,
                            plugin,
                            "6B69745F646973706C61795F6974656D",
                            "kit_display_item_check"
                        ) -> {
                            val kitConfig = YamlConfiguration.loadConfiguration(config)
                            event.isCancelled = true
                            val itemInHand = event.cursor
                            kitConfig.save(config)
                            val kitName = getCustomValue(clickedItem.itemMeta, plugin, "kit_name") as String
                            if (itemInHand.type != Material.AIR) {
                                val itemType = event.cursor.type.name
                                val itemEnchants = event.cursor.enchantments.map { it.key.key to it.value }.toMap()
                                val name = kitConfig.getString("kit.$kitName.show.DisplayName")
                                val kitLore = kitConfig.getString("kit.$kitName.show.Lore")
                                kitConfig.getConfigurationSection("kit.$kitName.show")?.apply {
                                    set("DisplayItem.item", itemType)
                                    kitConfig.save(config)
                                    kitConfig.save(config)
                                    getConfigurationSection("DisplayItem")?.getConfigurationSection("enchants")
                                        ?.let { enchantsSection ->
                                            enchantsSection.getKeys(false).forEach { enchantsSection.set(it, null) }
                                            itemEnchants.forEach { (enchant, level) ->
                                                val enchantKey =
                                                    enchant.toString().split(":").last().uppercase(Locale.getDefault())
                                                enchantsSection.set(enchantKey, level)
                                            }
                                        }
                                    kitConfig.save(config)
                                    if (kitLore != null && name != null) {
                                        KitModifier(plugin).kitEditor(
                                            source,
                                            name.asMini(),
                                            kitLore.asMini(),
                                            kitName,
                                            false
                                        )
                                    }
                                } ?: source.error("Kit section could not be found: ")
                            }
                        }

                        checkCustomValue(
                            clickedItem.itemMeta,
                            plugin,
                            "6F70656E5F6B69745F656469746F72",
                            "open_kit_editor"
                        ) -> {
                            event.isCancelled = true
                            clickedItem.itemMeta?.let { itemMeta ->
                                val name = itemMeta.displayName()
                                val lore = itemMeta.lore()?.get(0)
                                val kitName = getCustomValue(itemMeta, plugin, "kit_name") as String
                                if (name != null && lore != null) {
                                    KitModifier(plugin).kitEditor(source, name, lore, kitName, false)
                                } else {
                                    source.error("Name or lore could not be found.")
                                }
                            }
                        }

                        checkCustomValue(
                            clickedItem.itemMeta,
                            plugin,
                            "6B69745F646973706C61795F6E616D655F65646974",
                            "kit_display_name_edit"
                        ) -> {
                            event.isCancelled = true
                            source.closeInventory()
                            source.message("Please enter the new display name in the chat.")
                            editKitMap[source.uniqueId] = Pair(true, clickedItem.itemMeta)
                        }

                        checkCustomValue(
                            clickedItem.itemMeta,
                            plugin,
                            "6B69745F646973706C61795F6C6F72655F65646974",
                            "kit_display_lore_edit"
                        ) -> {
                            event.isCancelled = true
                            source.closeInventory()
                            source.message("Please enter the new lore in the chat.")
                            editKitMap[source.uniqueId] = Pair(true, clickedItem.itemMeta)
                        }

                        checkCustomValue(
                            clickedItem.itemMeta,
                            plugin,
                            "6B69745F646973706C61795F6974656D5F65646974",
                            "kit_display_item_edit"
                        ) -> {
                            event.isCancelled = true
                            val kitName = getCustomValue(clickedItem.itemMeta, plugin, "kit_name") as String
                            KitModifier(plugin).editKitGUI(source, kitName)
                        }

                        checkCustomValue(
                            clickedItem.itemMeta,
                            plugin,
                            "676F5F6261636B5F627574746F6E",
                            "go_back_button"
                        ) -> {
                            event.isCancelled = true
                            val menu = getCustomValue(clickedItem.itemMeta, plugin, "menu") as String
                            when (menu) {
                                "kit_selector" -> {
                                    KitSelector(plugin).editKitSelector(source)
                                }

                                "kit_editor" -> {
                                    getCustomValue(clickedItem.itemMeta, plugin, "kit_name")?.let { kitName ->
                                        if (kitName is String) {
                                            val displayName = getKitAttribute(kitName, "show.DisplayName")
                                            val lore = getKitAttribute(kitName, "show.Lore")
                                            KitModifier(plugin).kitEditor(source, displayName, lore, kitName, false)
                                        } else {
                                            source.error("Kit name could not be found or isn't a String")
                                        }
                                    }
                                }

                                "edit_kit_gui" -> {
                                    getCustomValue(clickedItem.itemMeta, plugin, "kit_name")?.let { kitName ->
                                        if (kitName is String) {
                                            KitModifier(plugin).editKitGUI(source, kitName)
                                        } else {
                                            source.error("Kit name could not be found or isn't a String")
                                        }
                                    }
                                }
                            }
                        }

                        checkCustomValue(
                            clickedItem.itemMeta,
                            plugin,
                            "edit_kit_item",
                            true
                        ) -> {
                            val kitConfig = YamlConfiguration.loadConfiguration(config)
                            event.isCancelled = true
                            val kitName = getCustomValue(clickedItem.itemMeta, plugin, "kit_name") as String
                            val itemInHand = event.cursor
                            val editItem: Boolean
                            val slot = if (getCustomValue(clickedItem.itemMeta, plugin, "slot") != null) {
                                editItem = true
                                when (val slotValue = getCustomValue(clickedItem.itemMeta, plugin, "slot") as Int) {
                                    in 9..18 -> slotValue - 9
                                    in 0..8 -> slotValue + 9
                                    else -> slotValue
                                }
                            } else {
                                editItem = false
                                when (event.slot) {
                                    in 9..18 -> event.slot - 9
                                    in 0..8 -> event.slot + 9
                                    else -> event.slot
                                }
                            }
                            if (itemInHand.type != Material.AIR) {
                                val itemMeta = event.cursor.itemMeta
                                with(kitConfig) {
                                    set("kit.$kitName.items.$slot.name", itemMeta.displayName()?.notMini())
                                    set("kit.$kitName.items.$slot.lore", itemMeta.lore()?.map { it.notMini() })
                                    set("kit.$kitName.items.$slot.item", event.cursor.type.name)
                                    set("kit.$kitName.items.$slot.amount", event.cursor.amount)
                                    val itemEnchants =
                                        itemMeta.enchants.mapKeys { it.key.key.key.uppercase(Locale.getDefault()) }
                                    set("kit.$kitName.items.$slot.enchants", itemEnchants)
                                    set("kit.$kitName.items.$slot.meta.model", itemMeta.customModel)
                                    if (itemMeta is Damageable) {
                                        set("kit.$kitName.items.$slot.meta.durability", itemMeta.damage)
                                    }
                                    set("kit.$kitName.items.$slot.meta.unbreakable", itemMeta.isUnbreakable)
                                    set(
                                        "kit.$kitName.items.$slot.meta.itemFlags",
                                        itemMeta.itemFlags.map { it.name }.toList()
                                    )
                                    save(config)
                                    if (editItem) {
                                        when (slot) {
                                            in 9..18 -> slot - 9
                                            in 0..8 -> slot + 9
                                        }
                                        ItemModifier(plugin).editKitItem(source, kitName, slot)
                                    } else {
                                        KitModifier(plugin).editKitGUI(source, kitName)
                                    }
                                }
                            } else {
                                ItemModifier(plugin).editKitItem(source, kitName, slot)
                            }
                        }

                        checkCustomValue(
                            clickedItem.itemMeta,
                            plugin,
                            "6D6F646966696572",
                            "modifier"
                        ) -> {
                            event.isCancelled = true
                            source.message("Modifier clicked")
                            val modify = getCustomValue(clickedItem.itemMeta, plugin, "modify") as String
                            when(modify) {
                                "placeBlock" -> {
                                    source.message("Place block modifier clicked")
                                    val kitName = getCustomValue(clickedItem.itemMeta, plugin, "kit_name") as String
                                    val slot = getCustomValue(clickedItem.itemMeta, plugin, "slot") as Int
                                    val kitConfig = YamlConfiguration.loadConfiguration(config)
                                    val isPlaceBlock = kitConfig.getBoolean("kit.$kitName.items.$slot.modifiers.placeBlock", false)
                                    kitConfig.set("kit.$kitName.items.$slot.modifiers.placeBlock", !isPlaceBlock)
                                    kitConfig.save(config)
                                    ItemModifier(plugin).editKitItem(source, kitName, slot)
                                }
                            }
                        }
                        checkCustomValue(
                            clickedItem.itemMeta,
                            plugin,
                            "64656C6574655F6974656D",
                            "delete_item"
                        ) -> {
                            event.isCancelled = true
                            val kitName = getCustomValue(clickedItem.itemMeta, plugin, "kit_name") as String
                            val slot = getCustomValue(clickedItem.itemMeta, plugin, "slot") as Int
                            val kitConfig = YamlConfiguration.loadConfiguration(config)
                            kitConfig.set("kit.$kitName.items.$slot", null)
                            kitConfig.save(config)
                            KitModifier(plugin).editKitGUI(source, kitName)
                        }
                    }
                }
            }
        }
    }
        @EventHandler
        fun onPlayerChat(event: AsyncChatEvent) {
            val message = event.message()
            val source = event.player
            val (isEditingDisplay, itemMeta) = editKitMap[source.uniqueId] ?: Pair(false, null)
            if (isEditingDisplay) {
                if (itemMeta?.let {
                        checkCustomValue(
                            it,
                            plugin,
                            "6B69745F646973706C61795F6E616D655F65646974",
                            "kit_display_name_edit"
                        )
                    } == true) {
                    val kitName = getCustomValue(itemMeta, plugin, "kit_name") as String?
                    if (kitName != null) {
                        handleKitEdit(source, kitName, "show.DisplayName", message)
                        event.isCancelled = true
                    }
                } else if (itemMeta?.let {
                        checkCustomValue(
                            it,
                            plugin,
                            "6B69745F646973706C61795F6C6F72655F65646974",
                            "kit_display_lore_edit"
                        )
                    } == true) {
                    val kitName = getCustomValue(itemMeta, plugin, "kit_name") as String?
                    if (kitName != null) {
                        handleKitEdit(source, kitName, "show.Lore", message)
                        event.isCancelled = true
                    }
                }
            }
        }
    private fun handleKitEdit(source: Player, kitName: String?, key: String, message: Component) {
        if (kitName != null) {
            val kitConfig = YamlConfiguration.loadConfiguration(config)
            kitConfig.set("kit.$kitName.$key", message.notMiniText())
            kitConfig.save(config)
            editKitMap[source.uniqueId] = Pair(false, null)
            val name = getKitAttribute(kitName, "show.DisplayName")
            val lore = getKitAttribute(kitName, "show.Lore")
            Bukkit.getScheduler().runTask(plugin, Runnable {
                KitModifier(plugin).kitEditor(source, name, lore, kitName, false)
            })
        }
    }
    private fun getKitAttribute(kitName: String, attribute: String): Component {
        val kitConfig = YamlConfiguration.loadConfiguration(config)
        return kitConfig.get("kit.$kitName.$attribute").toString().asMini()
    }
}