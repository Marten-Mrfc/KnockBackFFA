package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.guis.editor.KitModifier
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import dev.marten_mrfcyt.knockbackffa.utils.message
import dev.marten_mrfcyt.knockbackffa.utils.setCustomValue
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File

fun loadKit(plugin: KnockBackFFA, source: Player) {
    val config = File("${plugin.dataFolder}/kits.yml")
    val kitConfig = YamlConfiguration.loadConfiguration(config)
    val playerData = PlayerData(plugin).getPlayerData(source.uniqueId)
    val kit = playerData.getString("kit")
    if(kit != null) {
        // clear whole inventory
        source.inventory.clear()
        // load kit
        val itemsSection = kitConfig.getConfigurationSection("kit.$kit.items")
        itemsSection?.getKeys(false)?.forEach { slot ->
            val itemSection = itemsSection.getConfigurationSection(slot)
            KitModifier(plugin).loadItemData(itemSection, kit)?.let { item ->
                val itemMeta = item.itemMeta
                val modifiers = itemSection?.getConfigurationSection("modifiers")
                for (modifier in modifiers?.getKeys(false) ?: emptySet()) {
                    setCustomValue(itemMeta, plugin, "modify", modifier)
                }
                source.message("Loaded item: $itemMeta")
                item.itemMeta = itemMeta
                source.inventory.setItem(slot.toInt(), item)
            }
        }
    } else {
        playerData.set("kit", "default")
        PlayerData(plugin).savePlayerData(source.uniqueId, playerData)
        loadKit(plugin, source)
    }
}