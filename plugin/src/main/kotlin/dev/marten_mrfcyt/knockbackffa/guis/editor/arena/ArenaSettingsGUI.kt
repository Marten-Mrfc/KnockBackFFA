package dev.marten_mrfcyt.knockbackffa.guis.editor.arena

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.arena.utils.ArenaModel
import dev.marten_mrfcyt.knockbackffa.arena.utils.ArenaSetting
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
import mlib.api.gui.GuiSize
import mlib.api.gui.types.StandardGui
import mlib.api.utilities.asMini
import mlib.api.utilities.debug
import mlib.api.utilities.sendMini
import org.bukkit.Material
import org.bukkit.entity.Player

class ArenaSettingsGUI(private val plugin: KnockBackFFA, private val player: Player, private val arenaName: String) {
    private val gui: StandardGui = StandardGui(translate("arena.settings.title", "arena_name" to arenaName).asMini(), GuiSize.ROW_SIX)
    private var arena: ArenaModel? = plugin.arenaHandler.loadArenaByName(arenaName)

    init {
        if (arena == null) {
            player.sendMini(translate("arena.settings.not_found", "arena_name" to arenaName))
        }
        
        setupGui()
        gui.open(player)
    }
    
    private fun setupGui() {
        gui.fill(Material.BLACK_STAINED_GLASS_PANE) {}
        
        setupGlobalSettings()
        setupSpawnRegionButton()
        
        // Go back button
        gui.item(Material.BARRIER) {
            name(translate("arena.settings.close").asMini())
            slots(49)
            onClick { event ->
                event.isCancelled = true
                player.closeInventory()
            }
        }
    }
    
    private fun setupGlobalSettings() {
        // Add title
        gui.item(Material.GOLD_BLOCK) {
            name("<gold>Global Arena Settings".asMini())
            slots(4)
        }
        
        // Dynamically create global settings
        val settings = ArenaSetting.Global.values
        
        // Calculate slots based on the number of settings
        // Two rows of settings, 7 per row
        settings.forEachIndexed { index, setting ->
            // Skip any null settings to prevent NullPointerException

            val row = index / 7
            val col = index % 7
            val slot = 10 + (row * 9) + col
            
            setupToggleSetting(
                setting.icon,
                setting.key,
                setting.displayName,
                setting.description,
                slot
            )
        }
    }
    
    private fun setupSpawnRegionButton() {
        // Button to access spawn region settings
        gui.item(Material.BEACON) {
            name("<aqua>Spawn Region Settings".asMini())
            description(listOf(
                "<gray>Configure settings for the spawn region".asMini(),
                "<gray>Click to open".asMini()
            ))
            slots(40)
            onClick { event ->
                event.isCancelled = true
                SpawnSettingsGUI(plugin, player, arenaName)
            }
        }
    }
    
    private fun setupToggleSetting(material: Material, key: String, title: String, description: String, slot: Int) {
        val arena = this.arena ?: return

        // Find the setting but handle potential nulls safely
        val setting = ArenaSetting.Global.values.find { it?.key == key }
        
        val currentValue = try {
            if (setting == null) {
                debug("Setting with key $key not found in Global settings")
                false
            } else {
                ArenaSetting.getValue(arena.settings, setting)
            }
        } catch (e: Exception) {
            debug("Error getting setting value for $key: ${e.message}")
            false  // Default to false on error
        }
        
        val statusText = if (currentValue) translate("arena.settings.enabled") else translate("arena.settings.disabled")
        val enabledColor = if (currentValue) "<green>" else "<red>"
        
        gui.item(material) {
            name(title.asMini())
            description(listOf(
                description.asMini(),
                "".asMini(),
                "$enabledColor$statusText".asMini(),
                "<gray>Click to toggle".asMini()
            ))
            slots(slot)
            onClick { event ->
                event.isCancelled = true
                toggleSetting(key)
                setupGui() // Refresh the GUI
            }
        }
    }
    
    private fun toggleSetting(key: String) {
        val arena = this.arena ?: return
        val currentValue = arena.settings[key] as? Boolean ?: false
        plugin.arenaHandler.updateArenaSetting(arenaName, key, !currentValue, player, false)
        this.arena = plugin.arenaHandler.loadArenaByName(arenaName) // Reload arena after updating
    }
}
