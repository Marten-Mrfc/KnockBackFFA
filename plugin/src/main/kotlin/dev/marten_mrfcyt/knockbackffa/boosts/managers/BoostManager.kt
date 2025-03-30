package dev.marten_mrfcyt.knockbackffa.boosts.managers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.boosts.models.Boost
import dev.marten_mrfcyt.knockbackffa.boosts.models.BoostType
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.logging.Level

class BoostManager(private val plugin: KnockBackFFA) {
    private val boostRegistry = BoostRegistry(plugin)
    private val configHandler = BoostConfigHandler(plugin)

    init {
        configHandler.loadConfig()
        registerAllBoosts()
        applyConfigsToAllBoosts()
    }

    fun getBoost(id: String): Boost {
        return boostRegistry.getBoost(id) ?: throw IllegalArgumentException("Boost with ID $id not found")
    }

    fun getAllBoosts(): Collection<Boost> {
        return boostRegistry.getAllBoosts()
    }

    fun reloadBoosts() {
        boostRegistry.getAllBoosts().forEach { boost ->
            if (boost is Listener) {
                HandlerList.unregisterAll(boost)
            }
        }

        boostRegistry.clearBoosts()

        configHandler.loadConfig()

        registerAllBoosts()

        applyConfigsToAllBoosts()

        KnockBackFFA.instance.playerBoostManager.reloadAllPlayerBoosts()
    }

    private fun applyConfigsToAllBoosts() {
        boostRegistry.getAllBoosts().forEach { boost ->
            try {
                configHandler.applyConfigToBoost(boost)
            } catch (e: Exception) {
                plugin.logger.warning("Failed to apply configuration to boost ${boost.id}: ${e.message}")
            }
        }
    }

    private fun registerAllBoosts() {
        plugin.logger.info("🤖 Registering boosts automatically...")

        val boostsPackage = "dev.marten_mrfcyt.knockbackffa.boosts.boosts"
        val classLoader = plugin.javaClass.classLoader
        val registeredBoosts = mutableListOf<String>()

        try {
            val path = boostsPackage.replace('.', '/')
            val resources = classLoader.getResources(path)

            while (resources.hasMoreElements()) {
                val resource = resources.nextElement()
                val urls = resource.toString()

                if (urls.startsWith("jar:")) {
                    processJarResource(urls, path, registeredBoosts)
                } else {
                    processFileSystemResource(resource, registeredBoosts)
                }
            }

            val boostNames = registeredBoosts.joinToString(", ")
            plugin.logger.info("✅ Successfully found ${registeredBoosts.size} boosts: $boostNames")
        } catch (e: Exception) {
            plugin.logger.severe("Error loading boosts: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun processJarResource(urls: String, path: String, registeredBoosts: MutableList<String>) {
        val jarPath = urls.substringAfter("jar:file:").substringBefore("!")
        val jarFile = java.util.jar.JarFile(java.io.File(jarPath))

        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val entryName = entry.name

            if (entryName.startsWith(path) && entryName.endsWith(".class") && !entryName.contains('$')) {
                val className = entryName.replace('/', '.').removeSuffix(".class")
                val boostName = processBoostClass(className)
                if (boostName != null) {
                    registeredBoosts.add(boostName)
                }
            }
        }
        jarFile.close()
    }

    private fun processFileSystemResource(resource: java.net.URL, registeredBoosts: MutableList<String>) {
        val directory = java.io.File(resource.toURI())
        if (directory.exists()) {
            directory.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".class") && !file.name.contains('$')) {
                    val className = "dev.marten_mrfcyt.knockbackffa.boosts.boosts.${file.name.removeSuffix(".class")}"
                    val boostName = processBoostClass(className)
                    if (boostName != null) {
                        registeredBoosts.add(boostName)
                    }
                }
            }
        }
    }

    private fun processBoostClass(className: String): String? {
        try {
            val clazz = Class.forName(className)

            if (!Boost::class.java.isAssignableFrom(clazz) ||
                java.lang.reflect.Modifier.isAbstract(clazz.modifiers)) {
                return null
            }

            val annotation = clazz.getAnnotation(BoostType::class.java)
            if (annotation != null) {
                val field = clazz.getDeclaredField("INSTANCE")
                field.isAccessible = true
                val boost = field.get(null) as Boost

                boostRegistry.register(boost)
                return boost.id
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Could not process potential boost class $className", e)
        }
        return null
    }

    fun updateBoostConfig(boostId: String, key: String, value: Any) {
        val boost = getBoost(boostId)
        configHandler.updateBoostConfig(boost, key, value)
        configHandler.applyConfigToBoost(boost)
    }

    fun registerEvents(plugin: Plugin) {
        boostRegistry.getAllBoosts().forEach { boost ->
            if (boost is Listener) {
                plugin.server.pluginManager.registerEvents(boost, plugin)
            }
        }
        plugin.logger.info("👂 Registered ${boostRegistry.getAllBoosts().count { it is Listener }} boost listeners")
    }
}