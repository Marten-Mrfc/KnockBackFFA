package dev.marten_mrfcyt.knockbackffa.boosts.models

import java.time.Instant
import java.util.UUID

data class PlayerBoost(
    val playerId: UUID,
    val boostId: String,
    val startTime: Instant,
    val endTime: Instant
) {
    fun isExpired(): Boolean {
        return Instant.now().isAfter(endTime)
    }
}