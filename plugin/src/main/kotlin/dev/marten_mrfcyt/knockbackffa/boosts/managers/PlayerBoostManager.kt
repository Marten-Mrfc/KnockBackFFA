package dev.marten_mrfcyt.knockbackffa.boosts.managers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.boosts.events.BoostActivatedEvent
import dev.marten_mrfcyt.knockbackffa.boosts.events.BoostExpiredEvent
import dev.marten_mrfcyt.knockbackffa.boosts.models.Boost
import dev.marten_mrfcyt.knockbackffa.boosts.models.EffectBoost
import dev.marten_mrfcyt.knockbackffa.boosts.models.PlayerBoost
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import dev.marten_mrfcyt.knockbackffa.utils.models.BoostTiming
import dev.marten_mrfcyt.knockbackffa.utils.models.PlayerDataModel
import mlib.api.utilities.message
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.apply
import kotlin.text.get
import kotlin.text.set
import kotlin.times

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

        val playerBoosts = activeBoosts.computeIfAbsent(playerId) { mutableMapOf() }
        val existingBoost = playerBoosts[boostId]

        // If player already has this boost and it lasts longer than the new duration, keep the existing one
        if (existingBoost != null && existingBoost.endTime.isAfter(endTime)) {
            return
        }

        // If there's an existing boost, extend it instead of creating a new one
        val finalEndTime = if (existingBoost != null) {
            existingBoost.endTime
        } else {
            endTime
        }

        val playerBoost = PlayerBoost(
            playerId = playerId,
            boostId = boostId,
            startTime = existingBoost?.startTime ?: now,
            endTime = finalEndTime
        )

        playerBoosts[boostId] = playerBoost

        val player = Bukkit.getPlayer(playerId)
        if (player != null && player.isOnline && !skipApply) {
            val boost = plugin.boostManager.getBoost(boostId)
            boost.apply(player)

            Bukkit.getPluginManager().callEvent(BoostActivatedEvent(player, boost, duration))
        }

        val durationSeconds = Duration.between(now, finalEndTime).seconds
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

        if (!kitBoosts.computeIfAbsent(playerId) { mutableSetOf() }.contains(boostId)) {
            kitBoosts.computeIfAbsent(playerId) { mutableSetOf() }.add(boostId)

        }
        try {
            val boost = plugin.boostManager.getBoost(boostId)
            boost.apply(player)
        } catch (e: Exception) {
            plugin.logger.warning(TranslationManager.translate("boost.error.apply_kit_boost",
                "boost_id" to boostId, "error" to e.message.toString()))
        }
    }

    fun removeKitBoost(player: Player, boostId: String) {
        val playerId = player.uniqueId
        kitBoosts[playerId]?.remove(boostId)
        if (!hasActiveBoost(playerId, boostId)) {
            try {
                val boost = plugin.boostManager.getBoost(boostId)
                if (boost is EffectBoost) {
                    boost.remove(player)
                }
            } catch (e: Exception) {
                plugin.logger.warning(TranslationManager.translate("boost.error.remove_kit_boost",
                    "boost_id" to boostId, "error" to e.message.toString()))
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
                player.message(TranslationManager.translate("boost.expired", "name" to boostType.name))

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
        val playerDataInstance = PlayerData.getInstance(plugin)
        val playerDataModel = playerDataInstance.getPlayerDataModel(playerId)

        // Update active boosts
        val updatedBoosts = playerDataModel.boosts.toMutableList()
        if (!updatedBoosts.contains(boostId)) {
            updatedBoosts.add(boostId)
        }
        playerDataModel.boosts = updatedBoosts

        // Store the timing information
        val boostTimings = playerDataModel.boostTimings?.toMutableMap() ?: mutableMapOf()
        boostTimings[boostId] = BoostTiming(
            startTime = playerBoost.startTime.epochSecond,
            endTime = playerBoost.endTime.epochSecond
        )
        playerDataModel.boostTimings = boostTimings

        playerDataInstance.savePlayerDataModel(playerId, playerDataModel)
    }

    private fun removeBoostFromPlayerData(playerId: UUID, boostId: String) {
        val playerDataInstance = PlayerData.getInstance(plugin)
        val playerDataModel = playerDataInstance.getPlayerDataModel(playerId)

        // Remove boost from the list
        val updatedBoosts = playerDataModel.boosts.toMutableList()
        updatedBoosts.remove(boostId)
        playerDataModel.boosts = updatedBoosts

        // Remove timing information
        val boostTimings = playerDataModel.boostTimings?.toMutableMap() ?: mutableMapOf()
        boostTimings.remove(boostId)
        playerDataModel.boostTimings = boostTimings

        playerDataInstance.savePlayerDataModel(playerId, playerDataModel)
    }

    private fun loadActiveBoosts() {
        val playerDataInstance = PlayerData.getInstance(plugin)

        for (playerId in Bukkit.getOnlinePlayers().map { it.uniqueId }) {
            val playerDataModel = playerDataInstance.getPlayerDataModel(playerId)
            val boostTimings = playerDataModel.boostTimings ?: continue

            for (boostId in playerDataModel.boosts) {
                val timing = boostTimings[boostId] ?: continue

                val startTime = Instant.ofEpochSecond(timing.startTime)
                val endTime = Instant.ofEpochSecond(timing.endTime)

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
                    val remainingDuration = Duration.between(Instant.now(), boost.endTime)
                    addBoost(playerId, boost.boostId, remainingDuration, skipApply = false)
                } catch (e: Exception) {
                    plugin.logger.warning(TranslationManager.translate("boost.error.apply_player_boost",
                        "boost_id" to boost.boostId, "player" to player.name, "error" to e.message.toString()))
                }
            } else {
                removeBoost(playerId, boost.boostId)
            }
        }

        val playerDataModel = PlayerData.getInstance(plugin).getPlayerDataModel(playerId)
        val kitName = playerDataModel.kit
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
                plugin.logger.warning(TranslationManager.translate("boost.error.apply_kit_boosts",
                    "kit_name" to newKitName, "error" to e.message.toString()))
            }
        }
    }
}