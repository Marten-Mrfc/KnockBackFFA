package dev.marten_mrfcyt.knockbackffa.guis.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.*
import mlib.api.gui.Gui
import mlib.api.gui.GuiSize
import mlib.api.utilities.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.io.File

class EditKit(private val plugin: KnockBackFFA) {
    private val config = File("${plugin.dataFolder}/kits.yml")
    private val kitConfig = YamlConfiguration.loadConfiguration(config)

    fun kitEditor(source: Player, name: Component, lore: Component, kitName: String, new: Boolean = true) {
        if (new && kitConfig.contains("kit.$kitName")) {
            source.error("Kit with this name already exists!")
            return
        }

        if (new) {
            kitConfig.set("kit.$kitName.show.DisplayName", name.notMini())
            kitConfig.set("kit.$kitName.show.Lore", lore.notMini())
            kitConfig.set("kit.$kitName.show.DisplayItem.item", Material.STICK.name)
            kitConfig.set("kit.$kitName.show.DisplayItem.enchants", mapOf("KNOCKBACK" to 2))
            kitConfig.save(config)
        }

        val inventoryTitle = "<gray>Editing:</gray><white> ".asMini().append(name)
        val gui = Gui(inventoryTitle, GuiSize.ROW_TWO).apply {
            item(Material.GRAY_STAINED_GLASS_PANE) {
                name(Component.text(""))
                description(listOf(Component.text("")))
                slots(9, 10, 11, 12, 14, 15, 16, 17)
                onClick { event -> event.isCancelled = true }
            }
            item(Material.CHEST) {
                name("<gray>Edit Items".asMini())
                description(listOf())
                slots(2)
                onClick { event -> editItems(event, kitName) }
            }
            item(Material.NAME_TAG) {
                name("<gray>Edit Display Name".asMini())
                description(listOf())
                slots(0)
                onClick { event -> editDisplayName(event) }
            }
            item(Material.BOOK) {
                name("<gray>Edit Lore".asMini())
                description(listOf())
                slots(1)
                onClick { event -> editLore(event) }
            }
            item(Material.BARRIER) {
                name("<gray>Go Back".asMini())
                description(listOf())
                slots(8)
                onClick { event -> goBack(event) }
            }
        }

        val kitSection = kitConfig.getConfigurationSection("kit.$kitName.show") ?: run {
            source.error("Kit $kitName is missing from kits.yml")
            return
        }

        val displayName = kitSection.getString("DisplayName")?.asMini()
        val kitLore = kitSection.getString("Lore")?.asMini()
        val displayItemMaterial = Material.getMaterial(kitSection.getString("DisplayItem.item") ?: return)

        if (displayName == null || kitLore == null || displayItemMaterial == null) {
            source.error("Kit $kitName is missing required fields in kits.yml")
            return
        }

        val enchantments = kitSection.getConfigurationSection("DisplayItem.enchants")
        val modifiedKit = ItemStack(displayItemMaterial)
        val modifiedKitMeta: ItemMeta = modifiedKit.itemMeta
        modifiedKitMeta.displayName(displayName)
        modifiedKitMeta.lore(listOf(kitLore, "<gray>------------------<reset>".asMini(), "<dark_purple>Drag an item onto me".asMini(), "<dark_purple>To change my DisplayIcon!".asMini()))
        enchantments?.getKeys(false)?.forEach { enchantmentKey ->
            val enchantment = Enchantment.getByName(enchantmentKey)
            val level = enchantments.getInt(enchantmentKey)
            if (enchantment != null) {
                modifiedKitMeta.addEnchant(enchantment, level, true)
            }
        }
        setCustomValue(modifiedKitMeta, plugin, "type", "kit_display_item_check")
        setCustomValue(modifiedKitMeta, plugin, "kit_name", kitName)
        modifiedKit.itemMeta = modifiedKitMeta
        gui.item(modifiedKit.type) {
            name(modifiedKitMeta.displayName() ?: "".asMini())
            description(modifiedKitMeta.lore()?.map { it } ?: listOf())
            slots(13)
            onClick { event -> event.isCancelled = true }
        }

        gui.open(source)
    }

    private fun editItems(event: InventoryClickEvent, kitName: String) {
        val player = event.whoClicked as? Player ?: return
        EditKitItemSelector(plugin, player, kitName).initialize()
    }

    private fun editDisplayName(event: InventoryClickEvent) {
        val item = event.currentItem ?: return
        val player = event.whoClicked as? Player ?: return
        val kitName = getCustomValue(item.itemMeta, plugin, "kit_name") ?: return
        player.sendMessage("Editing display name for kit: $kitName")
    }

    private fun editLore(event: InventoryClickEvent) {
        val item = event.currentItem ?: return
        val player = event.whoClicked as? Player ?: return
        val kitName = getCustomValue(item.itemMeta, plugin, "kit_name") ?: return
        player.sendMessage("Editing lore for kit: $kitName")
    }

    private fun goBack(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        EditKitSelector(plugin, player)
    }
}