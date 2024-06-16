package dev.marten_mrfcyt.knockbackffa.handlers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.message
import org.bukkit.Bukkit
import org.bukkit.Location
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
        // Load arenas from arena.yml asynchronously
        object : BukkitRunnable() {
            override fun run() {
                try {
                    val config = YamlConfiguration.loadConfiguration(File("${plugin.dataFolder}/arena.yml"))
                    val arenaSection = config.getConfigurationSection("arenas")
                    if (arenaSection != null) {
                        for (key in arenaSection.getKeys(false)) {
                            val worldName = arenaSection.getString("$key.location.world")
                            val x = arenaSection.getDouble("$key.location.x")
                            val y = arenaSection.getDouble("$key.location.y")
                            val z = arenaSection.getDouble("$key.location.z")
                            val yaw = arenaSection.getDouble("$key.location.yaw")
                            val pitch = arenaSection.getDouble("$key.location.pitch")
                            val world = worldName?.let { Bukkit.getWorld(it) }
                            if (world != null) {
                                val location = Location(world, x, y, z, yaw.toFloat(), pitch.toFloat())
                                arenas.add(Arena(key, location))
                            }
                        }
                        areArenasLoaded = true // Set the variable to true when arenas are loaded
                    }
                    println("Loaded ${arenas.size} arenas") // Debugging line
                    arenasLoaded.complete(null) // Complete the CompletableFuture when the arenas are loaded
                } catch (e: Exception) {
                    println("Error loading arena(s): ${e.message}")
                    areArenasLoaded = false // Set the variable to false if there's an error
                    arenasLoaded.completeExceptionally(e)
                }
            }
        }.runTaskAsynchronously(plugin)
    }

    private var currentArena: Arena? = null
    fun switchArena() {
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
}