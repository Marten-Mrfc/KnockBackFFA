package dev.marten_mrfcyt.knockbackffa.handlers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.message
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.util.concurrent.CompletableFuture

data class Arena(val name: String, val location: Location)

class ArenaHandler(private val plugin: KnockBackFFA) {
    val arenas: MutableList<Arena> = mutableListOf()
    private val arenasLoaded = CompletableFuture<Void>()
    var areArenasLoaded = false
        private set

    private var currentArena: Arena? = null

    init {
        if (plugin.isEnabled) {
            loadArenas()
        }
    }

    fun addArena(arena: Arena) {
        arenas.add(arena)
    }

    fun removeArena(arena: Arena) {
        arenas.remove(arena)
    }

    private fun loadArenas() {
        object : BukkitRunnable() {
            override fun run() {
                try {
                    arenas.clear()
                    val config = YamlConfiguration.loadConfiguration(File("${plugin.dataFolder}/arena.yml"))
                    val arenaSection = config.getConfigurationSection("arenas")
                        ?: throw IllegalArgumentException("Arenas section not found in configuration")

                    for (key in arenaSection.getKeys(false)) {
                        locationFetcher(key)?.let { location ->
                            arenas.add(Arena(key, location))
                        }
                    }
                    areArenasLoaded = true
                    arenasLoaded.complete(null)
                } catch (e: Exception) {
                    areArenasLoaded = false
                    arenasLoaded.completeExceptionally(e)
                }
            }
        }.runTaskAsynchronously(plugin)
    }

    fun switchArena() {
        arenasLoaded.thenRun {
            if (arenas.isNotEmpty()) {
                val arena = arenas.random()
                currentArena = arena
                Bukkit.getOnlinePlayers().forEach { player ->
                    player.teleport(arena.location)
                    player.message("<green>Teleported to arena <white>${arena.name}<green>!")
                }
                plugin.config.set("currentArena", arena.name)
                plugin.config.set("currentLocation", arena.location)
                plugin.saveConfig()
            } else {
                plugin.config.set("currentArena", null)
                plugin.config.set("currentLocation", null)
            }
        }
    }

    fun getArenaNames(): CompletableFuture<List<String>> {
        return arenasLoaded.thenApply { arenas.map { it.name } }
    }

    fun locationFetcher(key: String): Location? {
        val config = YamlConfiguration.loadConfiguration(File("${plugin.dataFolder}/arena.yml"))
        val arenaSection = config.getConfigurationSection("arenas")
            ?: throw IllegalArgumentException("Arenas section not found in configuration")
        val worldName = arenaSection.getString("$key.location.world")
        val x = arenaSection.getDouble("$key.location.x")
        val y = arenaSection.getDouble("$key.location.y")
        val z = arenaSection.getDouble("$key.location.z")
        val yaw = arenaSection.getDouble("$key.location.yaw")
        val pitch = arenaSection.getDouble("$key.location.pitch")
        val world = worldName?.let { Bukkit.getWorld(it) }
        return if (world != null) {
            Location(world, x, y, z, yaw.toFloat(), pitch.toFloat())
        } else {
            null
        }
    }
}
