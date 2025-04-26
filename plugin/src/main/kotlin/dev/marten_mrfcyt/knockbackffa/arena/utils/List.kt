package dev.marten_mrfcyt.knockbackffa.arena.utils

import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
import mlib.api.utilities.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File

fun Plugin.listArena(source: CommandSender, page: Int = 1) {
    if (source is Player) {
        val config = File("$dataFolder/arena.yml")
        if (!config.exists()) {
            showNoArenasMessage(source)
            return
        }

        val arenaConfig = YamlConfiguration.loadConfiguration(config)
        val arenas = arenaConfig.getConfigurationSection("arenas")?.getKeys(false)?.toList()
        if (arenas.isNullOrEmpty()) {
            showNoArenasMessage(source)
            return
        }

        val sortedArenas = arenas.sorted()

        val arenasPerPage = 5
        val totalPages = (sortedArenas.size + arenasPerPage - 1) / arenasPerPage
        val safePageNumber = page.coerceIn(1, maxOf(1, totalPages))

        val startIndex = (safePageNumber - 1) * arenasPerPage
        val endIndex = minOf(startIndex + arenasPerPage, sortedArenas.size)
        val arenasToShow = sortedArenas.subList(startIndex, endIndex)

        val miniMessage = MiniMessage.miniMessage()

        val resolvers = TagResolver.builder()
            .resolver(Placeholder.parsed("total", sortedArenas.size.toString()))
            .resolver(Placeholder.parsed("current_page", safePageNumber.toString()))
            .resolver(Placeholder.parsed("total_pages", totalPages.toString()))
            .build()

        val header = miniMessage.deserialize(
            "<dark_gray>╔══════ <gradient:#FFD700:#FFA500>Available Arenas</gradient> <gray>(<gold><total></gold>)</gray> ══════╗</dark_gray>",
            resolvers
        )
        source.sendMessage(header)

        val subheader = miniMessage.deserialize(
            "<dark_gray>║</dark_gray> <gray>Page <gold><current_page></gold>/<total_pages></gray> " +
                    "",
            resolvers
        )
        source.sendMessage(subheader)

        arenasToShow.forEach { arenaName ->
            val arenaComponent = createArenaComponent(miniMessage, arenaConfig, arenaName)
            source.sendMessage(arenaComponent)
        }

        val navigationFooter = createNavigationFooter(miniMessage, safePageNumber, totalPages)
        source.sendMessage(navigationFooter)

        val footer = miniMessage.deserialize("<dark_gray>╚════════════════════════╝</dark_gray>")
        source.sendMessage(footer)
    } else {
        source.message(translate("error.player_only"))
    }
}

private fun showNoArenasMessage(player: Player) {
    val miniMessage = MiniMessage.miniMessage()
    val header = miniMessage.deserialize("<dark_gray>╔══════ <gradient:#FFD700:#FFA500>No Arenas Available</gradient> ══════╗</dark_gray>")
    val message = miniMessage.deserialize(
        "<dark_gray>║</dark_gray> <gray>There are no arenas set up yet.</gray>\n" +
                "<dark_gray>║</dark_gray> <gray>Create one with:</gray>\n" +
                "<dark_gray>║</dark_gray> <yellow><click:suggest_command:/kbffa arena create >/kbffa arena create <name> <killBlock></click></yellow>"
    )
    val footer = miniMessage.deserialize("<dark_gray>╚════════════════════════╝</dark_gray>")

    player.sendMessage(header)
    player.sendMessage(message)
    player.sendMessage(footer)
}

private fun createArenaComponent(miniMessage: MiniMessage, arenaConfig: YamlConfiguration, arenaName: String): Component {
    val arenaSection = arenaConfig.getConfigurationSection("arenas.$arenaName") ?: return miniMessage.deserialize("<dark_gray>║</dark_gray> <red>Error loading arena $arenaName</red>")

    val killBlock = arenaSection.getString("killBlock") ?: "VOID"

    val spawnRegionSection = arenaSection.getConfigurationSection("spawnRegion")
    val worldName = spawnRegionSection?.getString("world") ?: "unknown"

    val minSection = spawnRegionSection?.getConfigurationSection("min")
    val maxSection = spawnRegionSection?.getConfigurationSection("max")
    val regionSize = if (minSection != null && maxSection != null) {
        val width = maxSection.getDouble("x") - minSection.getDouble("x")
        val height = maxSection.getDouble("y") - minSection.getDouble("y")
        val depth = maxSection.getDouble("z") - minSection.getDouble("z")
        String.format("%.1fx%.1fx%.1f", width, height, depth)
    } else {
        "unknown"
    }

    val spawnpointSection = arenaSection.getConfigurationSection("spawnpoint")
    val spawnX = "%.1f".format(spawnpointSection?.getDouble("x") ?: 0.0)
    val spawnY = "%.1f".format(spawnpointSection?.getDouble("y") ?: 0.0)
    val spawnZ = "%.1f".format(spawnpointSection?.getDouble("z") ?: 0.0)

    val hoverInfo = "<gray>Arena Information:</gray>\n" +
            "<gray>• World: <white>$worldName</white></gray>\n" +
            "<gray>• Kill Block: <white>$killBlock</white></gray>\n" +
            "<gray>• Spawn Size: <white>$regionSize</white></gray>\n" +
            "<gray>• Spawnpoint: <white>$spawnX, $spawnY, $spawnZ</white></gray>"

    return miniMessage.deserialize(
        "<dark_gray>║</dark_gray> <yellow>• <gradient:#FFD700:#FFA500>$arenaName</gradient> " +
                "<hover:show_text:'$hoverInfo'>" +
                "<gray><underlined>Details</underlined></gray></hover> " +
                "<hover:show_text:'<yellow>Click to edit this arena'><yellow>[<click:run_command:/kbffa arena settings $arenaName>" +
                "<transition:gold:yellow>Settings</transition></click>]</yellow></hover> "
    )
}

private fun createNavigationFooter(miniMessage: MiniMessage, currentPage: Int, totalPages: Int): Component {
    val hasPrevious = currentPage > 1
    val hasNext = currentPage < totalPages

    val previousButton = if (hasPrevious) {
        "<hover:show_text:'<gray>Go to previous page</gray>'><green>[<click:run_command:/kbffa arena list ${currentPage-1}>" +
                "<transition:green:aqua>Previous</transition></click>]</green></hover>"
    } else {
        "<dark_gray>[Previous]</dark_gray>"
    }

    val nextButton = if (hasNext) {
        "<hover:show_text:'<gray>Go to next page</gray>'><green>[<click:run_command:/kbffa arena list ${currentPage+1}>" +
                "<transition:green:aqua>Next</transition></click>]</green></hover>"
    } else {
        "<dark_gray>[Next]</dark_gray>"
    }

    return miniMessage.deserialize(
        "<dark_gray>║</dark_gray> $previousButton <gray>Page <gold>$currentPage</gold>/<gold>$totalPages</gold></gray> $nextButton"
    )
}