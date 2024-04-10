package dev.marten_mrfcyt.knockbackffa.arena

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.message
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import kotlin.random.Random
import java.util.concurrent.CompletableFuture

data class Arena(val name: String, val location: Location)

class ArenaHandler(private val plugin: KnockBackFFA) {
    private val arenas: MutableList<Arena> = mutableListOf()
    private val arenasLoaded = CompletableFuture<Void>()
    init {
        // Load arenas from arena.yml asynchronously
        object : BukkitRunnable() {
            override fun run() {
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
                }
                println("Loaded ${arenas.size} arenas") // Debugging line
                arenasLoaded.complete(null) // Complete the CompletableFuture when the arenas are loaded
            }
        }.runTaskAsynchronously(plugin)
    }
    var currentArena: Arena? = null
        private set
    fun switchArena() {
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
        }
    }
    fun getArenaNames(): CompletableFuture<List<String>> {
        return arenasLoaded.thenApply { arenas.map { it.name } }
    }
    fun getArenaLocation(name: String): Location? {
        for (arena in arenas) {
            if (arena.name == name) {
                return arena.location
            }
        }
        return null
    }
}