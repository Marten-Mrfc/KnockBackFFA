package dev.marten_mrfcyt.knockbackffa.kits.guis.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.handlers.loadKit
import dev.marten_mrfcyt.knockbackffa.utils.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File
import kotlin.math.ceil

class KitSelector(private val plugin: KnockBackFFA) {
    fun editKitSelector(source: CommandSender) {
        if (source is Player) {
            val config = File("${plugin.dataFolder}/kits.yml")
            val kitConfig = YamlConfiguration.loadConfiguration(config)
            val kits = kitConfig.getConfigurationSection("kit")?.getKeys(false)
            val kitCount = kits?.size ?: 0
            val inventorySize = ceil((kitCount + 1) / 9.0).toInt() * 9
            val inventory =
                Bukkit.createInventory(null, inventorySize, "<gray>Please select or create kit</gray>".asMini())

            kits?.forEach { kit ->
                val item = loadKitGUI(kitConfig, kit)
                val meta = item.itemMeta ?: return@forEach
                setCustomValue(meta, plugin, "6F70656E5F6B69745F656469746F72", "open_kit_editor")
                setCustomValue(meta, plugin, "kit_name", kit)
                item.itemMeta = meta
                inventory.addItem(item)
            }

            source.openInventory(inventory)
        } else {
            source.error("You must be a player to use this command!")
        }
    }

    fun kitSelector(source: CommandSender) {
        if (source is Player) {
            val config = File("${plugin.dataFolder}/kits.yml")
            val kitConfig = YamlConfiguration.loadConfiguration(config)
            val kits = kitConfig.getConfigurationSection("kit")?.getKeys(false)
            val kitCount = kits?.size ?: 0
            val inventorySize = ceil((kitCount + 1) / 9.0).toInt() * 9
            val inventory =
                Bukkit.createInventory(null, inventorySize, "<gray>Please select a kit</gray>".asMini())

            kits?.forEach { kit ->
                val item = loadKitGUI(kitConfig, kit)
                val meta = item.itemMeta ?: return@forEach
                setCustomValue(meta, plugin, "73656C6563742D6B6974", "select-kit")
                setCustomValue(meta, plugin, "kit_name", kit)
                item.itemMeta = meta
                inventory.addItem(item)
            }

            source.openInventory(inventory)
        } else {
            source.error("You must be a player to use this command!")
        }
    }

    fun setKit(kit: String, source: Player) {
        val playerData = plugin.playerData
        val playerDataConfig = playerData.getPlayerData(source.uniqueId)
        playerDataConfig.set("kit", kit)
        playerData.savePlayerData(source.uniqueId, playerDataConfig)
        loadKit(plugin, source)
        source.message("You have selected the $kit kit!")
    }

    private fun loadKitGUI(kitConfig: YamlConfiguration, kit: String): ItemStack {
        val kitSection = kitConfig.getConfigurationSection("kit.$kit.show") ?: return ItemStack(Material.BARRIER)
        val displayName = "<!italic>${kitSection.getString("DisplayName")}".asMini()
        val lore = kitSection.getString("Lore")?.asMini() ?: return ItemStack(Material.BARRIER)
        val displayItemMaterial = Material.getMaterial(kitSection.getString("DisplayItem.item") ?: return ItemStack(Material.BARRIER))
        val item = ItemStack(displayItemMaterial ?: Material.BARRIER)
        val meta = item.itemMeta ?: return ItemStack(Material.BARRIER)
        meta.displayName(displayName)
        meta.lore(listOf(lore))
        item.itemMeta = meta
        return item
    }
}