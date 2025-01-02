package dev.marten_mrfcyt.knockbackffa.handlers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.guis.editor.KitModifier
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import dev.marten_mrfcyt.knockbackffa.utils.setCustomValue
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.logging.Level
import kotlin.collections.set
import kotlin.text.clear

fun loadKit(plugin: KnockBackFFA, source: Player) {
    val config = File("${plugin.dataFolder}/kits.yml")
    val kitConfig = YamlConfiguration.loadConfiguration(config)
    val playerData = plugin.playerData.getPlayerData(source.uniqueId)
    val kit = playerData.getString("kit")
    if (kit != null) {
        plugin.logger.log(Level.INFO, "Loading kit: $kit for player: ${source.name}")
        // clear whole inventory
        source.inventory.clear()
        // load kit
        val itemsSection = kitConfig.getConfigurationSection("kit.$kit.items")
        itemsSection?.getKeys(false)?.forEach { slot ->
            val itemSection = itemsSection.getConfigurationSection(slot)
            KitModifier(plugin).loadItemData(itemSection, kit, false)?.let { item ->
                val itemMeta = item.itemMeta
                val modifiers = itemSection?.getConfigurationSection("modifiers")
                for (modifier in modifiers?.getKeys(false) ?: emptySet()) {
                    val modifiersList = modifiers?.getKeys(false)?.toList() ?: emptyList()
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
        plugin.playerData.savePlayerData(source.uniqueId, playerData)
        loadKit(plugin, source)
    }
}