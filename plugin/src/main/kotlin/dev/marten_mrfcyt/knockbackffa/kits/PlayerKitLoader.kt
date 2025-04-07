package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import org.bukkit.entity.Player

fun loadKit(plugin: KnockBackFFA, player: Player, joining: Boolean) {
    val playerDataInstance = PlayerData.getInstance(plugin)
    val playerDataModel = playerDataInstance.getPlayerDataModel(player.uniqueId)

    if (playerDataModel.kit == null) {
        playerDataModel.kit = "default"
        KitOwnership.addKit(player.uniqueId, "default")
        playerDataInstance.savePlayerDataModel(player.uniqueId, playerDataModel)
    }

    val kitName = playerDataModel.kit

    if (kitName != null) {
        KitLayoutManager.markKitLoading(player.uniqueId)

        try {
            if (joining) {
                KnockBackFFA.instance.playerBoostManager.loadPlayerBoostsOnJoin(player)
            }
            KnockBackFFA.kitManager.applyKit(player, kitName, true)
        } catch (e: Exception) {
            plugin.logger.warning("[KitLoader] Failed to load kit for player ${player.name}: ${e.message}")
        }
    }
}