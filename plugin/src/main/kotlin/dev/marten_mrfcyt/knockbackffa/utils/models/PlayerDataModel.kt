package dev.marten_mrfcyt.knockbackffa.utils.models

import java.util.UUID

data class PlayerDataModel(
    val playerId: UUID,
    var kit: String? = null,
    var deaths: Int = 0,
    var kills: Int = 0,
    var killstreak: Int = 0,
    var maxKillstreak: Int = 0,
    var coins: Int = 0,
    var kdRatio: Double = 0.0,
    var ownedKits: List<String> = emptyList(),
    var boosts: List<String> = emptyList(),
)