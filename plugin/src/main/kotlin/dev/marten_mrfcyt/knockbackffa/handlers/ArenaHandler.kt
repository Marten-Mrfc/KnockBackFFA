package dev.marten_mrfcyt.knockbackffa.handlers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.message
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.concurrent.CompletableFuture

data class Arena(val name: String, val location: Location, val killBlock: String = Material.VOID_AIR.name)

@OptIn(DelicateCoroutinesApi::class)
class ArenaHandler(private val plugin: KnockBackFFA) {
    private var currentArena: Arena? = null
    private val arenaConfig = YamlConfiguration.loadConfiguration(File("${plugin.dataFolder}/arena.yml"))

    suspend fun addArena(arena: Arena) {
        withContext(Dispatchers.IO) {
            arenaConfig.set("arenas.${arena.name}.location.world", arena.location.world?.name)
            arenaConfig.set("arenas.${arena.name}.location.x", arena.location.x)
            arenaConfig.set("arenas.${arena.name}.location.y", arena.location.y)
            arenaConfig.set("arenas.${arena.name}.location.z", arena.location.z)
            arenaConfig.set("arenas.${arena.name}.location.yaw", arena.location.yaw)
            arenaConfig.set("arenas.${arena.name}.location.pitch", arena.location.pitch)
            arenaConfig.set("arenas.${arena.name}.killBlock", arena.killBlock)
            arenaConfig.save(File("${plugin.dataFolder}/arena.yml"))
        }
    }

    suspend fun removeArena(arena: Arena) {
        withContext(Dispatchers.IO) {
            arenaConfig.set("arenas.${arena.name}", null)
            arenaConfig.save(File("${plugin.dataFolder}/arena.yml"))
            plugin.saveConfig()
        }
    }

    suspend fun loadArenas() {
        arenaConfig.load(File("${plugin.dataFolder}/arena.yml")) // Reload the configuration
        val arenaSection = arenaConfig.getConfigurationSection("arenas")
        withContext(Dispatchers.IO) {
            if (arenaSection == null) {
                plugin.logger.warning("Arenas section not found in configuration | Please run /kbffa arena create <name> <killBlock>")
                return@withContext
            }

            val keys = arenaSection.getKeys(false)
            var loadedCount = 0
            for (key in keys) {
                val location = locationFetcher(key)
                if (location != null) {
                    loadedCount++
                } else {
                    plugin.logger.warning("Failed to load arena $key")
                }
            }
            plugin.logger.info("Loaded $loadedCount arenas")
        }
    }

    suspend fun switchArena() {
        withContext(Dispatchers.IO) {
            arenaConfig.load(File("${plugin.dataFolder}/arena.yml")) // Reload the configuration
            val arenaSection = arenaConfig.getConfigurationSection("arenas")
            if (arenaSection != null && arenaSection.getKeys(false).isNotEmpty()) {
                val arenaName = arenaSection.getKeys(false).random()
                val location = locationFetcher(arenaName)
                if (location != null) {
                    currentArena = Arena(arenaName, location)
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        Bukkit.getOnlinePlayers().forEach { player ->
                            player.teleport(location)
                            player.message("<green>Teleported to arena <white>${arenaName}<green>!")
                        }
                        plugin.config.set("currentArena", arenaName)
                        plugin.config.set("currentLocation", location)
                        plugin.saveConfig()
                    })
                } else {
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        plugin.config.set("currentArena", null)
                        plugin.config.set("currentLocation", null)
                    })
                }
            } else {
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    plugin.config.set("currentArena", null)
                    plugin.config.set("currentLocation", null)
                })
            }
        }
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
            if(!killBlock.isEmpty()) {
            Location(world, x, y, z, yaw.toFloat(), pitch.toFloat())
            } else {
                plugin.logger.warning("Failed to load arena $key: Kill block not found")
                null
            }
        } else {
            null
        }
    }

    fun getArenaNames(): CompletableFuture<List<String>> {
        return CompletableFuture.supplyAsync {
            val arenaSection = arenaConfig.getConfigurationSection("arenas")
            arenaSection?.getKeys(false)?.toList() ?: emptyList()
        }
    }
}