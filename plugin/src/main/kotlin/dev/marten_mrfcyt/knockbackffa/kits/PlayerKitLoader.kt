package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import org.bukkit.entity.Player

fun loadKit(plugin: KnockBackFFA, player: Player, joining: Boolean) {
    mlib.api.utilities.debug(plugin, "Loading kit for player ${player.name} (joining: $joining)")
    val playerDataInstance = PlayerData.getInstance(plugin)
    val playerDataModel = playerDataInstance.getPlayerDataModel(player.uniqueId)

    if (playerDataModel.kit == null) {
        mlib.api.utilities.debug(plugin, "Player ${player.name} has no kit selected, assigning default kit")
        playerDataModel.kit = "default"
        KitOwnership.addKit(player.uniqueId, "default")
        playerDataInstance.savePlayerDataModel(player.uniqueId, playerDataModel)
    }

    val kitName = playerDataModel.kit
    mlib.api.utilities.debug(plugin, "Loading kit '${kitName}' for player ${player.name}")

    if (kitName != null) {
        KitLayoutManager.markKitLoading(player.uniqueId)

        try {
            if (joining) {
                mlib.api.utilities.debug(plugin, "Player ${player.name} is joining, loading boosts")
                KnockBackFFA.instance.playerBoostManager.loadPlayerBoostsOnJoin(player)
            }
            KnockBackFFA.kitManager.applyKit(player, kitName, true)
            mlib.api.utilities.debug(plugin, "Successfully applied kit '${kitName}' to player ${player.name}")
        } catch (e: Exception) {
            plugin.logger.warning("[KitLoader] Failed to load kit for player ${player.name}: ${e.message}")
            mlib.api.utilities.debug(plugin, "Error loading kit '${kitName}' for player ${player.name}: ${e.message}")
        }
    }
}