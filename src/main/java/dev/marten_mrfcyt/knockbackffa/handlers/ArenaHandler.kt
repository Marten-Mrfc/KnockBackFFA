package dev.marten_mrfcyt.knockbackffa.handlers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.message
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

data class Arena(val name: String, val location: Location)

class ArenaHandler(private val plugin: KnockBackFFA) {
    val arenas: MutableList<Arena> = mutableListOf()
    private val arenasLoaded = CompletableFuture<Void>()
    var areArenasLoaded = false // Add this line
    init {
        if(plugin.isEnabled) {
            loadArenas()
        }
    }
    fun addArena(arena: Arena) {
        arenas.add(arena)
    }
    fun removeArena(arena: Arena) {
        arenas.remove(arena)
    }
         fun loadArenas() {
            object : BukkitRunnable() {
                override fun run() {
                    try {
                        println(arenas)
                        arenas.clear()
                        val config = YamlConfiguration.loadConfiguration(File("${plugin.dataFolder}/arena.yml"))
                        val arenaSection = config.getConfigurationSection("arenas") as ConfigurationSection
                        for (key in arenaSection.getKeys(false)) {
                            locationFetcher(key, arenaSection).let { location ->
                                println("Loading arena $key at $location")
                                location?.let { Arena(key, it) }?.let { arenas.add(it) }
                                println(arenas)
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

    private var currentArena: Arena? = null
    fun switchArena() {
        println(arenas)
        arenasLoaded.thenRun {
            if (arenas.isNotEmpty()) {
                val arena = arenas[Random.nextInt(arenas.size)]
                currentArena = arena
                for (player in Bukkit.getOnlinePlayers()) {
                    player.teleport(arena.location)
                    player.message("<green>Teleported to arena <white>${arena.name}<green>!")
                }
                // Save the current arena name in the config.yml file
                plugin.config.set("currentArena", arena.name)
                plugin.config.set("currentLocation", arena.location)
                plugin.saveConfig()
            } else {
                println("Error: No arena(s) available to switch to.")
            }
        }
    }
    fun arenasLoaded(): Int {
        val config = YamlConfiguration.loadConfiguration(File("${plugin.dataFolder}/arena.yml"))
        val arenaSection = config.getConfigurationSection("arenas")
        return arenaSection?.getKeys(false)?.size ?: 0
    }

    fun getArenaNames(): CompletableFuture<List<String>> {
        return arenasLoaded.thenApply { arenas.map { it.name } }
    }

    fun locationFetcher(key: String, arenaSection: ConfigurationSection): Location? {
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