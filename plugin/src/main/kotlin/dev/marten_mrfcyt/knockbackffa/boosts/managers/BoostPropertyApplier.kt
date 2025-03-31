package dev.marten_mrfcyt.knockbackffa.boosts.managers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.boosts.models.Boost
import dev.marten_mrfcyt.knockbackffa.boosts.models.ConfigurableProperty
import dev.marten_mrfcyt.knockbackffa.boosts.models.EffectBoost
import dev.marten_mrfcyt.knockbackffa.boosts.models.StatsBoost
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import java.time.Duration

class BoostPropertyApplier(private val plugin: KnockBackFFA) {

    fun applyConfigValues(boost: Boost, config: ConfigurationSection) {
        processAnnotatedProperties(boost, config)

        processCommonProperties(boost, config)

        processTypeSpecificProperties(boost, config)
    }

    private fun processAnnotatedProperties(boost: Any, config: ConfigurationSection) {
        val fields = boost.javaClass.declaredFields

        for (field in fields) {
            val annotation = field.getAnnotation(ConfigurableProperty::class.java) ?: continue
            val configKey = annotation.configKey

            if (config.contains(configKey)) {
                try {
                    field.isAccessible = true
                    when (field.type) {
                        Double::class.java -> field.set(boost, config.getDouble(configKey))
                        Int::class.java -> field.set(boost, config.getInt(configKey))
                        String::class.java -> field.set(boost, config.getString(configKey))
                        Boolean::class.java -> field.set(boost, config.getBoolean(configKey))
                        Long::class.java -> field.set(boost, config.getLong(configKey))
                        Float::class.java -> field.set(boost, config.getDouble(configKey).toFloat())
                        Material::class.java -> {
                            val materialName = config.getString(configKey)
                            try {
                                if (materialName != null) {
                                    val material = Material.valueOf(materialName.uppercase())
                                    field.set(boost, material)
                                }
                            } catch (_: IllegalArgumentException) {
                                plugin.logger.warning("Invalid material name in config: $materialName")
                            }
                        }
                        // Add other types as needed
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to apply configurable property $configKey: ${e.message}")
                }
            }
        }
    }

    private fun processCommonProperties(boost: Boost, config: ConfigurationSection) {
        // Get all fields from the Boost class
        val commonFields = Boost::class.java.declaredFields

        for (field in commonFields) {
            val fieldName = field.name
            if (config.contains(fieldName)) {
                try {
                    field.isAccessible = true
                    when (field.type) {
                        String::class.java ->
                            field.set(boost, config.getString(fieldName))
                        Int::class.java ->
                            field.set(boost, config.getInt(fieldName))
                        Double::class.java ->
                            field.set(boost, config.getDouble(fieldName))
                        Material::class.java -> {
                            val materialName = config.getString(fieldName)
                            if (materialName != null) {
                                try {
                                    val material = Material.valueOf(materialName.uppercase())
                                    field.set(boost, material)
                                } catch (_: IllegalArgumentException) {
                                    plugin.logger.warning("Invalid material name for boost ${boost.id}: $materialName")
                                }
                            }
                        }
                        List::class.java -> {
                            if (field.genericType.toString().contains("String")) {
                                field.set(boost, config.getStringList(fieldName))
                            }
                        }
                        Boolean::class.java ->
                            field.set(boost, config.getBoolean(fieldName))
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to apply common property $fieldName: ${e.message}")
                }
            }
        }
    }

    private fun processTypeSpecificProperties(boost: Boost, config: ConfigurationSection) {
        when (boost) {
            is EffectBoost -> {
                if (config.contains("effectDuration")) {
                    val minutes = config.getInt("effectDuration")
                    setFieldValue(boost, "effectDuration", Duration.ofMinutes(minutes.toLong()))
                }
            }
            is StatsBoost -> {
                if (config.contains("multiplier")) {
                    val multiplier = config.getDouble("multiplier")
                    setFieldValue(boost, "multiplier", multiplier)
                }
                if (config.contains("effectDuration")) {
                    val minutes = config.getInt("effectDuration")
                    setFieldValue(boost, "effectDuration", Duration.ofMinutes(minutes.toLong()))
                }
            }
        }
    }

    private fun setFieldValue(instance: Any, fieldName: String, value: Any) {
        try {
            val field = findField(instance.javaClass, fieldName)
            if (field == null) {
                plugin.logger.warning("Field '$fieldName' not found in ${instance.javaClass.name} or its superclasses")
                return
            }

            if (!isAssignable(field.type, value)) {
                plugin.logger.warning("Type mismatch for field '$fieldName': expected ${field.type.name}, but got ${value.javaClass.name}")
                return
            }

            field.isAccessible = true
            field.set(instance, value)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to set field '$fieldName' on ${instance.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun findField(clazz: Class<*>, fieldName: String): java.lang.reflect.Field? {
        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName)
            } catch (_: NoSuchFieldException) {
                currentClass = currentClass.superclass
            }
        }
        return null
    }

    private fun isAssignable(fieldType: Class<*>, value: Any): Boolean {
        if (fieldType.isPrimitive) {
            when (fieldType.name) {
                "int" -> return value is Int
                "double" -> return value is Double
                "boolean" -> return value is Boolean
                "long" -> return value is Long
                "float" -> return value is Float
            }
        }

        if (fieldType == Duration::class.java && value is Duration) {
            return true
        }

        return fieldType.isInstance(value)
    }
}