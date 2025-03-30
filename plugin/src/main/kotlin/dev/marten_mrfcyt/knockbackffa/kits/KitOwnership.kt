package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import mlib.api.utilities.message
import org.bukkit.entity.Player
import java.util.UUID

object KitOwnership {

    fun ownsKit(playerId: UUID, kitName: String): Boolean {
        val playerData = PlayerData.getInstance(KnockBackFFA.instance).getPlayerData(playerId)
        val ownedKits = playerData.getStringList("owned_kits")
        return kitName == "default" || ownedKits.contains(kitName)
    }

    fun getOwnedKits(playerId: UUID): List<String> {
        val playerData = PlayerData.getInstance(KnockBackFFA.instance).getPlayerData(playerId)
        val ownedKits = playerData.getStringList("owned_kits").toMutableList()
        // Always add default kit
        if (!ownedKits.contains("default")) {
            ownedKits.add("default")
        }
        return ownedKits
    }

    fun addKit(playerId: UUID, kitName: String) {
        val playerData = PlayerData.getInstance(KnockBackFFA.instance).getPlayerData(playerId)
        val ownedKits = playerData.getStringList("owned_kits").toMutableList()
        if (!ownedKits.contains(kitName)) {
            ownedKits.add(kitName)
            playerData.set("owned_kits", ownedKits)
            PlayerData.getInstance(KnockBackFFA.instance).savePlayerData(playerId, playerData)
        }
    }

    fun buyKit(player: Player, kitName: String): Boolean {
        val plugin = KnockBackFFA.instance
        val kit = KnockBackFFA.kitManager.getKit(kitName)

        if (ownsKit(player.uniqueId, kitName)) {
            player.message("<red>You already own this kit!")
            return false
        }

        val playerData = PlayerData.getInstance(plugin).getPlayerData(player.uniqueId)
        val coins = playerData.getInt("coins", 0)

        if (coins < kit.price) {
            player.message("<red>You don't have enough coins to buy this kit!")
            return false
        }

        playerData.set("coins", coins - kit.price)
        PlayerData.getInstance(plugin).savePlayerData(player.uniqueId, playerData)
        addKit(player.uniqueId, kitName)
        return true
    }
}