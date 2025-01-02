package dev.marten_mrfcyt.knockbackffa.kits.guis

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.handlers.ModifyHandler
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
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import java.io.File
import java.util.*
import kotlin.collections.set
import kotlin.text.get
import kotlin.text.set

class GuiListener(private val plugin: KnockBackFFA) : Listener {
    private val editKitMap = HashMap<UUID, Pair<Boolean, ItemMeta?>>()
    private val config = File("${plugin.dataFolder}/kits.yml")
    private val kitConfig = YamlConfiguration.loadConfiguration(config)

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val clickedItem = event.currentItem ?: return
        val player = event.whoClicked as? Player ?: return
        val clickedInventory = event.clickedInventory ?: return

        if (clickedItem.itemMeta == null) return

        when {
            checkCustomValue(clickedItem.itemMeta, plugin, "is_draggable", false) -> {
                event.isCancelled = true
            }

            clickedInventory == event.whoClicked.openInventory.topInventory -> {
                handleTopInventoryClick(event, player, clickedItem)
            }
        }
    }

    private fun handleTopInventoryClick(
        event: InventoryClickEvent,
        player: Player,
        clickedItem: ItemStack
    ) {
        event.isCancelled = true
        when {
            checkCustomValue(
                clickedItem.itemMeta, plugin, "is_modifier", true
            ) -> {
                print("Modifier")
                handleModifier(player, clickedItem.itemMeta)
            }
            checkCustomValue(clickedItem.itemMeta, plugin, "6B69745F646973706C61795F6974656D", "kit_display_item_check") -> {
                handleKitDisplayItemChange(event, player)
            }
            checkCustomValue(clickedItem.itemMeta, plugin, "6F70656E5F6B69745F656469746F72", "open_kit_editor") -> {
                openKitEditor(player, clickedItem.itemMeta)
            }
            checkCustomValue(clickedItem.itemMeta, plugin, "6B69745F646973706C61795F6E616D655F65646974", "kit_display_name_edit") -> {
                initiateKitEdit(player, clickedItem.itemMeta, "Please enter the new display name in the chat.")
            }
            checkCustomValue(clickedItem.itemMeta, plugin, "6B69745F646973706C61795F6C6F72655F65646974", "kit_display_lore_edit") -> {
                initiateKitEdit(player, clickedItem.itemMeta, "Please enter the new lore in the chat.")
            }
            checkCustomValue(clickedItem.itemMeta, plugin, "6B69745F646973706C61795F6974656D5F65646974", "kit_display_item_edit") -> {
                val kitName = getCustomValue(clickedItem.itemMeta, plugin, "kit_name") as String
                KitModifier(plugin).editKitGUI(player, kitName)
            }
            checkCustomValue(clickedItem.itemMeta, plugin, "676F5F6261636B5F627574746F6E", "go_back_button") -> {
                handleGoBackButton(player, clickedItem.itemMeta)
            }
            checkCustomValue(clickedItem.itemMeta, plugin, "edit_kit_item", true) -> {
                handleEditKitItem(event, player)
            }
            checkCustomValue(clickedItem.itemMeta, plugin, "64656C6574655F6974656D", "delete_item") -> {
                handleDeleteItem(player, clickedItem.itemMeta)
            }
            checkCustomValue(clickedItem.itemMeta, plugin, "73656C6563742D6B6974", "select-kit") -> {
                val kitName = getCustomValue(clickedItem.itemMeta, plugin, "kit_name") as String
                KitSelector(plugin).setKit(kitName, player)
                player.closeInventory()
            }
        }
    }

    private fun handleKitDisplayItemChange(event: InventoryClickEvent, player: Player) {
        val itemInHand = event.cursor
        val kitName = event.currentItem?.itemMeta?.let { getCustomValue(it, plugin, "kit_name") } as String

        if (itemInHand.type != Material.AIR) {
            updateKitDisplayItem(kitName, itemInHand)
            openKitEditor(player, kitName)
        }
    }

    private fun updateKitDisplayItem(kitName: String, newItem: ItemStack) {
        kitConfig.apply {
            set("kit.$kitName.show.DisplayItem.item", newItem.type.name)
            set(
                "kit.$kitName.show.DisplayItem.enchants",
                newItem.enchantments.mapKeys { it.key.key.toString().split(":").last().uppercase() })
            save(config)
        }
    }

    private fun openKitEditor(player: Player, kitName: String) {
        val displayName = getKitAttribute(kitName, "show.DisplayName")
        val lore = getKitAttribute(kitName, "show.Lore")
        KitModifier(plugin).kitEditor(player, displayName, lore, kitName, false)
    }

    private fun openKitEditor(player: Player, itemMeta: ItemMeta) {
        val name = itemMeta.displayName()
        val lore = itemMeta.lore()?.get(0)
        val kitName = getCustomValue(itemMeta, plugin, "kit_name") as String
        if (name != null && lore != null) {
            KitModifier(plugin).kitEditor(player, name, lore, kitName, false)
        } else {
            player.error("Name or lore could not be found.")
        }
    }

    private fun initiateKitEdit(player: Player, itemMeta: ItemMeta, message: String) {
        player.closeInventory()
        player.message(message)
        editKitMap[player.uniqueId] = Pair(true, itemMeta)
    }

    private fun handleGoBackButton(player: Player, itemMeta: ItemMeta) {
        val menu = getCustomValue(itemMeta, plugin, "menu") as String
        when (menu) {
            "kit_selector" -> KitSelector(plugin).editKitSelector(player)
            "kit_editor" -> {
                getCustomValue(itemMeta, plugin, "kit_name")?.let { kitName ->
                    if (kitName is String) {
                        openKitEditor(player, kitName)
                    } else {
                        player.error("Kit name could not be found or isn't a String")
                    }
                }
            }

            "edit_kit_gui" -> {
                getCustomValue(itemMeta, plugin, "kit_name")?.let { kitName ->
                    if (kitName is String) {
                        KitModifier(plugin).editKitGUI(player, kitName)
                    } else {
                        player.error("Kit name could not be found or isn't a String")
                    }
                }
            }
        }
    }

    private fun handleEditKitItem(event: InventoryClickEvent, player: Player) {
        val kitName = event.currentItem?.itemMeta?.let { getCustomValue(it, plugin, "kit_name") } as String
        val itemInHand = event.cursor
        val editItem = event.currentItem?.itemMeta?.let { getCustomValue(it, plugin, "slot") } != null
        val slot = calculateSlot(event, editItem)

        if (itemInHand.type != Material.AIR) {
            updateKitItem(kitName, slot, itemInHand)
            if (editItem) {
                ItemModifier(plugin).editKitItem(player, kitName, slot)
            } else {
                KitModifier(plugin).editKitGUI(player, kitName)
            }
        } else {
            ItemModifier(plugin).editKitItem(player, kitName, slot)
        }
    }

    private fun calculateSlot(event: InventoryClickEvent, editItem: Boolean): Int {
        return if (editItem) {
            val slotValue = event.currentItem?.itemMeta?.let { getCustomValue(it, plugin, "slot") } as Int
            when (slotValue) {
                in 9..18 -> slotValue - 9
                in 0..8 -> slotValue + 9
                else -> slotValue
            }
        } else {
            when (event.slot) {
                in 9..18 -> event.slot - 9
                in 0..8 -> event.slot + 9
                else -> event.slot
            }
        }
    }

    private fun updateKitItem(kitName: String, slot: Int, item: ItemStack) {
        val itemMeta = item.itemMeta
        kitConfig.apply {
            set("kit.$kitName.items.$slot.name", itemMeta.displayName()?.notMini())
            set("kit.$kitName.items.$slot.lore", itemMeta.lore()?.map { it.notMini() })
            set("kit.$kitName.items.$slot.item", item.type.name)
            set("kit.$kitName.items.$slot.amount", item.amount)
            set(
                "kit.$kitName.items.$slot.enchants",
                itemMeta.enchants.mapKeys { it.key.key.key.uppercase(Locale.getDefault()) })
            set("kit.$kitName.items.$slot.meta.model", itemMeta.customModel)
            if (itemMeta is Damageable) {
                set("kit.$kitName.items.$slot.meta.durability", itemMeta.damage)
            }
            set("kit.$kitName.items.$slot.meta.unbreakable", itemMeta.isUnbreakable)
            set("kit.$kitName.items.$slot.meta.itemFlags", itemMeta.itemFlags.map { it.name }.toList())
            save(config)
        }
    }

    private fun handleModifier(player: Player, itemMeta: ItemMeta) {
        val modify = getCustomValue(itemMeta, plugin, "modifier") as String
        val kitName = getCustomValue(itemMeta, plugin, "kit_name") as String
        val slot = getCustomValue(itemMeta, plugin, "slot") as Int
        val currentValue = kitConfig.getBoolean("kit.$kitName.items.$slot.modifiers.$modify", false)
        val modifyObject = ModifyHandler().getModifyObjects().find { it.id == modify }
        if (modifyObject != null && modifyObject.args.isNotEmpty() && !currentValue) {
            player.closeInventory()
            player.message("Please provide the following values for the modifier:")
            modifyObject.args.forEach { arg ->
                player.message("${arg.first}:")
            }
            editKitMap[player.uniqueId] = Pair(true, itemMeta)
            return
        }
        if (modifyObject != null && modifyObject.args.isNotEmpty() && currentValue) {
            modifyObject.args.forEach { arg ->
                kitConfig.set("kit.$kitName.items.$slot.modifiers.${arg.first}", null)
            }
        }
        kitConfig.set("kit.$kitName.items.$slot.modifiers.$modify", !currentValue)
        kitConfig.save(config)
        ItemModifier(plugin).editKitItem(player, kitName, slot)
    }

    private fun handleDeleteItem(player: Player, itemMeta: ItemMeta) {
        val kitName = getCustomValue(itemMeta, plugin, "kit_name") as String
        val slot = getCustomValue(itemMeta, plugin, "slot") as Int
        kitConfig.set("kit.$kitName.items.$slot", null)
        kitConfig.save(config)
        KitModifier(plugin).editKitGUI(player, kitName)
    }

    @EventHandler
    fun onPlayerChat(event: AsyncChatEvent) {
        val player = event.player
        val (isEditing, itemMeta) = editKitMap[player.uniqueId] ?: return
        if (!isEditing || itemMeta == null) return
        event.isCancelled = true

        val modify = getCustomValue(itemMeta, plugin, "modifier") as String
        val kitName = getCustomValue(itemMeta, plugin, "kit_name") as String
        val slot = getCustomValue(itemMeta, plugin, "slot") as Int

        val modifyObject = ModifyHandler().getModifyObjects().find { it.id == modify }
        modifyObject?.let {
            if (it.args.isNotEmpty()) {
                val args = it.args.associate { arg ->
                    arg.first to when (arg.second) {
                        Int::class.java -> event.message().notMiniText().toIntOrNull() ?: 0
                        else -> event.message().notMiniText()
                    }
                }
                kitConfig.set("kit.$kitName.items.$slot.modifiers.$modify", true)
                args.forEach { (key, value) -> kitConfig.set("kit.$kitName.items.$slot.modifiers.$key", value) }
                kitConfig.save(config)
                player.message("Modifier arguments saved successfully!")
                editKitMap[player.uniqueId] = Pair(false, null)
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    ItemModifier(plugin).editKitItem(player, kitName, slot)
                })
                return
            }
        }

        when {
            checkCustomValue(itemMeta, plugin, "6b69745f646973706c61795f6e616d655f65646974", "kit_display_name_edit") -> {
                handleKitAttributeEdit(player, itemMeta, "show.DisplayName", event.message())
            }
            checkCustomValue(itemMeta, plugin, "6b69745f646973706c61795f6c6f72655f65646974", "kit_display_lore_edit") -> {
                handleKitAttributeEdit(player, itemMeta, "show.Lore", event.message())
            }
            checkCustomValue(itemMeta, plugin, "656469745f6b69745f6974656d5f446973706c61794e616d65", "edit_kit_item_DisplayName") -> {
                handleKitItemAttributeEdit(player, itemMeta, "name", event.message())
            }
            checkCustomValue(itemMeta, plugin, "656469745f6b69745f6974656d5f6c6f7265", "edit_kit_item_lore") -> {
                handleKitItemAttributeEdit(player, itemMeta, "lore", event.message())
            }
        }
    }

    private fun handleKitAttributeEdit(player: Player, itemMeta: ItemMeta, attribute: String, newValue: Component) {
        val kitName = getCustomValue(itemMeta, plugin, "kit_name") as String? ?: return
        val newValueString = newValue.notMiniText()
        kitConfig.set("kit.$kitName.$attribute", newValueString)
        kitConfig.save(config)
        editKitMap[player.uniqueId] = Pair(false, null)

        Bukkit.getScheduler().runTask(plugin, Runnable {
            openKitEditor(player, kitName)
        })
    }

    private fun handleKitItemAttributeEdit(player: Player, itemMeta: ItemMeta, attribute: String, newValue: Component) {
        val kitName = getCustomValue(itemMeta, plugin, "kit_name") as String? ?: return
        val slot = getCustomValue(itemMeta, plugin, "slot") as Int? ?: return
        kitConfig.set(
            "kit.$kitName.items.$slot.$attribute",
            if (attribute == "lore") listOf(newValue.notMiniText()) else newValue.notMiniText()
        )
        kitConfig.save(config)
        player.message("Successfully updated the $attribute of the kit item!")
        editKitMap[player.uniqueId] = Pair(false, null)

        Bukkit.getScheduler().runTask(plugin, Runnable {
            ItemModifier(plugin).editKitItem(player, kitName, slot)
        })
    }

    private fun getKitAttribute(kitName: String, attribute: String): Component {
        return kitConfig.get("kit.$kitName.$attribute").toString().asMini()
    }
}