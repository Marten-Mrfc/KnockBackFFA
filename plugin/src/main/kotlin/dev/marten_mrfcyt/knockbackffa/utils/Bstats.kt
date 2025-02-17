package dev.marten_mrfcyt.knockbackffa.utils

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import org.bstats.bukkit.Metrics
import org.bstats.charts.SingleLineChart

object BStatsMetrics {
    private const val ID = 24335

    fun registerMetrics() {
        val metrics = Metrics(KnockBackFFA.instance, ID)
        metrics.addCustomChart(SingleLineChart("total_arenas_created") {
            KnockBackFFA.instance.arenaHandler.getArenaNames().size
        })
    }
}