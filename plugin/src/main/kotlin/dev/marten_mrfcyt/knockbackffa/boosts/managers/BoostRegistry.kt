package dev.marten_mrfcyt.knockbackffa.boosts.managers

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.boosts.models.Boost
import java.util.concurrent.ConcurrentHashMap

class BoostRegistry(private val plugin: KnockBackFFA) {
    private val boosts = ConcurrentHashMap<String, Boost>()

    fun register(boost: Boost) {
        if(!boost.enabled) return
        boosts[boost.id] = boost
    }

    fun getBoost(id: String): Boost? {
        return boosts[id]
    }

    fun getAllBoosts(): Collection<Boost> {
        return boosts.values
    }

    fun clearBoosts() {
        boosts.clear()
    }
}