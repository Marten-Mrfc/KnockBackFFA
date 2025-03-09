package dev.marten_mrfcyt.knockbackffa.guis.shop

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.*
import mlib.api.gui.Gui
import mlib.api.gui.GuiSize
import mlib.api.utilities.*
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.io.File
import kotlin.math.min

class ShopGUI(private val plugin: KnockBackFFA, private val player: Player) {
    private val config: File = File(plugin.dataFolder, "shop.yml")
    private val shopConfig: YamlConfiguration = YamlConfiguration.loadConfiguration(config)
    private val playerData = PlayerData.getInstance(plugin)

    init {
        openMainShop()
    }

    private fun openMainShop() {
        val categories = shopConfig.getConfigurationSection("categories")?.getKeys(false)?.toList() ?: emptyList()
        val rows = min(6, (categories.size / 9) + 1)

        val gui = Gui("<gray>KnockBackFFA Shop".asMini(), GuiSize.fromRows(rows)).apply {
            // Add categories
            categories.forEachIndexed { index, category ->
                val section = shopConfig.getConfigurationSection("categories.$category") ?: return@forEachIndexed
                val material = Material.valueOf(section.getString("icon") ?: "BARRIER")
                val name = section.getString("display-name")?.asMini() ?: category.asMini()
                val description = section.getStringList("description").map { it.asMini() }

                item(material) {
                    name(name)
                    description(description)
                    slots(index)
                    onClick { event -> openCategoryShop(event, category) }
                }
            }

            // Add balance display
            item(Material.GOLD_INGOT) {
                val balance = playerData.getPlayerData(player.uniqueId).getInt("coins", 0)
                name("<yellow>Your Balance: $balance coins".asMini())
                description(listOf("<gray>Click to refresh".asMini()))
                slots(rows * 9 - 1)
                onClick { event ->
                    event.isCancelled = true
                    openMainShop()
                }
            }
        }

        gui.open(player)
    }

    private fun openCategoryShop(event: InventoryClickEvent, category: String) {
        event.isCancelled = true

        val items = shopConfig.getConfigurationSection("categories.$category.items")?.getKeys(false)?.toList() ?: emptyList()
        val rows = min(6, (items.size / 9) + 1)

        val gui = Gui("<gray>Shop: $category".asMini(), GuiSize.fromRows(rows)).apply {
            // Add items
            items.forEachIndexed { index, itemId ->
                val section = shopConfig.getConfigurationSection("categories.$category.items.$itemId") ?: return@forEachIndexed
                val material = Material.valueOf(section.getString("material") ?: "BARRIER")
                val name = section.getString("display-name")?.asMini() ?: itemId.asMini()
                val description = section.getStringList("description").map { it.asMini() }
                val price = section.getInt("price", 0)
                val playerOwns = playerData.getPlayerData(player.uniqueId).getBoolean("purchases.$category.$itemId", false)

                val lore = mutableListOf<net.kyori.adventure.text.Component>()
                lore.addAll(description)
                lore.add("".asMini())
                lore.add("<yellow>Price: $price coins".asMini())
                if (playerOwns) {
                    lore.add("<green>Purchased".asMini())
                } else {
                    lore.add("<red>Not Purchased".asMini())
                }

                item(material) {
                    name(name)
                    description(lore)
                    slots(index)
                    onClick { event -> handlePurchase(event, category, itemId) }
                }
            }

            // Back button
            item(Material.BARRIER) {
                name("<gray>Back to Categories".asMini())
                description(listOf("<gray>Click to go back".asMini()))
                slots(rows * 9 - 1)
                onClick { event ->
                    event.isCancelled = true
                    openMainShop()
                }
            }
        }

        gui.open(player)
    }

    private fun handlePurchase(event: InventoryClickEvent, category: String, itemId: String) {
        event.isCancelled = true

        val section = shopConfig.getConfigurationSection("categories.$category.items.$itemId") ?: return
        val price = section.getInt("price", 0)
        val playerDataConfig = playerData.getPlayerData(player.uniqueId)
        val currentBalance = playerDataConfig.getInt("coins", 0)
        val alreadyPurchased = playerDataConfig.getBoolean("purchases.$category.$itemId", false)

        if (alreadyPurchased) {
            player.message(TranslationManager.translate("shop.already_purchased"))
            return
        }

        if (currentBalance < price) {
            player.message(TranslationManager.translate("shop.insufficient_funds"))
            return
        }

        // Process purchase
        playerDataConfig.set("coins", currentBalance - price)
        playerDataConfig.set("purchases.$category.$itemId", true)
        playerData.savePlayerData(player.uniqueId, playerDataConfig)

        // Execute purchase actions
        val actions = section.getStringList("actions")
        executeActions(actions, player)

        player.message(TranslationManager.translate("shop.purchase_success", "item_name" to (section.getString("display-name", itemId) ?: "")))

        // Reopen the category to show updated status
        openCategoryShop(event, category)
    }

    private fun executeActions(actions: List<String>, player: Player) {
        actions.forEach { action ->
            when {
                action.startsWith("command:") -> {
                    val cmd = action.substring(8).replace("%player%", player.name)
                    plugin.server.dispatchCommand(plugin.server.consoleSender, cmd)
                }
                action.startsWith("unlock_kit:") -> {
                    val kitName = action.substring(11)
                    val playerDataConfig = playerData.getPlayerData(player.uniqueId)
                    playerDataConfig.set("unlocked_kits.$kitName", true)
                    playerData.savePlayerData(player.uniqueId, playerDataConfig)
                }
                action.startsWith("give_item:") -> {
                    val itemData = action.substring(10).split(":")
                    if (itemData.size >= 2) {
                        val material = Material.valueOf(itemData[0])
                        val amount = itemData[1].toIntOrNull() ?: 1
                        player.inventory.addItem(ItemStack(material, amount))
                    }
                }
            }
        }
    }
}