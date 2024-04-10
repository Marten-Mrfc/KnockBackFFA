package dev.marten_mrfcyt.knockbackffa.player

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.asMini
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import java.util.*

class ScoreboardHandler(private val plugin: KnockBackFFA) {
    // Create a map to store custom display names
    fun createScoreboard(source: Player) {
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard

        // Get configuration values
        val title = plugin.config.getString("scoreboard.title")?.asMini()
        val lines = plugin.config.getStringList("scoreboard.lines")

        // Create objective
        val objective: Objective = scoreboard.registerNewObjective("test", Criteria.DUMMY, title)
        objective.displaySlot = DisplaySlot.SIDEBAR
        objective.setAutoUpdateDisplay(true)
        // Set scores
        for ((index, line) in lines.withIndex()) {
            // Convert the line to a mini component
            val score = objective.getScore("KnockBackFFA_$index")
            score.customName(line.asMini(source))
            score.score = lines.size - index
            score.objective.willAutoUpdateDisplay()
        }

        // Display the scoreboard to the player
        source.scoreboard = scoreboard
    }

    private val taskIds: MutableMap<UUID, Int> = mutableMapOf()

    fun startUpdatingScoreboard(source: Player) {
        val taskId = object : BukkitRunnable() {
            override fun run() {
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    createScoreboard(source)
                })
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L).taskId // get the task ID

        // Store the task ID for this player
        taskIds[source.uniqueId] = taskId
    }

    fun stopUpdatingScoreboard(source: Player) {
        // Get the task ID for this player
        val taskId = taskIds[source.uniqueId]

        // If a task exists, cancel it
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId)
            // Remove the task ID from the map
            taskIds.remove(source.uniqueId)
        }
    }
}