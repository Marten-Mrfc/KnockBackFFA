package dev.marten_mrfcyt.knockbackffa.boosts.managers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.boosts.models.Boost
import dev.marten_mrfcyt.knockbackffa.boosts.models.BoostType
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.logging.Level
import kotlin.text.compareTo

class BoostManager(private val plugin: KnockBackFFA) {
    private val boostRegistry = BoostRegistry(plugin)
    private val configHandler = BoostConfigHandler(plugin)

    init {
        configHandler.loadConfig()
        registerAllBoosts()
        applyConfigsToAllBoosts()
    }

    fun getBoost(id: String): Boost {
        return boostRegistry.getBoost(id) ?: throw IllegalArgumentException(
            TranslationManager.translate("boost.manager.not_found", "id" to id)
        )
    }

    fun getAllBoosts(): Collection<Boost> {
        return boostRegistry.getAllBoosts()
    }

    fun reloadBoosts() {
        plugin.logger.info(TranslationManager.translate("boost.manager.reload.start"))

        val unregisteredCount = boostRegistry.getAllBoosts().count { it is Listener }
        boostRegistry.getAllBoosts().forEach { boost ->
            if (boost is Listener) {
                HandlerList.unregisterAll(boost)
            }
        }
        plugin.logger.info(TranslationManager.translate("boost.manager.reload.unregistered",
            "count" to unregisteredCount))

        val oldCount = boostRegistry.getAllBoosts().size
        boostRegistry.clearBoosts()

        configHandler.loadConfig()

        try {
            registerAllBoosts()
            applyConfigsToAllBoosts()
            registerEvents(plugin)
            val newCount = boostRegistry.getAllBoosts().size
            val changeText = when {
                newCount > oldCount -> "+${newCount - oldCount}"
                newCount < oldCount -> "-${oldCount - newCount}"
                else -> "no"
            }

            plugin.logger.info(TranslationManager.translate("boost.manager.reload.complete",
                "count" to newCount, "change" to changeText))

            KnockBackFFA.instance.playerBoostManager.reloadAllPlayerBoosts()
        } catch (e: Exception) {
            plugin.logger.severe(TranslationManager.translate("boost.manager.reload.error",
                "error" to e.message.toString()))
            plugin.logger.info(TranslationManager.translate("boost.manager.reload.recovery"))
            e.printStackTrace()
        }
    }

    private fun applyConfigsToAllBoosts() {
        boostRegistry.getAllBoosts().forEach { boost ->
            try {
                configHandler.applyConfigToBoost(boost)
            } catch (e: Exception) {
                plugin.logger.warning(TranslationManager.translate("boost.manager.config_apply_failed",
                    "boost_id" to boost.id, "error" to e.message.toString()))
            }
        }
    }

    private fun registerAllBoosts() {
        plugin.logger.info(TranslationManager.translate("boost.manager.register_start"))

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
            plugin.logger.info(TranslationManager.translate("boost.manager.register_success",
                "count" to registeredBoosts.size, "names" to boostNames))
        } catch (e: Exception) {
            plugin.logger.severe(TranslationManager.translate("boost.manager.register_error",
                "error" to e.message.toString()))
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
            plugin.logger.log(Level.WARNING, TranslationManager.translate("boost.manager.class_process_error",
                "class" to className, "error" to e.message.toString()), e)
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
        plugin.logger.info(TranslationManager.translate("boost.manager.listeners_registered",
            "count" to boostRegistry.getAllBoosts().count { it is Listener }))
    }
}