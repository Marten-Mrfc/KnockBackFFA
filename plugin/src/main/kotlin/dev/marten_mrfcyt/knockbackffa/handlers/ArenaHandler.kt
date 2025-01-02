package dev.marten_mrfcyt.knockbackffa.handlers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.message
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.concurrent.CompletableFuture
import kotlin.text.clear

data class Arena(val name: String, val location: Location)

@OptIn(DelicateCoroutinesApi::class)
class ArenaHandler(private val plugin: KnockBackFFA) {
    private val arenas: MutableList<Arena> = mutableListOf()
    private var currentArena: Arena? = null
    private val arenaConfig: YamlConfiguration = YamlConfiguration.loadConfiguration(File("${plugin.dataFolder}/arena.yml"))

    init {
        if (plugin.isEnabled) {
            GlobalScope.launch {
                loadArenas()
            }
        }
    }

    suspend fun addArena(arena: Arena) {
        withContext<Unit>(Dispatchers.IO) {
            arenas.add(arena)
            switchArena()
        }
    }

    suspend fun removeArena(arena: Arena) {
        withContext<Unit>(Dispatchers.IO) {
            arenas.remove(arena)
            switchArena()
        }
    }

    private suspend fun loadArenas() {
        withContext(Dispatchers.IO) {
            arenas.clear()
            val arenaSection = arenaConfig.getConfigurationSection("arenas")
            if (arenaSection == null) {
                plugin.logger.warning("Arenas section not found in configuration | Please run /kbffa arena create <name> <killBlock>")
                return@withContext
            }

            for (key in arenaSection.getKeys(false)) {
                val location = locationFetcher(key)
                if (location != null) {
                    arenas.add(Arena(key, location))
                } else {
                    plugin.logger.warning("Failed to load arena: $key")
                }
            }

            if (arenas.isEmpty()) {
                plugin.logger.warning("No arenas loaded")
            } else {
                plugin.logger.info("Loaded ${arenas.size} arenas")
            }
        }
    }

    suspend fun switchArena() {
        withContext<Unit>(Dispatchers.IO) {
            if (arenas.isNotEmpty()) {
                val arena = arenas.random()
                currentArena = arena
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    Bukkit.getOnlinePlayers().forEach { player ->
                        player.teleport(arena.location)
                        player.message("<green>Teleported to arena <white>${arena.name}<green>!")
                    }
                    plugin.config.set("currentArena", arena.name)
                    plugin.config.set("currentLocation", arena.location)
                    plugin.saveConfig()
                })
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
        return if (world != null) {
            Location(world, x, y, z, yaw.toFloat(), pitch.toFloat())
        } else {
            null
        }
    }

    fun getArenaNames(): CompletableFuture<List<String>> {
        return CompletableFuture.supplyAsync {
            arenas.map { it.name }
        }
    }
}