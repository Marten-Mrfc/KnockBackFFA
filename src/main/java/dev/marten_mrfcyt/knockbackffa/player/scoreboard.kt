package dev.marten_mrfcyt.knockbackffa.player
import net.kyori.adventure.text.Component

import dev.marten_mrfcyt.knockbackffa.utils.asMini
import dev.marten_mrfcyt.knockbackffa.utils.message
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard

class ScoreboardHandler {
    fun createScoreboard(source: Player) {
        val scoreboard = Bukkit.getScoreboardManager()?.newScoreboard ?: return

        // Create objective
        val objective: Objective = scoreboard.registerNewObjective("test", Criteria.DUMMY, Component.text("Scoreboard"))
        objective.displaySlot = DisplaySlot.SIDEBAR

        // Set scores
        val score = objective.getScore("Welcome, ${source.name}!")
        score.score = 1

        // Display the scoreboard to the player
        source.scoreboard = scoreboard
    }
}