package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.guis.editor.EditKitItemSelector
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import mlib.api.utilities.setCustomValue
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.logging.Level

fun loadKit(plugin: KnockBackFFA, source: Player) {
    val config = File(plugin.dataFolder, "kits.yml")
    val kitConfig = YamlConfiguration.loadConfiguration(config)
    val playerData = PlayerData.getInstance(plugin).getPlayerData(source.uniqueId)
    val kit = playerData.getString("kit")
    if (kit != null) {
        // clear whole inventory
        source.inventory.clear()
        // load kit
        val itemsSection = kitConfig.getConfigurationSection("kit.$kit.items")
        itemsSection?.getKeys(false)?.forEach { slot ->
            val itemSection = itemsSection.getConfigurationSection(slot) ?: return@forEach
            EditKitItemSelector(plugin, source, kit).loadItemData(itemSection, kit, false)?.let { item ->
                val itemMeta = item.itemMeta
                val modifiers = itemSection.getConfigurationSection("modifiers")
                modifiers?.getKeys(false)?.let { keys ->
                    val modifiersList = keys.toList()
                    setCustomValue(itemMeta, plugin, "modify", modifiersList)
                }
                setCustomValue(itemMeta, plugin, "kit_name", kit)
                setCustomValue(itemMeta, plugin, "slot", slot.toInt())
                item.itemMeta = itemMeta
                source.inventory.setItem(slot.toInt(), item)
            }
        }
    } else {
        plugin.logger.log(Level.INFO, "No kit found for player: ${source.name}. Setting default kit.")
        playerData.set("kit", "default")
        PlayerData.getInstance(plugin).savePlayerData(source.uniqueId, playerData)
        loadKit(plugin, source)
    }
}

fun listKits(plugin: KnockBackFFA): List<String> {
    val config = File(plugin.dataFolder, "kits.yml")
    val kitConfig = YamlConfiguration.loadConfiguration(config)
    return kitConfig.getConfigurationSection("kit")?.getKeys(false)?.toList() ?: emptyList()
}