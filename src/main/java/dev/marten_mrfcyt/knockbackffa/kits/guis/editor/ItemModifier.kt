package dev.marten_mrfcyt.knockbackffa.kits.guis.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.asMini
import dev.marten_mrfcyt.knockbackffa.utils.message
import dev.marten_mrfcyt.knockbackffa.utils.setCustomValue
import lirand.api.extensions.inventory.set
import lirand.api.nbt.tagNbtData
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
        source.message(slot.toString())
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
                val item = KitModifier(plugin).loadItemData(kitItem, kitName)
                val itemMeta = item?.itemMeta
                if (itemMeta != null) {
                    setCustomValue(itemMeta, plugin, "slot", slot)
                    setCustomValue(itemMeta, plugin, "edit_kit_item", true)
                    item.itemMeta = itemMeta
                }
                inventory[13] = item
            }
            // modifiers
            // Is place block
            val isPlaceBlock = ItemStack(Material.GRASS_BLOCK)
            val isPlaceBlockMeta: ItemMeta = isPlaceBlock.itemMeta
            if(kitConfig.getBoolean("kit.$kitName.items.$slot.modifiers.placeBlock", false)){
                isPlaceBlockMeta.addEnchant(Enchantment.UNBREAKING, 1, true)
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
            inventory[0] = isPlaceBlock

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
            source.sendMessage("You must be a player to use this command!")
        }
    }
 }