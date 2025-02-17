package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import mlib.api.utilities.asMini
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import java.util.*

class ScoreboardHandler(private val plugin: KnockBackFFA) {
    private val playerScoreboards = mutableMapOf<UUID, PlayerBoard>()

    private data class PlayerBoard(
        val scoreboard: Scoreboard,
        val objective: Objective,
        val lastLines: MutableList<Component> = mutableListOf()
    )

    private fun createScoreboard(player: Player): PlayerBoard {
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard
        val title = TranslationManager.translate("scoreboard.title").asMini()

        val objective = scoreboard.registerNewObjective("kbffa", Criteria.DUMMY, title)
        objective.displaySlot = DisplaySlot.SIDEBAR

        return PlayerBoard(scoreboard, objective).also {
            playerScoreboards[player.uniqueId] = it
            player.scoreboard = scoreboard
        }
    }

    private fun updateScoreboard(player: Player) {
        val board = playerScoreboards[player.uniqueId] ?: return
        val lines = TranslationManager.getStringList("scoreboard.lines")
            .map { it.asMini(player) }

        for ((index, line) in lines.withIndex()) {
            val score = "line_${15 - index}"

            if (index >= board.lastLines.size || !board.lastLines[index].equals(line)) {
                board.scoreboard.resetScores(score)

                val obj = board.objective.getScore(score)
                obj.score = 15 - index
                obj.customName(line)
            }
        }

        if (lines.size < board.lastLines.size) {
            for (i in lines.size until board.lastLines.size) {
                board.scoreboard.resetScores("line_${15 - i}")
            }
        }

        board.lastLines.clear()
        board.lastLines.addAll(lines)
    }

    fun startUpdatingScoreboard(player: Player) {
        createScoreboard(player)
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            updateScoreboard(player)
        }, 0L, 10L)
    }

    fun stopUpdatingScoreboard(player: Player) {
        playerScoreboards.remove(player.uniqueId)
        player.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
    }
}