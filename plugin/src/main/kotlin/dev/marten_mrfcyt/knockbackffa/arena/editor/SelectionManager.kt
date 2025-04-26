package dev.marten_mrfcyt.knockbackffa.arena.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
import mlib.api.utilities.action
import mlib.api.utilities.asMini
import mlib.api.utilities.message
import mlib.api.utilities.sendMini
import org.bukkit.Bukkit
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
    private var worldEditAvailable = false

    init {

        worldEditAvailable = Bukkit.getPluginManager().getPlugin("WorldEdit") != null
    }

    companion object {
        const val WAND_KEY = "knockbackffa:selection_wand"
        val WAND_MATERIAL = Material.GOLDEN_AXE
    }

    fun startSelection(player: Player, mode: SelectionMode): Boolean {
        playersInSelectionMode[player.uniqueId] = mode

        when (mode) {
            SelectionMode.SPAWN_REGION -> {
                if (worldEditAvailable) {
                    player.sendMini(translate("arena.editor.worldedit_available"))

                    if (!hasWorldEditWand(player)) {
                        player.sendMini(translate("arena.editor.worldedit_wand_given"))
                        giveWorldEditWand(player)
                    }
                } else {
                    player.sendMini(translate("arena.editor.using_custom_wand"))
                    giveCustomSelectionWand(player)
                }
                player.sendMini(translate("arena.editor.region_instructions"))
            }
            SelectionMode.SPAWNPOINT -> {
                if (!worldEditAvailable) {
                    removeCustomSelectionWand(player)
                }
                player.sendMini(translate("arena.editor.spawnpoint_instructions"))
            }
        }

        return true
    }

    fun getSelection(player: Player): Selection? {
        val selection = playerSelections[player.uniqueId]
        println("[KnockBackFFA-DEBUG] getSelection for ${player.name}: ${selection?.firstPosition != null} ${selection?.secondPosition != null}")
        return selection
    }

    fun clearSelection(player: Player) {

        if (!worldEditAvailable && isInSelectionMode(player)) {
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

    fun isWorldEditAvailable(): Boolean {
        return worldEditAvailable
    }

    private fun hasWorldEditWand(player: Player): Boolean {
        if (!worldEditAvailable) return false

        return player.inventory.contents.any {
            it?.type == Material.WOODEN_AXE
        }
    }

    private fun giveWorldEditWand(player: Player) {
        if (!worldEditAvailable) return

        Bukkit.dispatchCommand(player, "worldedit:wand")
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
            println("[KnockBackFFA-DEBUG] Set secondPosition for ${player.name} at ${location.x}, ${location.y}, ${location.z}")
            player.action(translate("arena.editor.second_position_set",
                "x" to "%.1f".format(location.x),
                "y" to "%.1f".format(location.y),
                "z" to "%.1f".format(location.z)
            ))
        } else {
            selection.firstPosition = location
            println("[KnockBackFFA-DEBUG] Set firstPosition for ${player.name} at ${location.x}, ${location.y}, ${location.z}")
            player.action(translate("arena.editor.first_position_set",
                "x" to "%.1f".format(location.x),
                "y" to "%.1f".format(location.y),
                "z" to "%.1f".format(location.z)
            ))
        }

        if (selection.isComplete()) {
            println("[KnockBackFFA-DEBUG] Selection is complete for ${player.name}")
            player.action(translate("arena.editor.complete"))

            player.message("<green><bold><click:run_command:/kbffa arena selection_complete>[ Set Spawn Region ]</click></bold></green>")
        }
    }

    fun getWorldEditSelection(player: Player): Pair<Location, Location>? {
        if (!worldEditAvailable) {
            println("[KnockBackFFA-DEBUG] WorldEdit not available for ${player.name}")
            return null
        }

        try {
            println("[KnockBackFFA-DEBUG] Attempting to get WorldEdit editor for ${player.name}")

            val worldEditPlugin = Bukkit.getPluginManager().getPlugin("WorldEdit")
            val worldEditClass = Class.forName("com.sk89q.worldedit.bukkit.WorldEditPlugin")

            val getSessionMethod = worldEditClass.getMethod("getSession", Player::class.java)
            val session = getSessionMethod.invoke(worldEditPlugin, player)

            val sessionClass = session.javaClass
            val getSelectionMethod = sessionClass.getMethod("getSelection", Class.forName("com.sk89q.worldedit.world.World"))

            val bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter")
            val adaptMethod = bukkitAdapterClass.getMethod("adapt", org.bukkit.World::class.java)
            val weWorld = adaptMethod.invoke(null, player.world)

            val region = getSelectionMethod.invoke(session, weWorld)
            if (region == null) {
                println("[KnockBackFFA-DEBUG] No WorldEdit region found for ${player.name}")
                return null
            }

            val regionClass = region.javaClass
            val getMinimumPointMethod = regionClass.getMethod("getMinimumPoint")
            val getMaximumPointMethod = regionClass.getMethod("getMaximumPoint")

            val minPoint = getMinimumPointMethod.invoke(region)
            val maxPoint = getMaximumPointMethod.invoke(region)

            val blockVectorToLocationMethod = bukkitAdapterClass.getDeclaredMethod("adapt", player.world.javaClass, Class.forName("com.sk89q.worldedit.math.BlockVector3"))

            val minLoc = blockVectorToLocationMethod.invoke(null, player.world, minPoint) as Location
            val maxLoc = blockVectorToLocationMethod.invoke(null, player.world, maxPoint) as Location

            println("[KnockBackFFA-DEBUG] Successfully got WorldEdit editor for ${player.name}: ${minLoc.x},${minLoc.y},${minLoc.z} to ${maxLoc.x},${maxLoc.y},${maxLoc.z}")
            return Pair(minLoc, maxLoc)
        } catch (e: Exception) {

            println("[KnockBackFFA-DEBUG] Failed to get WorldEdit editor for ${player.name}: ${e.message}")
            plugin.logger.warning("Failed to get WorldEdit editor: ${e.message}")
            e.printStackTrace()
            return null
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