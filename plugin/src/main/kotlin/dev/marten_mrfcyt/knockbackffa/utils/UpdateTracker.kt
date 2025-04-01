package dev.marten_mrfcyt.knockbackffa.utils

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
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

@Suppress("UnstableApiUsage")
class UpdateTracker(private val plugin: KnockBackFFA) : Listener {
    private var latestVersion: String? = null
    private var updateAvailable = false
    private val updateCheckUrl = "https://api.github.com/repos/Marten-Mrfc/KnockBackFFA/releases/latest"
    private val mm = MiniMessage.miniMessage()

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
                        plugin.logger.info("✅ KnockBackFFA is up to date (Active: $currentVersion | Latest: $latestVersion)")
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to check for updates: ${e.message}", e)
        }
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

        val header = mm.deserialize("<dark_gray><bold>┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
        val title = mm.deserialize("<dark_gray><bold>┃        <yellow><bold>🔄 UPDATE AVAILABLE     <dark_gray><bold>┃")
        val divider = mm.deserialize("<dark_gray><bold>┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")

        val currentVersionText = mm.deserialize("<dark_gray><bold>┃ <gray>Current: <red><bold>v${currentVersion}")
        val latestVersionText = mm.deserialize("<dark_gray><bold>┃ <gray>Latest:  <green><bold>${latestVersion}")

        val downloadUrl = "https://github.com/Marten-Mrfc/KnockBackFFA/releases/latest"
        val downloadText = Component.text("┃ ", NamedTextColor.DARK_GRAY, TextDecoration.BOLD)
            .append(Component.text("Download: ", NamedTextColor.GRAY))
            .append(
                Component.text("CLICK HERE", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.openUrl(downloadUrl))
                    .hoverEvent(HoverEvent.showText(mm.deserialize("<gray>Click to open <aqua><underlined>${downloadUrl}")))
            )

        val footer = mm.deserialize("<dark_gray><bold>┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

        player.sendMessage(header)
        player.sendMessage(title)
        player.sendMessage(divider)
        player.sendMessage(currentVersionText)
        player.sendMessage(latestVersionText)
        player.sendMessage(Component.text("").append(downloadText))
        player.sendMessage(footer)
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