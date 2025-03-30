package dev.marten_mrfcyt.knockbackffa.boosts.managers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.boosts.events.BoostActivatedEvent
import dev.marten_mrfcyt.knockbackffa.boosts.events.BoostExpiredEvent
import dev.marten_mrfcyt.knockbackffa.boosts.models.Boost
import dev.marten_mrfcyt.knockbackffa.boosts.models.EffectBoost
import dev.marten_mrfcyt.knockbackffa.boosts.models.PlayerBoost
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import mlib.api.utilities.message
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PlayerBoostManager(private val plugin: KnockBackFFA) {
    private val activeBoosts = ConcurrentHashMap<UUID, MutableMap<String, PlayerBoost>>()
    private val kitBoosts = ConcurrentHashMap<UUID, MutableSet<String>>()

    init {
        loadActiveBoosts()
        startCleanupTask()
    }

    fun addBoost(playerId: UUID, boostId: String, duration: Duration, skipApply: Boolean = false) {
        val now = Instant.now()
        val endTime = now.plus(duration)

        val playerBoost = PlayerBoost(
            playerId = playerId,
            boostId = boostId,
            startTime = now,
            endTime = endTime
        )

        val existingBoost = activeBoosts.computeIfAbsent(playerId) { mutableMapOf() }[boostId]
        if (existingBoost != null) {
            if (existingBoost.endTime.isAfter(endTime)) {
                return
            }
        }

        activeBoosts.computeIfAbsent(playerId) { mutableMapOf() }[boostId] = playerBoost

        val player = Bukkit.getPlayer(playerId)
        if (player != null && player.isOnline && !skipApply) {
            val boost = plugin.boostManager.getBoost(boostId)
            boost.apply(player)

            Bukkit.getPluginManager().callEvent(BoostActivatedEvent(player, boost, duration))
        }

        val durationSeconds = duration.seconds
        object : BukkitRunnable() {
            override fun run() {
                if (hasActiveBoost(playerId, boostId)) {
                    removeBoost(playerId, boostId)
                }
            }
        }.runTaskLater(plugin, durationSeconds * 20L)

        saveBoost(playerId, boostId, playerBoost)
    }

    fun addKitBoost(player: Player, boostId: String) {
        val playerId = player.uniqueId

        if (kitBoosts.computeIfAbsent(playerId) { mutableSetOf() }.contains(boostId)) {
            return
        }

        kitBoosts.computeIfAbsent(playerId) { mutableSetOf() }.add(boostId)

        try {
            val boost = plugin.boostManager.getBoost(boostId)
            boost.apply(player)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to apply kit boost $boostId: ${e.message}")
        }
    }

    fun removeKitBoost(player: Player, boostId: String) {
        val playerId = player.uniqueId
        kitBoosts[playerId]?.remove(boostId)
        if (!hasActiveBoost(playerId, boostId)) {
            try {
                val boost = plugin.boostManager.getBoost(boostId)
                boost.remove(player)
            } catch (e: Exception) {
                plugin.logger.warning("Failed to remove kit boost $boostId: ${e.message}")
            }
        }
    }

    fun removeAllKitBoosts(player: Player) {
        val playerId = player.uniqueId
        val playerKitBoosts = kitBoosts[playerId] ?: return

        val boostsToRemove = playerKitBoosts.toSet()

        boostsToRemove.forEach { boostId ->
            removeBoost(playerId, boostId)
            removeKitBoost(player, boostId)
        }

        kitBoosts.remove(playerId)
    }

    fun removeBoost(playerId: UUID, boostId: String) {
        val playerBoosts = activeBoosts[playerId] ?: return
        playerBoosts.remove(boostId)

        if (playerBoosts.isEmpty()) {
            activeBoosts.remove(playerId)
        }

        val player = Bukkit.getPlayer(playerId) ?: return

        if (!isKitBoost(playerId, boostId)) {
            val boostType = plugin.boostManager.getBoost(boostId)
            if (player.isOnline && boostType is EffectBoost) {
                boostType.remove(player)
                player.message("<red>Your ${boostType.name} has expired!")

                Bukkit.getPluginManager().callEvent(BoostExpiredEvent(player, boostType))
            }
        }

        removeBoostFromPlayerData(playerId, boostId)
    }

    fun hasActiveBoost(playerId: UUID, boostId: String): Boolean {
        val playerBoosts = activeBoosts[playerId] ?: return false
        val boost = playerBoosts[boostId] ?: return false

        if (boost.isExpired()) {
            removeBoost(playerId, boostId)
            return false
        }

        return true
    }

    fun isKitBoost(playerId: UUID, boostId: String): Boolean {
        return kitBoosts[playerId]?.contains(boostId) == true
    }

    fun getActiveBoosts(playerId: UUID): List<PlayerBoost> {
        val playerBoosts = activeBoosts[playerId] ?: return emptyList()
        return playerBoosts.values.filter { !it.isExpired() }
    }

    fun getActiveKitBoosts(playerId: UUID): List<Boost> {
        return kitBoosts[playerId]?.mapNotNull { boostId ->
            plugin.boostManager.getBoost(boostId)
        }?.filter { it is EffectBoost } ?: emptyList()
    }

    fun getRemainingTime(playerId: UUID, boostId: String): Duration {
        val playerBoosts = activeBoosts[playerId] ?: return Duration.ZERO
        val boost = playerBoosts[boostId] ?: return Duration.ZERO

        if (boost.isExpired()) {
            removeBoost(playerId, boostId)
            return Duration.ZERO
        }

        val now = Instant.now()
        return Duration.between(now, boost.endTime)
    }

    private fun saveBoost(playerId: UUID, boostId: String, playerBoost: PlayerBoost) {
        val playerData = PlayerData.getInstance(plugin)
        val playerConfig = playerData.getPlayerData(playerId)

        playerConfig.set("boosts.$boostId.startTime", playerBoost.startTime.epochSecond)
        playerConfig.set("boosts.$boostId.endTime", playerBoost.endTime.epochSecond)

        playerData.savePlayerData(playerId, playerConfig)
    }

    private fun removeBoostFromPlayerData(playerId: UUID, boostId: String) {
        val playerData = PlayerData.getInstance(plugin)
        val playerConfig = playerData.getPlayerData(playerId)

        playerConfig.set("boosts.$boostId", null)

        playerData.savePlayerData(playerId, playerConfig)
    }

    private fun loadActiveBoosts() {
        val playerData = PlayerData.getInstance(plugin)

        for (playerId in Bukkit.getOnlinePlayers().map { it.uniqueId }) {
            val config = playerData.getPlayerData(playerId)
            val boostsSection = config.getConfigurationSection("boosts") ?: continue

            for (boostId in boostsSection.getKeys(false)) {
                val startTimeSeconds = boostsSection.getLong("$boostId.startTime")
                val endTimeSeconds = boostsSection.getLong("$boostId.endTime")

                val startTime = Instant.ofEpochSecond(startTimeSeconds)
                val endTime = Instant.ofEpochSecond(endTimeSeconds)

                val playerBoost = PlayerBoost(
                    playerId = playerId,
                    boostId = boostId,
                    startTime = startTime,
                    endTime = endTime
                )

                if (!playerBoost.isExpired()) {
                    activeBoosts.computeIfAbsent(playerId) { mutableMapOf() }[boostId] = playerBoost
                } else {
                    removeBoostFromPlayerData(playerId, boostId)
                }
            }
        }
    }

    fun loadPlayerBoostsOnJoin(player: Player) {
        val playerId = player.uniqueId
        val playerBoosts = activeBoosts[playerId] ?: return

        playerBoosts.values.forEach { boost ->
            if (!boost.isExpired()) {
                try {
                    removeBoost(playerId, boost.boostId)
                    val boostType = plugin.boostManager.getBoost(boost.boostId)
                    boostType.apply(player)
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to apply boost ${boost.boostId} to player ${player.name}: ${e.message}")
                }
            } else {
                removeBoost(playerId, boost.boostId)
            }
        }

        val kitName = PlayerData.getInstance(plugin).getPlayerData(playerId).getString("kit")
        if (kitName != null) {
            val kit = KnockBackFFA.kitManager.getKit(kitName)
            kit.boosts.forEach { boostId ->
                addKitBoost(player, boostId)
            }
        }
    }

    private fun startCleanupTask() {
        object : BukkitRunnable() {
            override fun run() {
                cleanupExpiredBoosts()
            }
        }.runTaskTimer(plugin, 100L, 100L) // 100 ticks = 5 seconds
    }

    private fun cleanupExpiredBoosts() {
        val now = Instant.now()

        activeBoosts.forEach { (playerId, boosts) ->
            val expiredBoosts = boosts.filter { (_, boost) -> boost.endTime.isBefore(now) }

            expiredBoosts.forEach { (boostId, _) ->
                removeBoost(playerId, boostId)
            }
        }
    }

    fun reloadAllPlayerBoosts() {
        activeBoosts.clear()
        kitBoosts.clear()

        loadActiveBoosts()

        for (player in Bukkit.getOnlinePlayers()) {
            loadPlayerBoostsOnJoin(player)
        }
    }

    fun handleKitChange(player: Player, newKitName: String?) {
        removeAllKitBoosts(player)

        if (newKitName != null) {
            try {
                val kit = KnockBackFFA.kitManager.getKit(newKitName)
                kit.boosts.forEach { boostId ->
                    addKitBoost(player, boostId)
                }
            } catch (e: Exception) {
                plugin.logger.warning("Failed to apply kit boosts for kit $newKitName: ${e.message}")
            }
        }
    }
}