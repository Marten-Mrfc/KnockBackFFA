package dev.marten_mrfcyt.knockbackffa.arena.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
import mlib.api.utilities.action
import mlib.api.utilities.asMini
import mlib.api.utilities.debug
import mlib.api.utilities.message
import mlib.api.utilities.sendMini
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*

class SelectionManager(private val plugin: KnockBackFFA) : Listener {

    private val playerSelections = mutableMapOf<UUID, Selection>()
    private val playersInSelectionMode = mutableMapOf<UUID, SelectionMode>()

    companion object {
        val WAND_MATERIAL = Material.GOLDEN_AXE
    }

    fun startSelection(player: Player, mode: SelectionMode): Boolean {
        playersInSelectionMode[player.uniqueId] = mode

        when (mode) {
            SelectionMode.SPAWN_REGION -> {
                player.sendMini(translate("arena.editor.using_custom_wand"))
                giveCustomSelectionWand(player)
                player.sendMini(translate("arena.editor.region_instructions"))
            }
            SelectionMode.SPAWNPOINT -> {
                removeCustomSelectionWand(player)
                player.sendMini(translate("arena.editor.spawnpoint_instructions"))
            }
        }

        return true
    }

    fun getSelection(player: Player): Selection? {
        val selection = playerSelections[player.uniqueId]
        
        // Use the plugin's template debug method
        plugin.debug("getSelection called for ${player.name}: $selection")
        
        return selection
    }

    fun clearSelection(player: Player) {
        if (isInSelectionMode(player)) {
            removeCustomSelectionWand(player)
        }

        playerSelections.remove(player.uniqueId)
        playersInSelectionMode.remove(player.uniqueId)
    }

    fun isInSelectionMode(player: Player): Boolean {
        return playersInSelectionMode.containsKey(player.uniqueId)
    }

    fun getSelectionMode(player: Player): SelectionMode? {
        return playersInSelectionMode[player.uniqueId]
    }

    private fun createCustomSelectionWand(): ItemStack {
        val wand = ItemStack(WAND_MATERIAL)
        val meta = wand.itemMeta

        meta.displayName("<gold>KnockBackFFA Selection Wand".asMini())
        meta.lore(listOf(
            "<gray>Left-click to select first position".asMini(),
            "<gray>Right-click to select second position".asMini()
        ))

        meta.persistentDataContainer.set(
            org.bukkit.NamespacedKey(plugin, "selection_wand"),
            PersistentDataType.STRING,
            "true"
        )

        wand.itemMeta = meta
        return wand
    }

    private fun giveCustomSelectionWand(player: Player) {
        player.inventory.addItem(createCustomSelectionWand())
    }

    private fun removeCustomSelectionWand(player: Player) {
        val inventory = player.inventory
        val itemsToRemove = mutableListOf<ItemStack>()

        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i) ?: continue
            if (isCustomSelectionWand(item)) {
                itemsToRemove.add(item)
            }
        }

        for (item in itemsToRemove) {
            inventory.remove(item)
        }
    }

    private fun isCustomSelectionWand(item: ItemStack?): Boolean {
        if (item == null) return false

        val meta = item.itemMeta ?: return false
        val container = meta.persistentDataContainer

        return container.has(org.bukkit.NamespacedKey(plugin, "selection_wand"), PersistentDataType.STRING)
    }

    private fun updateSelection(player: Player, location: Location, isSecondPosition: Boolean) {
        val selection = playerSelections.getOrPut(player.uniqueId) { Selection() }

        if (isSecondPosition) {
            selection.secondPosition = location
            plugin.debug("Set secondPosition for ${player.name} at x=${location.x}, y=${location.y}, z=${location.z}")
            player.action(translate("arena.editor.second_position_set",
                "x" to "%.1f".format(location.x),
                "y" to "%.1f".format(location.y),
                "z" to "%.1f".format(location.z)
            ))
        } else {
            selection.firstPosition = location
            plugin.debug("Set firstPosition for ${player.name} at x=${location.x}, y=${location.y}, z=${location.z}")
            player.action(translate("arena.editor.first_position_set",
                "x" to "%.1f".format(location.x),
                "y" to "%.1f".format(location.y),
                "z" to "%.1f".format(location.z)
            ))
        }

        if (selection.isComplete()) {
            plugin.debug("Selection complete for ${player.name}: firstPos=${selection.firstPosition}, secondPos=${selection.secondPosition}")
            player.action(translate("arena.editor.complete"))

            player.message("<green><bold><click:run_command:/kbffa arena selection_complete>[ Set Spawn Region ]</click></bold></green>")
        }
    }



    fun setSpawnpoint(player: Player, location: Location) {
        val selection = playerSelections.getOrPut(player.uniqueId) { Selection() }
        selection.spawnpoint = location

        player.sendMini(translate("arena.editor.spawnpoint_set",
            "x" to "%.1f".format(location.x),
            "y" to "%.1f".format(location.y),
            "z" to "%.1f".format(location.z)
        ))

        playersInSelectionMode.remove(player.uniqueId)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return

        if (!isInSelectionMode(player)) return

        val mode = getSelectionMode(player) ?: return

        when (mode) {
            SelectionMode.SPAWN_REGION -> {
                if (isCustomSelectionWand(item)) {
                    when (event.action) {
                        Action.LEFT_CLICK_BLOCK -> {
                            event.isCancelled = true
                            updateSelection(player, event.clickedBlock?.location ?: return, false)
                        }
                        Action.RIGHT_CLICK_BLOCK -> {
                            event.isCancelled = true
                            updateSelection(player, event.clickedBlock?.location ?: return, true)
                        }
                        else -> {}
                    }
                }
            }
            SelectionMode.SPAWNPOINT -> {
                if (event.action == Action.RIGHT_CLICK_BLOCK) {
                    event.isCancelled = true
                    setSpawnpoint(player, player.location)
                }
            }
        }
    }
}

data class Selection(
    var firstPosition: Location? = null,
    var secondPosition: Location? = null,
    var spawnpoint: Location? = null
) {
    fun isComplete(): Boolean {
        return firstPosition != null && secondPosition != null
    }

    fun getRegionIfComplete(): Pair<Location, Location>? {
        if (!isComplete()) return null
        return Pair(firstPosition!!, secondPosition!!)
    }
}

enum class SelectionMode {
    SPAWN_REGION,
    SPAWNPOINT
}