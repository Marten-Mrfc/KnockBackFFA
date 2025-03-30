package dev.marten_mrfcyt.knockbackffa.guis.editor.boosts

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.boosts.models.Boost
import dev.marten_mrfcyt.knockbackffa.boosts.models.ConfigurableProperty
import mlib.api.forms.Form
import mlib.api.forms.FormType
import mlib.api.gui.GuiSize
import mlib.api.gui.types.builder.PaginatedGuiBuilder
import mlib.api.utilities.asMini
import mlib.api.utilities.message
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.lang.reflect.Field
import java.time.Duration

class BoostSpecialPropertiesEditor(
    private val plugin: KnockBackFFA,
    private val player: Player,
    private val boost: Boost
) {
    private val configProperties = mutableListOf<ConfigProperty>()

    init {
        findConfigurableProperties()
        createAndOpenGui()
    }

    private data class ConfigProperty(
        val field: Field,
        val annotation: ConfigurableProperty,
        val currentValue: Any?
    ) {
        val name: String? = field.name
        val configKey get() = annotation.configKey
        val type: Class<*> = field.type

        fun getTypeIcon(): Material {
            return when (type) {
                Int::class.java -> Material.CLOCK
                Double::class.java -> Material.WATER_BUCKET
                Boolean::class.java -> Material.LEVER
                String::class.java -> Material.PAPER
                Long::class.java -> Material.END_CRYSTAL
                Float::class.java -> Material.FEATHER
                Material::class.java -> Material.ITEM_FRAME
                Duration::class.java -> Material.SUNFLOWER
                Sound::class.java -> Material.NOTE_BLOCK
                Particle::class.java -> Material.ENDER_EYE
                else -> Material.BOOK
            }
        }

        fun getDisplayValue(): String {
            return currentValue?.toString() ?: "null"
        }

        fun getFormType(): FormType {
            return when (type) {
                Int::class.java -> FormType.INTEGER
                Double::class.java -> FormType.INTEGER
                Boolean::class.java -> FormType.BOOLEAN
                String::class.java -> FormType.STRING
                Long::class.java -> FormType.INTEGER
                Float::class.java -> FormType.INTEGER
                Material::class.java -> FormType.STRING
                Sound::class.java -> FormType.STRING
                Particle::class.java -> FormType.STRING
                else -> FormType.STRING
            }
        }
    }

    private fun findConfigurableProperties() {
        configProperties.clear()

        var currentClass: Class<*>? = boost.javaClass
        while (currentClass != null && currentClass != Object::class.java) {
            val fields = currentClass.declaredFields

            for (field in fields) {
                val annotation = field.getAnnotation(ConfigurableProperty::class.java) ?: continue

                field.isAccessible = true
                val value = try {
                    field.get(boost)
                } catch (_: Exception) {
                    plugin.logger.warning("Failed to access field ${field.name} in class ${currentClass.simpleName}")
                    null
                }

                configProperties.add(ConfigProperty(field, annotation, value))
            }

            currentClass = currentClass.superclass
        }
    }

    private fun createAndOpenGui() {
        val builder = PaginatedGuiBuilder()
            .title("<dark_gray>Special Properties <gray>» <white>${boost.name}".asMini())
            .size(GuiSize.ROW_SIX)
            .setBackground(Material.BLACK_STAINED_GLASS_PANE)

        configProperties.forEach { property ->
            builder.addItem(
                property.getTypeIcon(),
                "<yellow>${property.name}".asMini(),
                listOf(
                    "<gray>Config Key: <white>${property.configKey}".asMini(),
                    "<gray>Current Value: <white>${property.getDisplayValue()}".asMini(),
                    "<gray>Type: <white>${property.type.simpleName}".asMini(),
                    "<gray>Default value: <white>${property.annotation.defaultValue}".asMini(),
                    "".asMini(),
                    "<yellow>Click to edit!".asMini()
                ),
                1
            )
        }

        builder.onItemClick { clickedPlayer, _, index ->
            val property = configProperties.getOrNull(index) ?: return@onItemClick
            openEditForm(clickedPlayer, property)
        }

        builder.customizeGui { gui ->
            gui.item(Material.ARROW) {
                name("<yellow>« Back".asMini())
                slots(49)
                onClick { event ->
                    event.isCancelled = true
                    BoostEditor(plugin, player, boost)
                }
            }
        }

        builder.build().open(player)
    }

    private fun openEditForm(player: Player, property: ConfigProperty) {
        if (property.type == Boolean::class.java) {
            try {
                val currentValue = property.currentValue as? Boolean == true
                val newValue = !currentValue

                plugin.boostManager.updateBoostConfig(boost.id, property.configKey, newValue)
                player.message("<green>Toggled ${property.name} to: $newValue")
                player.playSound(player.location, Sound.BLOCK_LEVER_CLICK, 1f, 1f)

                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    BoostSpecialPropertiesEditor(plugin, player, boost)
                }, 2L)
            } catch (e: Exception) {
                player.message("<red>Error toggling boolean property: ${e.message}")
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            }
            return
        }

        val formType = property.getFormType()
        val maxLength = if (property.type == String::class.java) 100 else 10

        val form = Form("Edit ${property.name}", formType, maxLength) { p, response ->
            try {
                val convertedValue = when (property.type) {
                    Int::class.java -> (response as Number).toInt()
                    Double::class.java -> (response as Number).toDouble()
                    String::class.java -> response as String
                    Long::class.java -> (response as Number).toLong()
                    Float::class.java -> (response as Number).toFloat()
                    Material::class.java -> Material.valueOf((response as String).uppercase())
                    else -> response
                }

                plugin.boostManager.updateBoostConfig(boost.id, property.configKey, convertedValue)
                p.message("<green>Updated ${property.name} to: $convertedValue")
                p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)

                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    BoostSpecialPropertiesEditor(plugin, p, boost)
                }, 2L)
            } catch (e: Exception) {
                p.message("<red>Error updating property: ${e.message}")
                p.playSound(p.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                openEditForm(p, property)
            }
        }
        form.show(player)
    }
}