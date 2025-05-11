package dev.marten_mrfcyt.knockbackffa.arena

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.arena.utils.ArenaModel
import dev.marten_mrfcyt.knockbackffa.guis.editor.arena.ArenaSettingsGUI
import dev.marten_mrfcyt.knockbackffa.guis.editor.arena.SpawnSettingsGUI
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
import mlib.api.utilities.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.*

var currentArena: ArenaModel? = null

class ArenaHandler(private val plugin: KnockBackFFA) {
    private val arenaConfig: YamlConfiguration
    private val arenaCreationSessions = mutableMapOf<UUID, ArenaCreationSession>()

    init {
        val arenaFile = File("${plugin.dataFolder}/arena.yml")
        if (!arenaFile.exists()) {
            arenaFile.createNewFile()
            YamlConfiguration().save(arenaFile)
            debug("Arena file created at ${arenaFile.absolutePath}")
        }
        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile)
    }

    fun addArena(arena: ArenaModel, callback: (() -> Unit)? = null) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            debug("Adding arena: ${arena.name}")

            if (arena.spawnRegion != null) {
                arenaConfig.set("arenas.${arena.name}.spawnRegion.world", arena.spawnRegion.first.world?.name)
                arenaConfig.set("arenas.${arena.name}.spawnRegion.min.x", minOf(arena.spawnRegion.first.x, arena.spawnRegion.second.x))
                arenaConfig.set("arenas.${arena.name}.spawnRegion.min.y", minOf(arena.spawnRegion.first.y, arena.spawnRegion.second.y))
                arenaConfig.set("arenas.${arena.name}.spawnRegion.min.z", minOf(arena.spawnRegion.first.z, arena.spawnRegion.second.z))
                arenaConfig.set("arenas.${arena.name}.spawnRegion.max.x", maxOf(arena.spawnRegion.first.x, arena.spawnRegion.second.x))
                arenaConfig.set("arenas.${arena.name}.spawnRegion.max.y", maxOf(arena.spawnRegion.first.y, arena.spawnRegion.second.y))
                arenaConfig.set("arenas.${arena.name}.spawnRegion.max.z", maxOf(arena.spawnRegion.first.z, arena.spawnRegion.second.z))
            }

            arenaConfig.set("arenas.${arena.name}.spawnpoint.world", arena.spawnpoint.world?.name)
            arenaConfig.set("arenas.${arena.name}.spawnpoint.x", arena.spawnpoint.x)
            arenaConfig.set("arenas.${arena.name}.spawnpoint.y", arena.spawnpoint.y)
            arenaConfig.set("arenas.${arena.name}.spawnpoint.z", arena.spawnpoint.z)
            arenaConfig.set("arenas.${arena.name}.spawnpoint.yaw", arena.spawnpoint.yaw)
            arenaConfig.set("arenas.${arena.name}.spawnpoint.pitch", arena.spawnpoint.pitch)

            arenaConfig.set("arenas.${arena.name}.killBlock", arena.killBlock.name)

            arena.settings.forEach { (key, value) ->
                arenaConfig.set("arenas.${arena.name}.settings.$key", value)
            }
            arenaConfig.save(File("${plugin.dataFolder}/arena.yml"))
            debug("Arena ${arena.name} saved successfully")

            if (callback != null) {
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    callback()
                })
            }
        })
    }    
    
    fun removeArena(arena: ArenaModel) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            debug("Removing arena: ${arena.name}")
            arenaConfig.set("arenas.${arena.name}", null)
            arenaConfig.save(File("${plugin.dataFolder}/arena.yml"))
            plugin.saveConfig()
            debug("Arena ${arena.name} removed successfully")
        })
    }    
    
    fun loadArenas() {
        arenaConfig.load(File("${plugin.dataFolder}/arena.yml"))
        val arenaSection = arenaConfig.getConfigurationSection("arenas")

        if (arenaSection == null) {
            plugin.logger.warning(translate("arena.load.none"))
            debug("No arenas found in configuration")
            return
        }

        val keys = arenaSection.getKeys(false)
        debug("Found ${keys.size} arenas in configuration")
        var loadedCount = 0
        for (key in keys) {
            val arena = loadArenaByName(key)
            if (arena != null) {
                loadedCount++
                debug( "Successfully loaded arena: ${arena.name}")
            } else {
                plugin.logger.warning(translate("arena.load.failed", "arena_name" to key))
                debug( "Failed to load arena: $key")
            }
        }
        plugin.logger.info(translate("arena.load.success", "count" to loadedCount.toString()))
    }
    
    fun loadArenaByName(name: String): ArenaModel? {

        val worldName = arenaConfig.getString("arenas.$name.spawnRegion.world") ?: arenaConfig.getString("arenas.$name.spawnpoint.world")
        val world = worldName?.let { Bukkit.getWorld(it) } ?: return null

        val spawnpoint = if (arenaConfig.contains("arenas.$name.spawnpoint")) {
            Location(
                world,
                arenaConfig.getDouble("arenas.$name.spawnpoint.x"),
                arenaConfig.getDouble("arenas.$name.spawnpoint.y"),
                arenaConfig.getDouble("arenas.$name.spawnpoint.z"),
                arenaConfig.getDouble("arenas.$name.spawnpoint.yaw").toFloat(),
                arenaConfig.getDouble("arenas.$name.spawnpoint.pitch").toFloat()
            )
        } else if (arenaConfig.contains("arenas.$name.location")) {

            Location(
                world,
                arenaConfig.getDouble("arenas.$name.location.x"),
                arenaConfig.getDouble("arenas.$name.location.y"),
                arenaConfig.getDouble("arenas.$name.location.z"),
                arenaConfig.getDouble("arenas.$name.location.yaw").toFloat(),
                arenaConfig.getDouble("arenas.$name.location.pitch").toFloat()
            )
        } else {
            return null
        }

        val spawnRegion = if (arenaConfig.contains("arenas.$name.spawnRegion")) {
            val min = Location(
                world,
                arenaConfig.getDouble("arenas.$name.spawnRegion.min.x"),
                arenaConfig.getDouble("arenas.$name.spawnRegion.min.y"),
                arenaConfig.getDouble("arenas.$name.spawnRegion.min.z")
            )
            val max = Location(
                world,
                arenaConfig.getDouble("arenas.$name.spawnRegion.max.x"),
                arenaConfig.getDouble("arenas.$name.spawnRegion.max.y"),
                arenaConfig.getDouble("arenas.$name.spawnRegion.max.z")
            )
            Pair(min, max)
        } else {
            null
        }

        val killBlockName = arenaConfig.getString("arenas.$name.killBlock") ?: Material.VOID_AIR.name
        val killBlock = try {
            Material.valueOf(killBlockName)
        } catch (_: IllegalArgumentException) {
            plugin.logger.warning(translate("arena.load.killblock_not_found", "arena_name" to name))
            Material.VOID_AIR
        }

        val settings = mutableMapOf<String, Any>()
        val settingsSection = arenaConfig.getConfigurationSection("arenas.$name.settings")
        settingsSection?.getKeys(false)?.forEach { key ->
            settingsSection.get(key)?.let { settings[key] = it }
        }

        return ArenaModel(
            name = name,
            spawnRegion = spawnRegion,
            spawnpoint = spawnpoint,
            killBlock = killBlock,
            settings = settings
        )
    }    
    
    fun switchArena() {
        arenaConfig.load(File("${plugin.dataFolder}/arena.yml"))
        val arenaSection = arenaConfig.getConfigurationSection("arenas")

        if (arenaSection != null && arenaSection.getKeys(false).isNotEmpty()) {
            val arenaName = arenaSection.getKeys(false).random()
            val arena = loadArenaByName(arenaName)

            if (arena != null) {
                currentArena = arena
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    Bukkit.getOnlinePlayers().forEach { player ->
                        player.teleport(arena.spawnpoint)
                        player.sendMini(translate("arena.switch.success", "arena_name" to (currentArena?.name ?: "unknown")))
                    }
                    plugin.config.set("currentArena", arenaName)
                    plugin.saveConfig()
                })
            } else {
                clearArenaData()
            }
        } else {
            clearArenaData()
        }
    }

    private fun clearArenaData() {
        Bukkit.getScheduler().runTask(plugin, Runnable {
            plugin.config.set("currentArena", null)
        })
        currentArena = null
    }

    fun getArenaNames(): List<String> {
        val arenaSection = arenaConfig.getConfigurationSection("arenas")
        return arenaSection?.getKeys(false)?.toList() ?: emptyList()
    }

    fun startArenaCreation(player: Player, name: String, killBlock: Material, defaultSettings: Map<String, Any>? = null): ArenaCreationSession {
        try {
            val session = ArenaCreationSession(name, killBlock)
            
            // If default settings are provided, store them
            if (defaultSettings != null) {
                session.settings = defaultSettings.toMutableMap()
                debug("Arena creation session for ${player.name} initialized with ${defaultSettings.size} default settings")
                
                // Log the first few settings for debugging
                val settingsList = defaultSettings.entries.take(3).joinToString { "${it.key}=${it.value}" }
                debug("Sample settings: $settingsList${if (defaultSettings.size > 3) "..." else ""}")
            } else {
                debug("No default settings provided for ${player.name}'s arena creation session")
            }
            
            arenaCreationSessions[player.uniqueId] = session
            return session
        } catch (e: Exception) {
            debug("Error in startArenaCreation: ${e.message}")
            e.printStackTrace()
            
            // Create a basic session as fallback
            val fallbackSession = ArenaCreationSession(name, killBlock)
            arenaCreationSessions[player.uniqueId] = fallbackSession
            return fallbackSession
        }
    }

    fun getArenaCreationSession(player: Player): ArenaCreationSession? {
        return arenaCreationSessions[player.uniqueId]
    }
    fun completeArenaCreation(player: Player): Boolean {
        val session = arenaCreationSessions[player.uniqueId] ?: return false

        if (!session.isComplete()) {
            player.sendMini(translate("arena.create.incomplete"))
            return false
        }

        val arena = ArenaModel(
            name = session.name,
            spawnRegion = session.spawnRegion,
            spawnpoint = session.spawnpoint!!,
            killBlock = session.killBlock,
            settings = session.settings // Use the settings from the session
        )

        addArena(arena)
        arenaCreationSessions.remove(player.uniqueId)
        return true
    }

    fun cancelArenaCreation(player: Player) {
        arenaCreationSessions.remove(player.uniqueId)
    }

    fun updateArenaSetting(arenaName: String, settingKey: String, value: Any, player: Player, spawn: Boolean = false) {
        val arena = loadArenaByName(arenaName) ?: return

        val updatedSettings = arena.settings.toMutableMap()
        updatedSettings[settingKey] = value

        val updatedArena = ArenaModel(
            name = arena.name,
            spawnRegion = arena.spawnRegion,
            spawnpoint = arena.spawnpoint,
            killBlock = arena.killBlock,
            settings = updatedSettings
        )

        addArena(updatedArena) {
            // This code runs after the arena is saved
            if (currentArena?.name == arenaName) {
                currentArena = updatedArena
            }
            if (spawn) {
                SpawnSettingsGUI(plugin, player, arenaName)
            } else {
                ArenaSettingsGUI(plugin, player, arenaName)
            }
        }
    }
}

data class ArenaCreationSession(
    val name: String,
    val killBlock: Material,
    var spawnRegion: Pair<Location, Location>? = null,
    var spawnpoint: Location? = null,
    var step: ArenaCreationStep = ArenaCreationStep.SELECT_REGION,
    var settings: MutableMap<String, Any> = mutableMapOf() // Added settings property
) {
    fun isComplete(): Boolean {
        return spawnRegion != null && spawnpoint != null
    }

    fun nextStep() {
        step = when (step) {
            ArenaCreationStep.SELECT_REGION -> ArenaCreationStep.SET_SPAWNPOINT
            ArenaCreationStep.SET_SPAWNPOINT -> ArenaCreationStep.COMPLETE
            ArenaCreationStep.COMPLETE -> ArenaCreationStep.COMPLETE
        }
    }
}

enum class ArenaCreationStep {
    SELECT_REGION,
    SET_SPAWNPOINT,
    COMPLETE
}