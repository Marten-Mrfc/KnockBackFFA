package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.arena.currentArena
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
import mlib.api.utilities.asMini
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant
import java.util.*

class BossBarHandler(private val plugin: KnockBackFFA) {
    private val playerBossBars = mutableMapOf<UUID, BossBar>()

    fun showBossBar(player: Player) {
        if (!plugin.config.getBoolean("bossbar.enabled", true)) return

        val bossBar = BossBar.bossBar(
            formatBossBarText(currentArena?.name ?: "None"),
            1.0f,
            BossBar.Color.GREEN,
            BossBar.Overlay.PROGRESS
        )

        player.showBossBar(bossBar)
        playerBossBars[player.uniqueId] = bossBar
        startUpdateTask(player)
    }

    private fun startUpdateTask(player: Player) {
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (!player.isOnline) return@Runnable
            updateBossBarProgress(player)
        }, 0L, 20L)
    }

    private fun updateBossBarProgress(player: Player) {
        playerBossBars[player.uniqueId]?.let { bossBar ->
            val now = Instant.now()
            val duration = Duration.between(now, KnockBackFFA.nextSwitchTime)
            val totalDuration = plugin.config.getInt("mapDuration", 120)

            val progress = duration.seconds.toFloat() / totalDuration
            bossBar.progress(progress.coerceIn(0f, 1f))

            bossBar.name(formatBossBarText(currentArena?.name ?: "None"))
        }
    }

    private fun formatBossBarText(arenaName: String): net.kyori.adventure.text.Component {
        val now = Instant.now()
        val duration = Duration.between(now, KnockBackFFA.nextSwitchTime)
        val minutes = duration.toMinutes()
        val seconds = duration.seconds % 60
        val timeLeft = "%02d:%02d".format(minutes, seconds)

        return translate("bossbar.format",
            "arena_name" to arenaName,
            "time_left" to timeLeft
        ).asMini()
    }

    fun removeBossBar(player: Player) {
        playerBossBars[player.uniqueId]?.let { bossBar ->
            player.hideBossBar(bossBar)
            playerBossBars.remove(player.uniqueId)
        }
    }

    fun updateBossBar(player: Player, arenaName: String) {
        playerBossBars[player.uniqueId]?.name(formatBossBarText(arenaName))
    }
}