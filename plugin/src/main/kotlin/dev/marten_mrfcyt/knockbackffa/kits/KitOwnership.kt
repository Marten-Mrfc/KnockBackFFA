package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import mlib.api.utilities.message
import org.bukkit.entity.Player
import java.util.UUID

object KitOwnership {

    fun ownsKit(playerId: UUID, kitName: String): Boolean {
        val playerDataModel = PlayerData.getInstance(KnockBackFFA.instance).getPlayerDataModel(playerId)
        return kitName == "default" || playerDataModel.ownedKits.contains(kitName)
    }

    fun getOwnedKits(playerId: UUID): List<String> {
        val playerDataModel = PlayerData.getInstance(KnockBackFFA.instance).getPlayerDataModel(playerId)
        val ownedKits = playerDataModel.ownedKits.toMutableList()
        if (!ownedKits.contains("default")) {
            ownedKits.add("default")
        }
        return ownedKits
    }

    fun addKit(playerId: UUID, kitName: String) {
        val playerData = PlayerData.getInstance(KnockBackFFA.instance)
        val playerDataModel = playerData.getPlayerDataModel(playerId)

        if (!playerDataModel.ownedKits.contains(kitName)) {
            val updatedKits = playerDataModel.ownedKits.toMutableList().apply {
                add(kitName)
            }
            playerDataModel.ownedKits = updatedKits
            playerData.savePlayerDataModel(playerId, playerDataModel)
        }
    }

    fun buyKit(player: Player, kitName: String): Boolean {
        val plugin = KnockBackFFA.instance
        val kit = KnockBackFFA.kitManager.getKit(kitName)

        if (ownsKit(player.uniqueId, kitName)) {
            player.message(TranslationManager.translate("kit.shop.already_owned"))
            return false
        }

        val playerData = PlayerData.getInstance(plugin)
        val playerDataModel = playerData.getPlayerDataModel(player.uniqueId)

        if (playerDataModel.coins < kit.price) {
            player.message(TranslationManager.translate("kit.shop.not_enough_coins"))
            return false
        }

        playerDataModel.coins -= kit.price

        val updatedKits = playerDataModel.ownedKits.toMutableList().apply {
            add(kitName)
        }
        playerDataModel.ownedKits = updatedKits

        playerData.savePlayerDataModel(player.uniqueId, playerDataModel)
        return true
    }
}