package dev.marten_mrfcyt.knockbackffa.arena

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
import mlib.api.utilities.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

data class Arena(val name: String, val location: Location, val killBlock: Material = Material.VOID_AIR)
var currentArena: Arena? = null
class ArenaHandler(private val plugin: KnockBackFFA) {
    private val arenaConfig: YamlConfiguration

    init {
        val arenaFile = File("${plugin.dataFolder}/arena.yml")
        if (!arenaFile.exists()) {
            arenaFile.createNewFile()
            YamlConfiguration().save(arenaFile)
        }
        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile)
    }

    fun addArena(arena: Arena) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            arenaConfig.set("arenas.${arena.name}.location.world", arena.location.world?.name)
            arenaConfig.set("arenas.${arena.name}.location.x", arena.location.x)
            arenaConfig.set("arenas.${arena.name}.location.y", arena.location.y)
            arenaConfig.set("arenas.${arena.name}.location.z", arena.location.z)
            arenaConfig.set("arenas.${arena.name}.location.yaw", arena.location.yaw)
            arenaConfig.set("arenas.${arena.name}.location.pitch", arena.location.pitch)
            arenaConfig.set("arenas.${arena.name}.killBlock", arena.killBlock.name)
            arenaConfig.save(File("${plugin.dataFolder}/arena.yml"))
        })
    }

    fun removeArena(arena: Arena) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            arenaConfig.set("arenas.${arena.name}", null)
            arenaConfig.save(File("${plugin.dataFolder}/arena.yml"))
            plugin.saveConfig()
        })
    }

    fun loadArenas() {
        arenaConfig.load(File("${plugin.dataFolder}/arena.yml"))
        val arenaSection = arenaConfig.getConfigurationSection("arenas")

        if (arenaSection == null) {
            plugin.logger.warning(translate("arena.load.none"))
            return
        }

        val keys = arenaSection.getKeys(false)
        var loadedCount = 0
        for (key in keys) {
            val location = locationFetcher(key)
            if (location != null) {
                loadedCount++
            } else {
                plugin.logger.warning(translate("arena.load.failed", "arena_name" to key))
            }
        }
        plugin.logger.info(translate("arena.load.success", "count" to loadedCount.toString()))
    }

    fun switchArena() {
        arenaConfig.load(File("${plugin.dataFolder}/arena.yml"))
        val arenaSection = arenaConfig.getConfigurationSection("arenas")

        if (arenaSection != null && arenaSection.getKeys(false).isNotEmpty()) {
            val arenaName = arenaSection.getKeys(false).random()
            val location = locationFetcher(arenaName)
            val killBlock = arenaConfig.getString("arenas.$arenaName.killBlock") ?: Material.VOID_AIR.name
            if (location != null) {
                currentArena = Arena(arenaName, location, Material.valueOf(killBlock))
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    Bukkit.getOnlinePlayers().forEach { player ->
                        player.teleport(location)
                        player.message(translate("arena.switch.success", "arena_name" to (currentArena?.name ?: "unknown")))
                    }
                    plugin.config.set("currentArena", arenaName)
                    plugin.config.set("currentLocation", location)
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
            plugin.config.set("currentLocation", null)
        })
        currentArena = null
    }

    internal fun locationFetcher(key: String): Location? {
        val worldName = arenaConfig.getString("arenas.$key.location.world")
        val x = arenaConfig.getDouble("arenas.$key.location.x")
        val y = arenaConfig.getDouble("arenas.$key.location.y")
        val z = arenaConfig.getDouble("arenas.$key.location.z")
        val yaw = arenaConfig.getDouble("arenas.$key.location.yaw")
        val pitch = arenaConfig.getDouble("arenas.$key.location.pitch")
        val world = worldName?.let { Bukkit.getWorld(it) }
        val killBlock = arenaConfig.getString("arenas.$key.killBlock") ?: Material.VOID_AIR.name

        return if (world != null) {
            if (killBlock.isNotEmpty()) {
                Location(world, x, y, z, yaw.toFloat(), pitch.toFloat())
            } else {
                plugin.logger.warning(translate("arena.load.killblock_not_found", "arena_name" to key))
                null
            }
        } else {
            null
        }
    }

    fun getArenaNames(): List<String> {
        val arenaSection = arenaConfig.getConfigurationSection("arenas")
        return arenaSection?.getKeys(false)?.toList() ?: emptyList()
    }
}