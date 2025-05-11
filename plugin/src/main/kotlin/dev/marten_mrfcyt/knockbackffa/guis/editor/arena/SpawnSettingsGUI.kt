package dev.marten_mrfcyt.knockbackffa.guis.editor.arena

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.arena.utils.ArenaModel
import dev.marten_mrfcyt.knockbackffa.arena.utils.ArenaSetting
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
import mlib.api.gui.GuiSize
import mlib.api.gui.types.StandardGui
import mlib.api.utilities.asMini
import mlib.api.utilities.sendMini
import org.bukkit.Material
import org.bukkit.entity.Player

class SpawnSettingsGUI(private val plugin: KnockBackFFA, private val player: Player, private val arenaName: String) {
    private val gui: StandardGui = StandardGui(translate("arena.spawn_settings.title", "arena_name" to arenaName).asMini(), GuiSize.ROW_FIVE)
    private var arena: ArenaModel? = plugin.arenaHandler.loadArenaByName(arenaName)

    init {
        if (arena == null) {
            player.sendMini(translate("arena.settings.not_found", "arena_name" to arenaName))
        }
        
        if (arena?.spawnRegion == null) {
            player.sendMini(translate("arena.spawn_settings.no_spawn_region", "arena_name" to arenaName))
        }
        
        setupGui()
        gui.open(player)
    }
    
    private fun setupGui() {
        gui.fill(Material.BLACK_STAINED_GLASS_PANE) {}
        
        setupToggleSettings()
        
        // Go back button
        gui.item(Material.ARROW) {
            name(translate("arena.settings.back").asMini())
            slots(40)
            onClick { event ->
                event.isCancelled = true
                ArenaSettingsGUI(plugin, player, arenaName)
            }
        }
    }
    
    private fun setupToggleSettings() {
        // Dynamically create settings from enum values
        val settings = ArenaSetting.Spawn.values
        
        // Calculate slots based on the number of settings
        // Starting at slot 10 (top row + 1)
        settings.forEachIndexed { index, setting ->
            // Skip any null settings to prevent NullPointerException
            if (setting == null) {
                debug("Null setting found at index $index in Spawn settings")
                return@forEachIndexed
            }
            
            val slot = 10 + index
            setupToggleSetting(
                setting.icon,
                setting.key,
                setting.displayName,
                setting.description,
                slot
            )
        }
    }
    
    private fun setupToggleSetting(material: Material, key: String, title: String, description: String, slot: Int) {
        val arena = this.arena ?: return

        // Find the setting but handle potential nulls safely
        val setting = ArenaSetting.Spawn.values.find { it?.key == key }
        
        val currentValue = try {
            if (setting == null) {
                debug("Setting with key $key not found in Spawn settings")
                false
            } else {
                ArenaSetting.getValue(arena.settings, setting)
            }
        } catch (e: Exception) {
            debug("Error getting setting value for $key: ${e.message}")
            false  // Default to false on error
        }

        val enabledColor = if (currentValue) "<green>" else "<red>"
        val statusText = if (currentValue) translate("arena.settings.enabled") else translate("arena.settings.disabled")

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
        plugin.arenaHandler.updateArenaSetting(arenaName, key, !currentValue, player, true)
        this.arena = plugin.arenaHandler.loadArenaByName(arenaName) // Reload arena after updating
    }
    
    private fun debug(message: String) {
        mlib.api.utilities.debug(plugin, "[SpawnSettingsGUI] $message")
    }
}
