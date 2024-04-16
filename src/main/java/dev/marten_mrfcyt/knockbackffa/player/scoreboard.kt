package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.asMini
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import java.util.*

class ScoreboardHandler(private val plugin: KnockBackFFA) {
    private val playerScoreboards: MutableMap<UUID, Scoreboard> = mutableMapOf()

    private fun createScoreboard(source: Player) {
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard

        // Get configuration values
        val title = plugin.config.getString("scoreboard.title")?.asMini()
        val lines = plugin.config.getStringList("scoreboard.lines")

        // Create objective
        val objective: Objective = scoreboard.registerNewObjective("test", Criteria.DUMMY, title)
        objective.displaySlot = DisplaySlot.SIDEBAR

        // Set scores
        for ((index) in lines.withIndex()) {
            val score = objective.getScore("KnockBackFFA_$index")
            score.score = lines.size - index
        }

        // Store the scoreboard for this player
        playerScoreboards[source.uniqueId] = scoreboard

        // Display the scoreboard to the player
        source.scoreboard = scoreboard
    }

    private fun updateScoreboard(source: Player) {
        val scoreboard = playerScoreboards[source.uniqueId]
        val objective = scoreboard?.getObjective(DisplaySlot.SIDEBAR)
        val lines = plugin.config.getStringList("scoreboard.lines")

        // Update scores
        for ((index, line) in lines.withIndex()) {
            val score = objective?.getScore("KnockBackFFA_$index")
            score?.customName(line.asMini(source))
        }
    }

    fun startUpdatingScoreboard(source: Player) {
        // Create the scoreboard
        createScoreboard(source)
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            updateScoreboard(source)
        }, 0L, 10L)
    }

    fun stopUpdatingScoreboard(source: Player) {
        // Remove the scoreboard from the map
        playerScoreboards.remove(source.uniqueId)
    }
}