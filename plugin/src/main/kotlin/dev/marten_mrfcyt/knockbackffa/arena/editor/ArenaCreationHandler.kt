package dev.marten_mrfcyt.knockbackffa.arena.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.arena.utils.ArenaSetting
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import mlib.api.utilities.action
import mlib.api.utilities.debug
import mlib.api.utilities.sendMini
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Listener

class ArenaCreationHandler(private val plugin: KnockBackFFA) : Listener {
    private val miniMessage = MiniMessage.miniMessage()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }    fun startArenaCreation(player: Player, name: String, killBlock: Material) {
        debug(plugin, "Starting arena creation for ${player.name}: arena=${name}, killBlock=${killBlock.name}")

        // Initialize with default settings
        val defaultSettings = try {
            debug(plugin, "Attempting to create default settings...")
            ArenaSetting.createDefaultSettings().also { 
                debug(plugin, "Successfully created ${it.size} default settings") 
            }
        } catch (e: Exception) {
            debug(plugin, "Error creating default settings: ${e.message}")
            e.printStackTrace() // Print stack trace for better debugging
            mutableMapOf<String, Any>() // Empty map as fallback
        }
        
        debug(plugin, "Initializing arena with default settings: $defaultSettings")
        
        plugin.arenaHandler.startArenaCreation(player, name, killBlock, defaultSettings)

        plugin.selectionManager.startSelection(player, SelectionMode.SPAWN_REGION)

        player.action(
            TranslationManager.Companion.translate(
                "arena.create.started",
                "arena_name" to name,
                "kill_block" to killBlock.name
            )
        )

        sendSelectionCompletionButton(player)
        debug(plugin, "Arena creation session initialized for ${player.name}")
    }

    private fun sendSelectionCompletionButton(player: Player) {
        val message = miniMessage.deserialize(
            "<dark_gray>━━━━━━━━━━ <gold>Arena Creation</gold> ━━━━━━━━━━</dark_gray>\n" +
                    "<gray>Select two points to define the spawn region:</gray>\n" +
                    "<yellow>• Left-click: <white>Set first point</white></yellow>\n" +
                    "<yellow>• Right-click: <white>Set second point</white></yellow>\n" +
                    "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        )

        player.sendMessage(message)
    }

    private fun sendSpawnpointButton(player: Player) {
        val message = miniMessage.deserialize(
            "<dark_gray>━━━━━━━━━━ <gold>Arena Creation</gold> ━━━━━━━━━━</dark_gray>\n" +
                    "<gray>Spawn region set successfully!</gray>\n" +
                    "<gray>Now go to where players should spawn and click:</gray>\n" +
                    "<green><bold><click:run_command:/kbffa arena zspawnpoint_complete>[ Set Spawnpoint ]</click></bold></green>\n" +
                    "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        )

        player.sendMessage(message)
    }

    private fun sendCompletionMessage(player: Player) {
        val session = plugin.arenaHandler.getArenaCreationSession(player)
        val arenaName = session?.name ?: "unknown"

        val message = miniMessage.deserialize(
            "<dark_gray>━━━━━━━━━━ <gold>Arena Creation</gold> ━━━━━━━━━━</dark_gray>\n" +
                    "<gray>Spawnpoint set successfully!</gray>\n" +
                    "<gray>Arena <gold>$arenaName</gold> is ready to be created:</gray>\n" +
                    "<green><bold><click:run_command:/kbffa arena zconfirm>[ Create Arena ]</click></bold></green> " +
                    "<red><bold><click:run_command:/kbffa arena zcancel>[ Cancel ]</click></bold></red>\n" +
                    "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        )

        player.sendMessage(message)
    }

    fun handleSelectionComplete(player: Player) {
        val session = plugin.arenaHandler.getArenaCreationSession(player) ?: run {
            player.sendMini(TranslationManager.Companion.translate("arena.create.no_session"))
            return
        }

        val selection = plugin.selectionManager.getSelection(player)

        if (selection?.isComplete() == true) {

            session.spawnRegion = selection.getRegionIfComplete()
            session.nextStep()

            plugin.selectionManager.clearSelection(player)
            plugin.selectionManager.startSelection(player, SelectionMode.SPAWNPOINT)
            sendSpawnpointButton(player)
        } else {

            player.sendMini(TranslationManager.Companion.translate("arena.create.selection_incomplete"))
            sendSelectionCompletionButton(player)
        }
    }

    fun handleSpawnpointComplete(player: Player) {
        val session = plugin.arenaHandler.getArenaCreationSession(player) ?: run {
            player.sendMini(TranslationManager.Companion.translate("arena.create.no_session"))
            return
        }

        session.spawnpoint = player.location
        session.nextStep()

        plugin.selectionManager.clearSelection(player)

        sendCompletionMessage(player)
    }

    fun completeArenaCreation(player: Player) {
        val session = plugin.arenaHandler.getArenaCreationSession(player)
        if (session == null) {
            player.sendMini(TranslationManager.Companion.translate("arena.create.no_session"))
            return
        }

        if (plugin.arenaHandler.completeArenaCreation(player)) {

            val message = miniMessage.deserialize(
                "<dark_gray>━━━━━━━━━━ <gold>Arena Created</gold> ━━━━━━━━━━</dark_gray>\n" +
                        "<green>Arena <gold>${session.name}</gold> has been successfully created!</green>\n" +
                        "<gray>Arena details:</gray>\n" +
                        "<yellow>• <white>Spawn Region: Set</white></yellow>\n" +
                        "<yellow>• <white>Spawnpoint: Set</white></yellow>\n" +
                        "<yellow>• <white>Kill Block: ${session.killBlock}</white></yellow>\n" +
                        "<gray>Next steps: Use <gold>/kbffa arena list</gold> to see all arenas.</gray>\n" +
                        "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
            )

            player.sendMessage(message)
        } else {

            val messageBuilder = StringBuilder()
            messageBuilder.append("<dark_gray>━━━━━━━━━━ <red>Arena Creation Failed</red> ━━━━━━━━━━</dark_gray>\n")
            messageBuilder.append("<red>Failed to create arena ${session.name}.</red>\n")

            if (!session.isComplete()) {
                messageBuilder.append("<red>Arena creation is incomplete:</red>\n")
                if (session.spawnRegion == null) {
                    messageBuilder.append("<red>• Missing spawn region</red>\n")
                }
                if (session.spawnpoint == null) {
                    messageBuilder.append("<red>• Missing spawnpoint</red>\n")
                }
            }

            messageBuilder.append("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>")

            player.sendMessage(miniMessage.deserialize(messageBuilder.toString()))
        }
    }

    fun cancelArenaCreation(player: Player) {
        val session = plugin.arenaHandler.getArenaCreationSession(player)
        val arenaName = session?.name ?: "unknown"

        plugin.arenaHandler.cancelArenaCreation(player)
        plugin.selectionManager.clearSelection(player)

        val message = miniMessage.deserialize(
            "<dark_gray>━━━━━━━━━━ <gold>Arena Creation</gold> ━━━━━━━━━━</dark_gray>\n" +
                    "<red>Arena creation for <gold>$arenaName</gold> has been cancelled.</red>\n" +
                    "<gray>You can start a new arena creation with:</gray>\n" +
                    "<gold>/kbffa arena create <name> <killBlock></gold>\n" +
                    "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        )

        player.sendMessage(message)
    }
}