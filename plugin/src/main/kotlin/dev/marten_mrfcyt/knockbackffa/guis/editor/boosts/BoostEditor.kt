package dev.marten_mrfcyt.knockbackffa.guis.editor.boosts

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.boosts.models.Boost
import mlib.api.forms.Form
import mlib.api.forms.FormType
import mlib.api.gui.GuiSize
import mlib.api.gui.types.StandardGui
import mlib.api.utilities.asMini
import mlib.api.utilities.message
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.time.Duration

class BoostEditor(private val plugin: KnockBackFFA, private val player: Player, private val boost: Boost) {
    private val gui: StandardGui = StandardGui("<dark_gray>Boost Editor <gray>» <white>${boost.name}".asMini(), GuiSize.ROW_FOUR)

    init {
        setupGui()
        gui.open(player)
    }

    private fun setupGui() {
        gui.fill(Material.BLACK_STAINED_GLASS_PANE) {}

        val enabledMaterial = if (boost.enabled) Material.LIME_DYE else Material.GRAY_DYE
        val enabledStatus = if (boost.enabled) "<green>Enabled" else "<red>Disabled"

        gui.item(enabledMaterial) {
            name("<yellow>Toggle Status".asMini())
            description(listOf(
                "<gray>Current: $enabledStatus".asMini(),
                "".asMini(),
                "<white>Click to toggle!".asMini()
            ))
            slots(10)
            onClick { event ->
                event.isCancelled = true
                toggleEnabled()
            }
        }

        gui.item(Material.NAME_TAG) {
            name("<yellow>Edit Name".asMini())
            description(listOf(
                "<gray>Current: <white>${boost.name}".asMini(),
                "".asMini(),
                "<white>Click to edit!".asMini()
            ))
            slots(11)
            onClick { event ->
                event.isCancelled = true
                editName()
            }
        }

        gui.item(Material.BOOK) {
            name("<yellow>Edit Description".asMini())
            description(listOf(
                "<gray>Current:".asMini(),
                *boost.description.map { "<white>$it".asMini() }.toTypedArray(),
                "".asMini(),
                "<white>Click to edit!".asMini()
            ))
            slots(12)
            onClick { event ->
                event.isCancelled = true
                editDescription()
            }
        }

        gui.item(Material.PAINTING) {
            name("<yellow>Edit Icon".asMini())
            description(listOf(
                "<gray>Current: <white>${boost.icon.name}".asMini(),
                "".asMini(),
                "<white>Click to edit!".asMini()
            ))
            slots(13)
            onClick { event ->
                event.isCancelled = true
                editIcon()
            }
        }

        gui.item(Material.GOLD_INGOT) {
            name("<yellow>Edit Price".asMini())
            description(listOf(
                "<gray>Current: <white>${boost.price} coins".asMini(),
                "".asMini(),
                "<white>Click to edit!".asMini()
            ))
            slots(14)
            onClick { event ->
                event.isCancelled = true
                editPrice()
            }
        }

        if (boost.isTimed()) {
            val duration = boost.getDuration() ?: Duration.ZERO
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60
            val durationStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

            gui.item(Material.CLOCK) {
                name("<yellow>Edit Duration".asMini())
                description(listOf(
                    "<gray>Current: <white>$durationStr".asMini(),
                    "".asMini(),
                    "<white>Click to edit!".asMini()
                ))
                slots(15)
                onClick { event ->
                    event.isCancelled = true
                    editDuration()
                }
            }
        }

        gui.item(Material.REDSTONE) {
            name("<yellow>Special Properties".asMini())
            description(listOf(
                "<gray>Edit boost-specific properties".asMini(),
                "".asMini(),
                "<white>Click to open!".asMini()
            ))
            slots(16)
            onClick { event ->
                event.isCancelled = true
                BoostSpecialPropertiesEditor(plugin, player, boost)
            }
        }

        gui.item(boost.icon) {
            name("<yellow>${boost.name}".asMini())
            description(
                listOf("<gold>ID: <gray>${boost.id}".asMini()) +
                        boost.description.map { "<white>$it".asMini() } +
                        listOf(
                            "".asMini(),
                            "<gray>Price: <white>${boost.price} coins".asMini(),
                            if (boost.isTimed()) "<gray>Duration: <white>${formatDuration(boost.getDuration())}".asMini() else "<gray>Type: <white>Permanent".asMini()
                        )
            )
            slots(22)
        }

        gui.item(Material.EXPERIENCE_BOTTLE) {
            name("<green>Test Boost".asMini())
            description(listOf(
                "<gray>Apply this boost to yourself".asMini(),
                "<gray>to test its functionality.".asMini(),
                "".asMini(),
                "<white>Click to test!".asMini()
            ))
            slots(20)
            onClick { event ->
                event.isCancelled = true
                testBoost()
            }
        }

        gui.item(Material.ARROW) {
            name("<yellow>« Back".asMini())
            slots(31)
            onClick { event ->
                event.isCancelled = true
                BoostSelector(plugin, player)
            }
        }
    }

    private fun formatDuration(duration: Duration?): String {
        if (duration == null) return "N/A"
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private fun toggleEnabled() {
        val newState = !boost.enabled
        plugin.boostManager.updateBoostConfig(boost.id, "enabled", newState)

        val statusMsg = if (newState) "<green>Enabled" else "<red>Disabled"
        player.message("<green>Boost ${boost.name} is now $statusMsg")
        player.playSound(player.location, Sound.BLOCK_LEVER_CLICK, 1f, 1f)

        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            val updatedBoost = plugin.boostManager.getBoost(boost.id)
            BoostEditor(plugin, player, updatedBoost)
        }, 2L)
    }

    private fun testBoost() {
        player.closeInventory()

        val result = boost.apply(player)
        if (result) {
            player.message("<green>Successfully applied ${boost.name} for testing!")
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        } else {
            player.message("<red>Could not apply the boost. Is it configured correctly?")
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
        }

        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            boost.remove(player)
            BoostEditor(plugin, player, boost)
        }, 40L)
    }

    private fun editName() {
        val form = Form("Enter new name for ${boost.name}", FormType.STRING, 30) { p, response ->
            val newName = response as String
            plugin.boostManager.updateBoostConfig(boost.id, "name", newName)

            p.message("<green>Name updated to: $newName")
            p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)

            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                val updatedBoost = plugin.boostManager.getBoost(boost.id)
                BoostEditor(plugin, p, updatedBoost)
            }, 2L)
        }
        form.show(player)
    }

    private fun editDescription() {
        val form = Form("Enter new description for ${boost.name}\nUse \\n for new lines", FormType.STRING, 100) { p, response ->
            val newDesc = (response as String).split("\\n")
            plugin.boostManager.updateBoostConfig(boost.id, "description", newDesc)
            p.message("<green>Description updated!")
            p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)

            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                val updatedBoost = plugin.boostManager.getBoost(boost.id)
                BoostEditor(plugin, p, updatedBoost)
            }, 2L)
        }
        form.show(player)
    }

    private fun editIcon() {
        val form = Form("Enter material name for ${boost.name}", FormType.STRING, 30) { p, response ->
            try {
                val material = Material.valueOf(response.toString().uppercase())
                plugin.boostManager.updateBoostConfig(boost.id, "icon", material.name)
                p.message("<green>Icon updated to: ${material.name}")
                p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)

                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    val updatedBoost = plugin.boostManager.getBoost(boost.id)
                    BoostEditor(plugin, p, updatedBoost)
                }, 2L)
            } catch (_: IllegalArgumentException) {
                p.message("<red>Invalid material name!")
                p.playSound(p.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                editIcon()
            }
        }
        form.show(player)
    }

    private fun editPrice() {
        val form = Form("Enter new price for ${boost.name}", FormType.INTEGER, 10) { p, response ->
            val newPrice = response as Int
            if (newPrice < 0) {
                p.message("<red>Price cannot be negative!")
                p.playSound(p.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                editPrice()
                return@Form
            }
            plugin.boostManager.updateBoostConfig(boost.id, "price", newPrice)
            p.message("<green>Price updated to: $newPrice coins")
            p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)

            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                val updatedBoost = plugin.boostManager.getBoost(boost.id)
                BoostEditor(plugin, p, updatedBoost)
            }, 2L)
        }
        form.show(player)
    }

    private fun editDuration() {
        val form = Form("Enter new duration in minutes for ${boost.name}", FormType.INTEGER, 10) { p, response ->
            val minutes = response as Int
            if (minutes <= 0) {
                p.message("<red>Duration must be greater than 0!")
                p.playSound(p.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                editDuration()
                return@Form
            }

            val durationField = when {
                boost.javaClass.name.contains("EffectBoost") -> "effectDuration"
                boost.javaClass.name.contains("StatsBoost") -> "effectDuration"
                else -> "effectDuration"
            }

            plugin.boostManager.updateBoostConfig(boost.id, durationField, minutes)
            p.message("<green>Duration updated to: $minutes minutes")
            p.playSound(p.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)

            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                val updatedBoost = plugin.boostManager.getBoost(boost.id)
                BoostEditor(plugin, p, updatedBoost)
            }, 2L)
        }
        form.show(player)
    }
}