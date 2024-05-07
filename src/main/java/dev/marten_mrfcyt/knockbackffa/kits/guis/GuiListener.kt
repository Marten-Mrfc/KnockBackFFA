package dev.marten_mrfcyt.knockbackffa.kits.guis

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
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
    private val kitConfig = YamlConfiguration.loadConfiguration(config)
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val clickedItem = event.currentItem ?: return
        val source = event.whoClicked as? Player ?: return
        val clickedInventory = event.clickedInventory
        if (clickedItem.itemMeta != null) {

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
                            event.isCancelled = true
                            val itemInHand = event.cursor
                            val kitName = getCustomValue(clickedItem.itemMeta, plugin, "kit_name") as String
                            val name = kitConfig.getConfigurationSection("kit.$kitName")?.getString("DisplayName")
                            val kitLore = kitConfig.getConfigurationSection("kit.$kitName")?.getString("Lore")
                            if (itemInHand.type != Material.AIR) {
                                val itemType = itemInHand.type.name
                                val itemEnchants = itemInHand.enchantments.map { it.key.key to it.value }.toMap()

                                kitConfig.getConfigurationSection("kit.$kitName")?.apply {
                                    getConfigurationSection("DisplayItem")?.set("item", itemType)
                                    getConfigurationSection("DisplayItem")?.getConfigurationSection("enchants")
                                        ?.let { enchantsSection ->
                                            enchantsSection.getKeys(false).forEach { enchantsSection.set(it, null) }
                                            itemEnchants.forEach { (enchant, level) ->
                                                val enchantKey =
                                                    enchant.toString().split(":").last().uppercase(Locale.getDefault())
                                                enchantsSection.set(enchantKey, level)
                                            }
                                        }
                                } ?: source.error("Kit section could not be found.")
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
                                    KitSelector(plugin).kitSelector(source)
                                }
                                "kit_editor" -> {
                                    getCustomValue(clickedItem.itemMeta, plugin, "kit_name")?.let { kitName ->
                                        if (kitName is String) {
                                            val displayName = getKitAttribute(kitName, "DisplayName")
                                            val lore = getKitAttribute(kitName, "Lore")
                                            KitModifier(plugin).kitEditor(source, displayName, lore, kitName, false)
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
                            event.isCancelled = true
                            source.message("Editing slot ${event.slot}")
                            val kitName = getCustomValue(clickedItem.itemMeta, plugin, "kit_name") as String
                            // if it has item in hand set that item to the slot
                            val itemInHand = event.cursor
                            if (itemInHand.type != Material.AIR) {
                                val itemMeta = event.cursor.itemMeta
                                kitConfig.apply {
                                    set("kit.$kitName.items.${event.slot}", null)
                                    set("kit.$kitName.items.${event.slot}.name", itemMeta.displayName()?.notMini())
                                    set("kit.$kitName.items.${event.slot}.lore", itemMeta.lore()?.map { it.notMini() })
                                    set("kit.$kitName.items.${event.slot}.item", event.cursor.type.name)
                                    set("kit.$kitName.items.${event.slot}.amount", event.cursor.amount)
                                    set("kit.$kitName.items.${event.slot}.enchants", event.cursor.enchantments)
                                    set("kit.$kitName.items.${event.slot}.meta.model", itemMeta.customModel)
                                    if (itemMeta is Damageable) {
                                        set("kit.$kitName.items.${event.slot}.meta.durability", itemMeta.damage)
                                    }
                                    set("kit.$kitName.items.${event.slot}.meta.unbreakable", itemMeta.isUnbreakable)
                                    set("kit.$kitName.items.${event.slot}.meta.itemFlags", itemMeta.itemFlags.map { it.name }.toList())
                                    save(config)
                                }
                                source.message("Item set to slot ${event.slot} successfully.")
                            } else {
                                source.message("Please hold an item in your hand to set it to the slot.")
                            }
                        }
                    }
                }
            }
        }
        fun handleKitEdit(source: Player, kitName: String?, key: String, message: Component) {
            if (kitName != null) {
                kitConfig.set("kit.$kitName.$key", message.notMiniText())
                kitConfig.save(config)
                editKitMap[source.uniqueId] = Pair(false, null)
                val name = getKitAttribute(kitName, "DisplayName")
                val lore = getKitAttribute(kitName, "Lore")
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    KitModifier(plugin).kitEditor(source, name, lore, kitName, false)
                })
            }
        }

        @EventHandler
        fun onPlayerChat(event: AsyncChatEvent) {
            val message = event.message()

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
                        handleKitEdit(source, kitName, "DisplayName", message)
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
                        handleKitEdit(source, kitName, "Lore", message)
                        event.isCancelled = true
                    }
                }
            }
        }
    }
    private fun getKitAttribute(kitName: String, attribute: String): Component {
        return kitConfig.get("kit.$kitName.$attribute").toString().asMini()
    }
}