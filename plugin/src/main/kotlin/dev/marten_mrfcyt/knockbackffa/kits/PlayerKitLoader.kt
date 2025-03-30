// src/main/kotlin/dev/marten_mrfcyt/knockbackffa/kits/PlayerKitLoader.kt
package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import org.bukkit.entity.Player

fun loadKit(plugin: KnockBackFFA, player: Player) {
    val playerData = PlayerData.getInstance(plugin).getPlayerData(player.uniqueId)
    val kitName = playerData.getString("kit")

    if (kitName != null) {
        KnockBackFFA.kitManager.applyKit(player, kitName)
        KnockBackFFA.instance.playerBoostManager.loadPlayerBoostsOnJoin(player)
    } else {
        KitOwnership.addKit(player.uniqueId, "default")
        KnockBackFFA.kitManager.applyKit(player, "default")
    }
}