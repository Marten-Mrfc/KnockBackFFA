package dev.marten_mrfcyt.knockbackffa.utils

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin
import java.time.Duration
import java.time.Instant

@Suppress("UnstableApiUsage")
class PlaceHolderAPI(knockBackFFA: KnockBackFFA) : PlaceholderExpansion() {
    private val plugin: Plugin = knockBackFFA
    override fun getAuthor(): String {
        return plugin.pluginMeta.authors[0]
    }

    override fun getIdentifier(): String {
        return plugin.pluginMeta.name
    }

    override fun getVersion(): String {
        return plugin.pluginMeta.version
    }

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return null
        val playerData = PlayerData(KnockBackFFA()).getPlayerData(player.uniqueId)
        return when (params) {
            "deaths" -> {
                playerData.getInt("deaths", 0).toString()
            }

            "kills" -> {
                playerData.getInt("kills", 0).toString()
            }

            "killstreak" -> {
                playerData.getInt("killstreak", 0).toString()
            }

            "max-killstreak" -> {
                playerData.getInt("max-killstreak", 0).toString()
            }

            "coins" -> {
                playerData.getDouble("coins", 0.0).toString()
            }

            "kd-ratio" -> {
                playerData.getDouble("kd-ratio", 0.0).toString()
            }

            "map" -> {
                plugin.config.getString("currentArena", "No current arena")
            }

            "next_map" -> {
                val now = Instant.now()
                if (now.isBefore(KnockBackFFA.nextSwitchTime)) {
                    val remainingTime = Duration.between(now, KnockBackFFA.nextSwitchTime)
                    val minutes = remainingTime.toMinutes()
                    val seconds = remainingTime.seconds % 60
                    "%02d:%02d".format(minutes, seconds)
                } else {
                    "00:00"
                }
            }

            else -> null
        }
    }
}