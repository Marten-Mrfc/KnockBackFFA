package dev.marten_mrfcyt.knockbackffa.utils

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.*
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
    }

    companion object {
        private lateinit var instance: TranslationManager

        fun init(plugin: Plugin) {
            instance = TranslationManager(plugin)
        }

        fun translate(key: String, vararg args: Pair<String, Any>): String {
            val message = instance.get(key, args = args.toMap().mapValues { it.value.toString() })
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
            val message = instance.get(key, args = args.toMap().mapValues { it.value.toString() })
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

    private fun get(key: String, locale: Locale = configuredLocale, args: Map<String, String> = emptyMap()): Any {
        val translation = translations[locale]?.get(key)
            ?: translations[defaultLocale]?.get(key)
            ?: key

        if (translation == key) {
            plugin.logger.warning("Translation key '$key' not found for locale '${locale.language}'")
        }

        val message = if (translation is List<*>) {
            if (translation.isNotEmpty()) {
                translation
            } else {
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
        }
    }

    private fun initLanguageFiles() {
        val langFolder = File(plugin.dataFolder, "lang")
        if (!langFolder.exists()) {
            langFolder.mkdirs()
        }

        val resourceLangFolder = plugin.javaClass.classLoader.getResource("lang")
        if (resourceLangFolder != null) {
            val resourcePath = resourceLangFolder.path
            val jarPath = resourcePath.substring(0, resourcePath.indexOf("!")).replace("file:", "")
            val jarFile = java.util.jar.JarFile(jarPath)
            val entries = jarFile.entries()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.startsWith("lang/") && entry.name.endsWith(".yml")) {
                    val resourceFileName = entry.name.substringAfterLast("/")
                    val langFile = File(langFolder, resourceFileName)
                    if (!langFile.exists()) {
                        plugin.saveResource(entry.name, false)
                        plugin.logger.info("Created language file $resourceFileName")
                    }
                }
            }
        }
    }

    private fun loadTranslations() {
        val langFolder = File(plugin.dataFolder, "lang")
        langFolder.listFiles { file -> file.extension == "yml" }?.forEach { file ->
            try {
                val locale = Locale.forLanguageTag(file.nameWithoutExtension)
                val loadedTranslations = YamlConfiguration.loadConfiguration(file)
                    .getValues(true)
                    .mapValues { (_, value) ->
                        when (value) {
                            is List<*> -> value.map { it.toString() }
                            else -> value.toString()
                        }
                    }
                translations[locale] = loadedTranslations
                plugin.logger.info("Loaded ${loadedTranslations.size} translations for locale '${locale.language}'")
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load language file ${file.name}: ${e.message}")
            }
        }
    }
}