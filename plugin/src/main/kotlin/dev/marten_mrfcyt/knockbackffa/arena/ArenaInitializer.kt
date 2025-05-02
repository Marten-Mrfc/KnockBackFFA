package dev.marten_mrfcyt.knockbackffa.arena

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.arena.utils.DeathBlock
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
import mlib.api.architecture.extensions.registerEvents
import mlib.api.utilities.*
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.time.Instant

class ArenaInitializer(private val plugin: KnockBackFFA) {

    fun initializeArenas(mapDuration: Int) {
        plugin.logger.info(translate("plugin.starting_arena_handler"))

        plugin.arenaHandler.loadArenas()

        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            KnockBackFFA.lastSwitchTime = Instant.now()
            KnockBackFFA.nextSwitchTime = KnockBackFFA.lastSwitchTime.plusSeconds(mapDuration.toLong())
            plugin.arenaHandler.switchArena()
        }, 0L, mapDuration * 20L)

        plugin.logger.info(translate("plugin.arena_handler_ready", "duration" to mapDuration))
    }

    fun setupArenaSystem() {

        plugin.server.pluginManager.registerEvents(plugin.selectionManager, plugin)

        plugin.server.pluginManager.registerEvents(plugin.arenaCreationHandler, plugin)

        plugin.registerEvents(DeathBlock())

        debug("Arena system initialized - registered event listeners")
    }
}