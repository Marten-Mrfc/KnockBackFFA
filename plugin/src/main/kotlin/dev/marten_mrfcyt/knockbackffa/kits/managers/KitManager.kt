package dev.marten_mrfcyt.knockbackffa.kits.managers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.bypassMode
import dev.marten_mrfcyt.knockbackffa.kits.models.Kit
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import mlib.api.utilities.debug
import mlib.api.utilities.message
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.UUID
import java.util.logging.Level

class KitManager(private val plugin: KnockBackFFA) {
    private val configFile: File = File(plugin.dataFolder, "kits.yml")
    internal val cachedKits = mutableMapOf<String, Kit>()
    init {
        if (!configFile.exists()) {
            plugin.saveResource("kits.yml", false)
            debug(plugin, "Created default kits.yml file")
        }
        loadAllKits()
    }

    private fun loadAllKits() {
        debug(plugin, "Loading all kits from configuration")
        val kitConfig = YamlConfiguration.loadConfiguration(configFile)
        val kitSection = kitConfig.getConfigurationSection("kit")
        
        if (kitSection == null) {
            mlib.api.utilities.debug(plugin, "No kits section found in configuration")
            return
        }

        val kitNames = kitSection.getKeys(false)
        debug(plugin, "Found ${kitNames.size} kits in configuration")
        
        kitNames.forEach { kitName ->
            try {
                debug(plugin, "Loading kit: $kitName")
                val kit = Kit.load(kitName)
                if (kit != null) {
                    cachedKits[kitName] = kit
                    debug(plugin, "Successfully loaded kit: $kitName")
                } else {
                    debug(plugin, "Failed to load kit: $kitName (returned null)")
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, TranslationManager.translate("kit.load.failed", "name" to kitName, "error" to e.message.toString()), e)
                debug(plugin, "Error loading kit $kitName: ${e.message}")
            }
        }
    }

    fun getKit(kitName: String): Kit {
        return cachedKits[kitName] ?: try {
            val kit = Kit.load(kitName)
            if (kit != null) {
                cachedKits[kitName] = kit
            }
            kit
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, TranslationManager.translate("kit.load.failed", "name" to kitName, "error" to e.message.toString()), e)
            null
        } ?: throw IllegalArgumentException(TranslationManager.translate("kit.not_found", "name" to kitName))
    }

    fun getAllKitNames(): List<String> {
        val kitConfig = YamlConfiguration.loadConfiguration(configFile)
        return kitConfig.getConfigurationSection("kit")?.getKeys(false)?.toList() ?: emptyList()
    }

    private val kitCooldowns = mutableMapOf<UUID, Long>()
    private val kitCooldownSeconds = plugin.config.getInt("kit-cooldown", 30)

    fun applyKit(player: Player, kitName: String, force: Boolean): Boolean {
        val now = System.currentTimeMillis()
        val playerId = player.uniqueId
        val lastUse = kitCooldowns[playerId] ?: 0L
        val remainingCooldown = ((lastUse + (kitCooldownSeconds * 1000) - now) / 1000).toInt()

        if (remainingCooldown > 0 && !force && !bypassMode.getOrDefault(player, false)) {
            player.message(TranslationManager.translate("kit.cooldown", "seconds" to remainingCooldown))
            return false
        }

        getKit(kitName).applyTo(player)
        kitCooldowns[playerId] = now
        return true
    }

    fun createKit(kitName: String, displayName: String, description: String): Kit {
        val kit = Kit(
            name = kitName,
            displayName = displayName,
            description = description
        )

        kit.save()

        cachedKits[kitName] = kit

        return kit
    }    fun deleteKit(kitName: String): Boolean {
        mlib.api.utilities.debug(plugin, "Attempting to delete kit: $kitName")
        try {
            val kitConfig = YamlConfiguration.loadConfiguration(configFile)
            kitConfig.set("kit.$kitName", null)
            kitConfig.save(configFile)
            cachedKits.remove(kitName)
            mlib.api.utilities.debug(plugin, "Successfully deleted kit: $kitName")
            return true
        } catch (e: Exception) {
            plugin.logger.severe(TranslationManager.translate("kit.delete.failed", "name" to kitName, "error" to e.message.toString()))
            mlib.api.utilities.debug(plugin, "Error deleting kit $kitName: ${e.message}")
            return false
        }
    }

    internal fun updateKitCache(kitName: String, kit: Kit) {
        mlib.api.utilities.debug(plugin, "Updating kit cache for: $kitName")
        cachedKits[kitName] = kit
    }    fun reloadKits() {
        mlib.api.utilities.debug(plugin, "Reloading all kits")
        cachedKits.clear()
        if (!configFile.exists()) {
            plugin.saveResource("kits.yml", false)
            mlib.api.utilities.debug(plugin, "Created default kits.yml file during reload")
        }
        loadAllKits()
        plugin.logger.info(TranslationManager.translate("kit.reload.success", "count" to cachedKits.size))
        mlib.api.utilities.debug(plugin, "Successfully reloaded ${cachedKits.size} kits")
    }

    fun reloadKit(kitName: String): Boolean {
        mlib.api.utilities.debug(plugin, "Reloading specific kit: $kitName")
        try {
            val kit = Kit.load(kitName)
            if (kit != null) {
                cachedKits[kitName] = kit
                plugin.logger.info(TranslationManager.translate("kit.reload.single.success", "name" to kitName))
                mlib.api.utilities.debug(plugin, "Successfully reloaded kit: $kitName")
                return true
            }
            mlib.api.utilities.debug(plugin, "Failed to reload kit $kitName: Kit not found or invalid")
            return false
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, TranslationManager.translate("kit.load.failed",
                "name" to kitName, "error" to e.message.toString()), e)
            return false
        }
    }
}