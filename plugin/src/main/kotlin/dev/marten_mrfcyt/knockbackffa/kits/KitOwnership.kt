// src/main/kotlin/dev/marten_mrfcyt/knockbackffa/kits/KitOwnership.kt
package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.util.UUID

object KitOwnership {
    /**
     * Checks if a player owns a specific kit
     */
    fun ownsKit(playerId: UUID, kitName: String): Boolean {
        val playerData = PlayerData.getInstance(KnockBackFFA.instance).getPlayerData(playerId)
        val ownedKits = playerData.getStringList("owned_kits")
        return kitName == "default" || ownedKits.contains(kitName)
    }

    /**
     * Get a list of all kits owned by player
     */
    fun getOwnedKits(playerId: UUID): List<String> {
        val playerData = PlayerData.getInstance(KnockBackFFA.instance).getPlayerData(playerId)
        val ownedKits = playerData.getStringList("owned_kits").toMutableList()
        // Always add default kit
        if (!ownedKits.contains("default")) {
            ownedKits.add("default")
        }
        return ownedKits
    }

    /**
     * Add a kit to a player's owned kits
     */
    fun addKit(playerId: UUID, kitName: String) {
        val playerData = PlayerData.getInstance(KnockBackFFA.instance).getPlayerData(playerId)
        val ownedKits = playerData.getStringList("owned_kits").toMutableList()
        if (!ownedKits.contains(kitName)) {
            ownedKits.add(kitName)
            playerData.set("owned_kits", ownedKits)
            PlayerData.getInstance(KnockBackFFA.instance).savePlayerData(playerId, playerData)
        }
    }

    /**
     * Buy a kit for a player if they have enough coins
     * @return true if purchase was successful
     */
    fun buyKit(player: Player, kitName: String): Boolean {
        val plugin = KnockBackFFA.instance
        val kit = KnockBackFFA.kitManager.getKit(kitName)

        // Check if player already owns the kit
        if (ownsKit(player.uniqueId, kitName)) {
            return false
        }

        // Check if player has enough coins
        val playerData = PlayerData.getInstance(plugin).getPlayerData(player.uniqueId)
        val coins = playerData.getInt("coins", 0)

        if (coins < kit.price) {
            return false
        }

        // Deduct coins and add kit
        playerData.set("coins", coins - kit.price)
        addKit(player.uniqueId, kitName)
        PlayerData.getInstance(plugin).savePlayerData(player.uniqueId, playerData)

        return true
    }
}