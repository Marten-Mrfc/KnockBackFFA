package dev.marten_mrfcyt.knockbackffa.combat

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.combat.models.CombatParticipant
import dev.marten_mrfcyt.knockbackffa.combat.models.CombatSession
import dev.marten_mrfcyt.knockbackffa.combat.models.KillType
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import mlib.api.utilities.debug
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CombatManager(private val plugin: KnockBackFFA) : Listener {
    
    private val combatSessions = ConcurrentHashMap<UUID, CombatSession>()
    private val recentKills = ConcurrentHashMap<UUID, MutableSet<UUID>>() // killer -> victims
    private val killTimestamps = ConcurrentHashMap<Pair<UUID, UUID>, Long>() // (killer, victim) -> timestamp
    private val playerData = PlayerData.getInstance(plugin)
    private val miniMessage = MiniMessage.miniMessage()
    private val minAssistPercentage = plugin.config.getDouble("combat.min-assist-percentage", 5.0)
    
    init {
        startCleanupTask()
    }
    
    @EventHandler
    fun onPlayerDamage(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        val attacker = event.damager as? Player ?: return
        
        if (victim == attacker) return
        
        val damage = event.finalDamage
        debug("Tracking damage: ${attacker.name} -> ${victim.name} ($damage)")
        
        // Get or create combat session for victim
        val session = combatSessions.computeIfAbsent(victim.uniqueId) {
            CombatSession(victim.uniqueId, victim.name)
        }
        
        // Add damage to session
        session.addDamage(attacker.uniqueId, attacker.name, damage)
    }
    
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val victim = event.entity
        val killer = victim.killer
        
        debug("Player ${victim.name} died, killer: ${killer?.name ?: "none"}")
        
        // Get combat session
        val session = combatSessions.remove(victim.uniqueId)
        
        if (session != null && killer != null) {
            processCombatDeath(victim, killer, session)
        } else if (killer != null) {
            // Direct kill without tracked combat session
            processSimpleKill(victim, killer)
        }
    }
    
    private fun processCombatDeath(victim: Player, killer: Player, session: CombatSession) {
        val participants = session.getParticipants()
        val totalDamage = session.getTotalDamage()
        
        debug("Processing combat death: ${victim.name}, ${participants.size} participants, $totalDamage total damage")
        
        // Determine kill type
        val killType = determineKillType(killer, victim)
        
        // Process each participant
        participants.forEach { participant ->
            val isKiller = participant.playerId == killer.uniqueId
            val contributionPercentage = participant.getContributionPercentage(totalDamage)
            val coins = if (isKiller) killType.coins else participant.calculateCoins(totalDamage)
            
            // Only process participants with meaningful contribution
            if (!isKiller && contributionPercentage < minAssistPercentage) {
                debug("Skipping assist for ${participant.playerName}: ${String.format("%.1f", contributionPercentage)}% < ${minAssistPercentage}%")
                return@forEach
            }
            
            // Update player data
            val playerDataModel = playerData.getPlayerDataModel(participant.playerId)
            
            // Add coins (rounded to 2 decimal places for storage, but we'll track precise amounts)
            val roundedCoins = (coins * 100).toInt() / 100.0
            playerDataModel.coins += roundedCoins.toInt()
            
            if (isKiller) {
                playerDataModel.kills += 1
                playerDataModel.killstreak += 1
                if (playerDataModel.killstreak > playerDataModel.maxKillstreak) {
                    playerDataModel.maxKillstreak = playerDataModel.killstreak
                }
                // Track recent kill for revenge detection
                recentKills.computeIfAbsent(participant.playerId) { mutableSetOf() }.add(victim.uniqueId)
                killTimestamps[Pair(participant.playerId, victim.uniqueId)] = System.currentTimeMillis()
            } else {
                playerDataModel.assists += 1
            }
            
            playerDataModel.damageDealt += participant.damageDealt
            playerDataModel.kdRatio = if (playerDataModel.deaths > 0) {
                playerDataModel.kills.toDouble() / playerDataModel.deaths
            } else {
                playerDataModel.kills.toDouble()
            }
            
            playerData.savePlayerDataModel(participant.playerId, playerDataModel)
            
            // Send action bar message
            val player = plugin.server.getPlayer(participant.playerId)
            if (player != null) {
                val message = if (isKiller) {
                    "Killed ${victim.name} - ${killType.displayName} - ${coins.toInt()} ${if (coins.toInt() == 1) "coin" else "coins"}"
                } else {
                    val percentageText = String.format("%.0f", contributionPercentage)
                    val coinsText = String.format("%.2f", coins)
                    "Assist kill ${percentageText}% - $coinsText ${if (coins == 1.0) "coin" else "coins"}"
                }
                player.sendActionBar(Component.text(message))
                debug("Sent action bar to ${player.name}: $message")
            }
        }
        
        // Update victim data
        val victimDataModel = playerData.getPlayerDataModel(victim.uniqueId)
        victimDataModel.deaths += 1
        victimDataModel.killstreak = 0
        victimDataModel.kdRatio = if (victimDataModel.deaths > 0) {
            victimDataModel.kills.toDouble() / victimDataModel.deaths
        } else {
            victimDataModel.kills.toDouble()
        }
        playerData.savePlayerDataModel(victim.uniqueId, victimDataModel)
    }
    
    private fun processSimpleKill(victim: Player, killer: Player) {
        debug("Processing simple kill: ${killer.name} -> ${victim.name}")
        
        val killType = determineKillType(killer, victim)
        val coins = killType.coins
        
        // Update killer data
        val killerDataModel = playerData.getPlayerDataModel(killer.uniqueId)
        killerDataModel.coins += coins.toInt()
        killerDataModel.kills += 1
        killerDataModel.killstreak += 1
        if (killerDataModel.killstreak > killerDataModel.maxKillstreak) {
            killerDataModel.maxKillstreak = killerDataModel.killstreak
        }
        killerDataModel.kdRatio = if (killerDataModel.deaths > 0) {
            killerDataModel.kills.toDouble() / killerDataModel.deaths
        } else {
            killerDataModel.kills.toDouble()
        }
        
        // Track recent kill for revenge detection
        recentKills.computeIfAbsent(killer.uniqueId) { mutableSetOf() }.add(victim.uniqueId)
        killTimestamps[Pair(killer.uniqueId, victim.uniqueId)] = System.currentTimeMillis()
        
        playerData.savePlayerDataModel(killer.uniqueId, killerDataModel)
        
        // Update victim data
        val victimDataModel = playerData.getPlayerDataModel(victim.uniqueId)
        victimDataModel.deaths += 1
        victimDataModel.killstreak = 0
        victimDataModel.kdRatio = if (victimDataModel.deaths > 0) {
            victimDataModel.kills.toDouble() / victimDataModel.deaths
        } else {
            victimDataModel.kills.toDouble()
        }
        playerData.savePlayerDataModel(victim.uniqueId, victimDataModel)
        
        // Send action bar message
        killer.sendActionBar(Component.text("Killed ${victim.name} - ${killType.displayName} - ${coins.toInt()} ${if (coins.toInt() == 1) "coin" else "coins"}"))
    }
    
    private fun determineKillType(killer: Player, victim: Player): KillType {
        // Check for AERIAL kill (killer is airborne)
        if (killer.location.block.getRelative(BlockFace.DOWN).type.isAir && killer.velocity.y < 0.5) {
            println("Kill type: AERIAL for ${killer.name} -> ${victim.name}, ${killer.location.block.getRelative(BlockFace.DOWN).type} + ${killer.velocity.y}")
            return KillType.AERIAL
        }
        
        // Check for REVENGE kill (victim killed killer recently)
        val recentVictims = recentKills[victim.uniqueId]
        if (recentVictims?.contains(killer.uniqueId) == true) {
            return KillType.REVENGE
        }
        
        // Check for UNDERDOG kill (victim has higher K/D than killer)
        val killerData = playerData.getPlayerDataModel(killer.uniqueId)
        val victimData = playerData.getPlayerDataModel(victim.uniqueId)
        
        if (victimData.kdRatio > killerData.kdRatio) {
            return KillType.UNDERDOG
        }
        
        // Default to NORMAL
        return KillType.NORMAL
    }
    
    private fun startCleanupTask() {
        object : BukkitRunnable() {
            override fun run() {
                // Clean up expired combat sessions
                val expiredSessions = combatSessions.filter { (_, session) -> session.isExpired() }
                expiredSessions.forEach { (victimId, _) ->
                    combatSessions.remove(victimId)
                }
                
                // Clean up old recent kills (older than 30 seconds)
                val currentTime = System.currentTimeMillis()
                recentKills.entries.removeIf { (killerId, victims) ->
                    victims.removeIf { victimId ->
                        val killTime = killTimestamps[Pair(killerId, victimId)] ?: 0L
                        val isExpired = currentTime - killTime > 30000
                        if (isExpired) {
                            killTimestamps.remove(Pair(killerId, victimId))
                        }
                        isExpired
                    }
                    victims.isEmpty()
                }
            }
        }.runTaskTimerAsynchronously(plugin, 100L, 100L)
    }
}
