package dev.marten_mrfcyt.knockbackffa.utils

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.*
import kotlin.collections.get
import kotlin.random.Random

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
            return instance.get(key, instance.configuredLocale, args.toMap().mapValues { it.value.toString() })
        }

        fun reload(plugin: Plugin) {
            instance = TranslationManager(plugin)
        }
    }

    private fun get(key: String, locale: Locale = configuredLocale, args: Map<String, String> = emptyMap()): String {
        val translation = translations[locale]?.get(key)
            ?: translations[defaultLocale]?.get(key)
            ?: key

        if (translation == key) {
            plugin.logger.warning("Translation key '$key' not found for locale '${locale.language}'")
        }
        val message = if (translation is List<*>) {
            if (translation.isNotEmpty()) {
                translation[Random.nextInt(translation.size)].toString()
            } else {
                "Empty list for message key '$key'"
            }
        } else {
            translation.toString()
        }

        return args.entries.fold(message) { acc, (key, value) ->
            acc.replace("<$key>", value)
        }
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

        val defaultLangFile = File(langFolder, "en.yml")
        if (!defaultLangFile.exists()) {
            plugin.saveResource("lang/en.yml", false)
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