package dev.marten_mrfcyt.knockbackffa.kits.managers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.models.Kit
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
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
        }
        loadAllKits()
    }

    private fun loadAllKits() {
        val kitConfig = YamlConfiguration.loadConfiguration(configFile)
        val kitSection = kitConfig.getConfigurationSection("kit") ?: return

        kitSection.getKeys(false).forEach { kitName ->
            try {
                val kit = Kit.load(kitName)
                if (kit != null) {
                    cachedKits[kitName] = kit
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, TranslationManager.translate("kit.load.failed", "name" to kitName, "error" to e.message.toString()), e)
            }
        }
    }

    fun getKit(kitName: String): Kit{
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
    private val kitCooldownSeconds = 30

    fun applyKit(player: Player, kitName: String): Boolean {
        val now = System.currentTimeMillis()
        val playerId = player.uniqueId

        val lastUse = kitCooldowns[playerId] ?: 0L
        val remainingCooldown = ((lastUse + (kitCooldownSeconds * 1000) - now) / 1000).toInt()

        if (remainingCooldown > 0) {
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
    }

    fun deleteKit(kitName: String): Boolean {
        try {
            val kitConfig = YamlConfiguration.loadConfiguration(configFile)
            kitConfig.set("kit.$kitName", null)
            kitConfig.save(configFile)
            cachedKits.remove(kitName)
            return true
        } catch (e: Exception) {
            plugin.logger.severe(TranslationManager.translate("kit.delete.failed", "name" to kitName, "error" to e.message.toString()))
            return false
        }
    }

    internal fun updateKitCache(kitName: String, kit: Kit) {
        cachedKits[kitName] = kit
    }

    fun reloadKits() {
        cachedKits.clear()
        if (!configFile.exists()) {
            plugin.saveResource("kits.yml", false)
        }
        loadAllKits()
        plugin.logger.info(TranslationManager.translate("kit.reload.success", "count" to cachedKits.size))
    }
}