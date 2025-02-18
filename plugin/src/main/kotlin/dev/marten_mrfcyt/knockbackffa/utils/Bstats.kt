package dev.marten_mrfcyt.knockbackffa.utils

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import org.bstats.bukkit.Metrics
import org.bstats.charts.SingleLineChart

object BStatsMetrics {
    private const val ID = 24335

    fun registerMetrics() {
        val metrics = Metrics(KnockBackFFA.instance, ID)
        // total_arenas_created
        metrics.addCustomChart(SingleLineChart("total_arenas_created") {
            KnockBackFFA.instance.arenaHandler.getArenaNames().size
        })
        // total_kills
        metrics.addCustomChart(SingleLineChart("total_kills") {
            PlayerData.getInstance(KnockBackFFA.instance).getTotalKills()
        })
    }
}