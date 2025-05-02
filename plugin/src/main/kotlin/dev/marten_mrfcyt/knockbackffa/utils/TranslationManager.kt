package dev.marten_mrfcyt.knockbackffa.utils

import mlib.api.utilities.debug
import java.util.jar.JarFile
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.text.get
import kotlin.toString

class TranslationManager(private val plugin: Plugin) {
    private val translations = mutableMapOf<Locale, Map<String, Any>>()
    private val defaultLocale = Locale.ENGLISH
    private lateinit var configuredLocale: Locale

    init {
        initLanguageFiles()
        loadTranslations()
        loadConfiguredLanguage()
        plugin.logger.info("🌍 Using language: ${configuredLocale.displayLanguage} (${configuredLocale.language})")
        debug("Translation system initialized with locale: ${configuredLocale.language}")
    }
    fun translate(key: String, vararg args: Pair<String, Any>): String {
        val message = instance.get(key)

        // Only debug important messages to avoid log spam
        if (key.startsWith("arena.") || key.startsWith("error.") || key.startsWith("commands.")) {
            debug("Translating key: $key with ${args.size} arguments")
        }

        return if (message is List<*>) {
            message.firstOrNull()?.toString() ?: ""
        } else {
            message.toString()
        }.let { str ->
            args.fold(str) { acc, (placeholder, value) ->
                acc.replace("{$placeholder}", value.toString())
            }
        }
    }

    companion object {
        private lateinit var instance: TranslationManager

        fun init(plugin: Plugin) {
            instance = TranslationManager(plugin)
        }

        fun translate(key: String, vararg args: Pair<String, Any>): String {
            val message = instance.get(key)
            return if (message is List<*>) {
                message.firstOrNull()?.toString() ?: ""
            } else {
                message.toString()
            }.let { str ->
                args.fold(str) { acc, (placeholder, value) ->
                    acc.replace("<$placeholder>", value.toString())
                }
            }
        }

        fun getStringList(key: String, vararg args: Pair<String, Any>): List<String> {
            val message = instance.get(key)
            return when (message) {
                is List<*> -> message.mapNotNull { it?.toString() }.map { str ->
                    args.fold(str) { acc, (placeholder, value) ->
                        acc.replace("<$placeholder>", value.toString())
                    }
                }
                else -> listOf(message.toString())
            }
        }

        fun translateListRandom(key: String, vararg args: Pair<String, Any>): String {
            val list = getStringList(key, *args)
            return if (list.isNotEmpty()) {
                list.random()
            } else {
                key
            }
        }

        fun reload(plugin: Plugin) {
            instance = TranslationManager(plugin)
        }
    }

    private fun get(key: String, locale: Locale = configuredLocale): Any {
        val translation = translations[locale]?.get(key)
            ?: translations[defaultLocale]?.get(key)
            ?: key

        if (translation == key) {
            plugin.logger.warning("Translation key '$key' not found for locale '${locale.language}'")
        }

        val message = if (translation is List<*>) {
            translation.ifEmpty {
                "Empty list for message key '$key'"
            }
        } else {
            translation.toString()
        }

        return message
    }

    private fun loadConfiguredLanguage() {
        plugin.reloadConfig()
        val configLang = plugin.config.getString("language", "en")
        configuredLocale = Locale.forLanguageTag(configLang!!)

        if (!translations.containsKey(configuredLocale)) {
            plugin.logger.warning("Language '$configLang' not found, falling back to English")
            configuredLocale = defaultLocale
        }    }    private fun initLanguageFiles() {
        val langFolder = File(plugin.dataFolder, "lang")
        if (!langFolder.exists()) {
            langFolder.mkdirs()
        }

        try {
            // First ensure English exists as a fallback
            val enFile = File(langFolder, "en.yml")
            if (!enFile.exists()) {
                plugin.saveResource("lang/en.yml", false)
                plugin.logger.info("🏗️ Created fallback language file en.yml")
            }
              
            // Then try the dynamic approach with proper error handling
            val resourceLangFolder = plugin.javaClass.classLoader.getResource("lang")
            if (resourceLangFolder != null) {
                try {
                    val resourcePath = resourceLangFolder.toString()
                    if (resourcePath.startsWith("jar:")) {
                        val jarPath = resourcePath.substring(4, resourcePath.indexOf("!")).replace("file:", "")
                        // Decode URL-encoded paths safely
                        val decodedJarPath = try {
                            URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name())
                        } catch (_: Exception) {
                            jarPath // Return original if decoding fails
                        }
                        
                        val jarFile = JarFile(File(decodedJarPath))
                        val entries = jarFile.entries()
                        
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            if (entry.name.startsWith("lang/") && entry.name.endsWith(".yml") && !entry.name.endsWith("/en.yml")) {
                                val resourceFileName = entry.name.substringAfterLast("/")
                                val langFile = File(langFolder, resourceFileName)
                                if (!langFile.exists()) {
                                    plugin.saveResource(entry.name, false)
                                    plugin.logger.info("🏗️ Created language file $resourceFileName")
                                }
                            }
                        }
                        jarFile.close()
                    } else {
                        // Handle file system case if needed
                        plugin.logger.info("Language resources found in the file system, processing directly")
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to extract language files dynamically: ${e.message}")
                    plugin.logger.info("Continuing with English as fallback")
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to initialize language files: ${e.message}")
            // Continue execution with English
        }
    }    private fun loadTranslations() {
        val langFolder = File(plugin.dataFolder, "lang")
        val loadedLocales = mutableListOf<String>()
        var totalTranslations = 0

        debug("Loading translations from: ${langFolder.absolutePath}")
        langFolder.listFiles { file -> file.extension == "yml" }?.forEach { file ->
            try {
                val locale = Locale.forLanguageTag(file.nameWithoutExtension)
                debug("Processing language file: ${file.name} for locale: ${locale.language}")
                
                val loadedTranslations = YamlConfiguration.loadConfiguration(file)
                    .getValues(true)
                    .mapValues { (_, value) ->
                        when (value) {
                            is List<*> -> value.map { it.toString() }
                            else -> value.toString()
                        }
                    }

                translations[locale] = loadedTranslations
                loadedLocales.add("${locale.language}(${loadedTranslations.size})")
                totalTranslations += loadedTranslations.size
                debug("Successfully loaded ${loadedTranslations.size} translations for ${locale.language}")
            } catch (e: Exception) {
                plugin.logger.warning("❌ Failed to load language file ${file.name}: ${e.message}")
                debug("Error loading language file ${file.name}: ${e.message}")
            }
        }

        plugin.logger.info("✔️ Loaded $totalTranslations translations across ${loadedLocales.size} locales: ${loadedLocales.joinToString(", ")}")
    }
}