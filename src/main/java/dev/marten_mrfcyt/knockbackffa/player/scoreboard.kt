package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.asMini
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective

class ScoreboardHandler(private val plugin: KnockBackFFA) {
    // Create a map to store custom display names
    private val displayNames: MutableMap<String, String> = mutableMapOf()

    fun createScoreboard(source: Player) {
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard

        // Get configuration values
        val title = plugin.config.getString("scoreboard.title")?.asMini()
        val lines = plugin.config.getStringList("scoreboard.lines")

        // Create objective
        val objective: Objective = scoreboard.registerNewObjective("test", Criteria.DUMMY, title)
        objective.displaySlot = DisplaySlot.SIDEBAR

        // Set scores
        for ((index, line) in lines.withIndex()) {
            // Convert the line to a mini component
            val score = objective.getScore("KnockBackFFA_$index")
            score.customName(line.asMini(source))
            score.score = lines.size - index
        }

        // Display the scoreboard to the player
        source.scoreboard = scoreboard
    }
}