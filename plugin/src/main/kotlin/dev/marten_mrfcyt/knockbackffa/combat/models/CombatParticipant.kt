package dev.marten_mrfcyt.knockbackffa.combat.models

import java.util.UUID

/**
 * Represents a player's participation in combat
 */
data class CombatParticipant(
    val playerId: UUID,
    val playerName: String,
    var damageDealt: Double = 0.0,
    var lastDamageTime: Long = System.currentTimeMillis()
) {
    /**
     * Add damage to this participant's total
     */
    fun addDamage(damage: Double) {
        damageDealt += damage
        lastDamageTime = System.currentTimeMillis()
    }
    
    /**
     * Calculate the percentage of contribution based on total damage
     */
    fun getContributionPercentage(totalDamage: Double): Double {
        return if (totalDamage > 0) (damageDealt / totalDamage) * 100.0 else 0.0
    }
    
    /**
     * Calculate coins based on contribution percentage
     */
    fun calculateCoins(totalDamage: Double): Double {
        val percentage = getContributionPercentage(totalDamage)
        return percentage / 100.0 // 10% = 0.1 coins, 50% = 0.5 coins, etc.
    }
}
