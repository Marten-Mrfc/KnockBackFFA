package dev.marten_mrfcyt.knockbackffa.combat.models

/**
 * Different types of kills with their coin rewards
 */
enum class KillType(
    val displayName: String,
    val coins: Double,
    val description: String
) {
    NORMAL("NORMAL", 1.0, "Standard kill"),
    AERIAL("AERIAL", 5.0, "Kill while airborne"),
    UNDERDOG("UNDERDOG", 2.0, "Kill someone with higher K/D ratio"),
    REVENGE("REVENGE", 2.0, "Kill someone who recently killed you");
    
    companion object {
        fun getByName(name: String): KillType? {
            return KillType.entries.find { it.name.equals(name, ignoreCase = true) }
        }
    }
}
