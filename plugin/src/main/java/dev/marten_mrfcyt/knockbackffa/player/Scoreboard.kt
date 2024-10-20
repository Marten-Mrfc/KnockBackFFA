package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.*
import java.util.*

class ScoreboardHandler(private val plugin: KnockBackFFA) {
    private val playerScoreboards: MutableMap<UUID, Scoreboard> = mutableMapOf()

    private fun createScoreboard(source: Player) {
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard

        // Get configuration values
        val title = plugin.config.getString("scoreboard.title")?.asMini()
        val lines = plugin.config.getStringList("scoreboard.lines")

        // Create objective using different logic based on version
        val objective = scoreboard.registerNewObjective("test", Criteria.DUMMY, title)

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

        // Update scores with new entries
        for ((index, line) in lines.withIndex()) {
            // Reset the score for the current index

            if (isBelowVersion("1.20.4")) {
                scoreboard?.entries?.forEach { entry ->
                    val score = objective?.getScore(entry)
                    if (score?.score == lines.size - index) {
                        scoreboard.resetScores(entry)
                    }
                }
                // For versions below 1.20.4
                val legacyLine = line.toLegacyMini(source)
                val score = objective?.getScore(legacyLine)
                score?.score = lines.size - index  // Set the score in reverse order (higher up is closer to the top)
            } else {
                // For versions 1.20.4 and above, using customName (which can handle MiniMessage components)
                val score = objective?.getScore("KnockBackFFA_$index")
                score?.customName(line.asMini(source))
            }
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