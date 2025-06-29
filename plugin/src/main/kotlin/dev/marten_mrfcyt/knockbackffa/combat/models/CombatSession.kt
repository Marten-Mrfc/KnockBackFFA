package dev.marten_mrfcyt.knockbackffa.combat.models

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks damage dealt to a specific victim by multiple attackers
 */
class CombatSession(
    val victimId: UUID,
    val victimName: String
) {
    private val participants = ConcurrentHashMap<UUID, CombatParticipant>()
    private val sessionStartTime = System.currentTimeMillis()
    private val sessionTimeout = 15000L // 15 seconds
    
    /**
     * Add damage from an attacker
     */
    fun addDamage(attackerId: UUID, attackerName: String, damage: Double) {
        val participant = participants.computeIfAbsent(attackerId) {
            CombatParticipant(attackerId, attackerName)
        }
        participant.addDamage(damage)
    }
    
    /**
     * Get all participants in this combat session
     */
    fun getParticipants(): Collection<CombatParticipant> {
        return participants.values
    }
    
    /**
     * Get total damage dealt in this session
     */
    fun getTotalDamage(): Double {
        return participants.values.sumOf { it.damageDealt }
    }
    
    /**
     * Check if this session has expired
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() - sessionStartTime > sessionTimeout
    }
    
    /**
     * Get participant by UUID
     */
    fun getParticipant(playerId: UUID): CombatParticipant? {
        return participants[playerId]
    }
}