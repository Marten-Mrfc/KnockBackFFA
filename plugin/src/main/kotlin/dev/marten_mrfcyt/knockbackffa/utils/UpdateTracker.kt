package dev.marten_mrfcyt.knockbackffa.utils

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import mlib.api.utilities.asMini
import mlib.api.utilities.message
import mlib.api.utilities.notMini
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.logging.Level
import kotlin.text.append

@Suppress("UnstableApiUsage")
class UpdateTracker(private val plugin: KnockBackFFA) : Listener {
    private var latestVersion: String? = null
    private var updateAvailable = false
    private val updateCheckUrl = "https://api.github.com/repos/Marten-Mrfc/KnockBackFFA/releases/latest"

    init {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            checkForUpdates()
        })

        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    private fun checkForUpdates() {
        try {
            val connection = URL(updateCheckUrl).openConnection()
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "KnockBackFFA-UpdateChecker")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            BufferedReader(InputStreamReader(connection.getInputStream())).use { reader ->
                val response = reader.readText()
                val parser = JSONParser()
                val jsonObject = parser.parse(response) as JSONObject
                latestVersion = jsonObject["tag_name"]?.toString()

                if (latestVersion != null) {
                    val currentVersion = plugin.pluginMeta.version
                    updateAvailable = isNewerVersion(latestVersion, currentVersion)

                    if (updateAvailable) {
                        plugin.logger.info("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
                        plugin.logger.info("┃                    🔄 UPDATE AVAILABLE                    ┃")
                        plugin.logger.info("┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
                        plugin.logger.info("┃ Current: $currentVersion                                            ┃")
                        plugin.logger.info("┃ Latest:  $latestVersion                                            ┃")
                        plugin.logger.info("┃ URL: https://github.com/Marten-Mrfc/KnockBackFFA/releases ┃")
                        plugin.logger.info("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

                        notifyOnlinePlayers()
                    } else {
                        plugin.logger.info("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
                        plugin.logger.info("┃                 ✅ UP TO DATE                   ┃")
                        plugin.logger.info("┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
                        plugin.logger.info("┃ Active: $currentVersion ${isDevelopmentVersion(currentVersion, latestVersion) ?: ""}")
                        plugin.logger.info("┃ Latest: $latestVersion")
                        plugin.logger.info("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to check for updates: ${e.message}", e)
        }
    }
    private fun isDevelopmentVersion(current: String, latest: String?): String? {
        if (latest == null) return null

        val currentParts = current.removePrefix("v").split(".")
        val latestParts = latest.removePrefix("v").split(".")

        for (i in currentParts.indices.take(minOf(currentParts.size, latestParts.size))) {
            val currentNum = currentParts[i].toIntOrNull() ?: 0
            val latestNum = latestParts[i].toIntOrNull() ?: 0

            if (currentNum > latestNum) return " 🧪 DEV"
            if (currentNum < latestNum) return null
        }

        return if (currentParts.size > latestParts.size) " 🧪 DEV" else null
    }
    private fun isNewerVersion(latestVersion: String?, currentVersion: String): Boolean {
        if (latestVersion == null) return false

        val latest = latestVersion.removePrefix("v").split(".")
        val current = currentVersion.removePrefix("v").split(".")

        for (i in latest.indices) {
            if (i >= current.size) return true
            val latestNum = latest[i].toIntOrNull() ?: 0
            val currentNum = current[i].toIntOrNull() ?: 0
            if (latestNum > currentNum) return true
            if (latestNum < currentNum) return false
        }

        return false
    }

    private fun notifyOnlinePlayers() {
        Bukkit.getOnlinePlayers().forEach { player ->
            if (player.hasPermission("knockbackffa.admin") || player.isOp) {
                notifyPlayer(player)
            }
        }
    }

    private fun notifyPlayer(player: Player) {
        if (!updateAvailable || latestVersion == null) return

        val currentVersion = plugin.pluginMeta.version
        val downloadUrl = "https://github.com/Marten-Mrfc/KnockBackFFA/releases/latest"

        val messages = listOf(
            "<dark_gray><bold>┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓",
            "<dark_gray><bold>┃        <yellow><bold>🔄 UPDATE AVAILABLE     <dark_gray><bold>┃",
            "<dark_gray><bold>┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫",
            "<dark_gray><bold>┃ <gray>Current: <red><bold>v${currentVersion}",
            "<dark_gray><bold>┃ <gray>Latest:  <green><bold>${latestVersion}"
        )

        val downloadText = Component.text("┃ ", NamedTextColor.DARK_GRAY, TextDecoration.BOLD)
            .append(Component.text("Download: ", NamedTextColor.GRAY))
            .append(
                Component.text("CLICK HERE", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.openUrl(downloadUrl))
                    .hoverEvent(HoverEvent.showText(("<gray>Click to open <aqua><underlined>${downloadUrl}").asMini()))
            )

        messages.forEach { player.message(it) }
        player.message((Component.text("").append(downloadText)).notMini())
        player.message("<dark_gray><bold>┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.hasPermission("knockbackffa.admin") || player.isOp) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                notifyPlayer(player)
            }, 40L)
        }
    }

    companion object {
        @Volatile
        private var instance: UpdateTracker? = null

        fun init(plugin: KnockBackFFA): UpdateTracker {
            return instance ?: synchronized(this) {
                instance ?: UpdateTracker(plugin).also { instance = it }
            }
        }
    }
}