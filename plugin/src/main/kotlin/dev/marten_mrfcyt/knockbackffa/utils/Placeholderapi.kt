package dev.marten_mrfcyt.knockbackffa.utils

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin
import java.time.Duration
import java.time.Instant

@Suppress("UnstableApiUsage")
class PlaceHolderAPI(private val knockBackFFA: KnockBackFFA) : PlaceholderExpansion() {
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

    override fun persist(): Boolean {
        return true // This prevents PlaceholderAPI from unregistering the expansion on reload
    }

    override fun canRegister(): Boolean {
        return true // Explicitly confirm that this expansion can register
    }

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return null

        val playerDataInstance = PlayerData.getInstance(knockBackFFA)
        val playerDataModel = playerDataInstance.getPlayerDataModel(player.uniqueId)

        return when (params) {
            "deaths" -> {
                playerDataModel.deaths.toString()
            }

            "kills" -> {
                playerDataModel.kills.toString()
            }

            "killstreak" -> {
                playerDataModel.killstreak.toString()
            }

            "max-killstreak" -> {
                playerDataModel.maxKillstreak.toString()
            }

            "coins" -> {
                playerDataModel.coins.toString()
            }

            "kd-ratio" -> {
                val raw = playerDataModel.kdRatio
                val rounded = (raw * 100).toInt() / 100.0
                val formatted = if (rounded == 0.0) {
                    "0"
                } else {
                    rounded.toString().trimEnd('0').trimEnd('.')
                }
                formatted
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