package dev.marten_mrfcyt.knockbackffa.guis.shop.kit

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.guis.shop.ShopCategorySelector
import dev.marten_mrfcyt.knockbackffa.kits.KitOwnership
import dev.marten_mrfcyt.knockbackffa.utils.PlayerData
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import mlib.api.gui.GuiSize
import mlib.api.gui.types.PaginatedGui
import mlib.api.gui.types.builder.PaginatedGuiBuilder
import mlib.api.utilities.asMini
import mlib.api.utilities.message
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

class KitShop(private val plugin: KnockBackFFA, private val player: Player) {

    init {
        val gui = createGui()
        gui.open(player)
    }

    private fun createGui(): PaginatedGui {
        val builder = PaginatedGuiBuilder()
            .title(TranslationManager.translate("shop.kits.title").asMini())
            .size(GuiSize.ROW_SIX)
            .setBackground(Material.BLACK_STAINED_GLASS_PANE)

        val kitItems = mutableListOf<PaginatedGui.PaginatedItem>()
        val playerData = PlayerData.getInstance(plugin).getPlayerData(player.uniqueId)
        val ownedKits = KitOwnership.getOwnedKits(player.uniqueId)
        val coins = playerData.getInt("coins", 0)

        KnockBackFFA.kitManager.getAllKitNames().forEach { kitName ->
            val kit = KnockBackFFA.kitManager.getKit(kitName)

            val material = try {
                kit.displayIcon
            } catch (_: Exception) {
                Material.BARRIER
            }

            val owned = ownedKits.contains(kitName)
            val canAfford = coins >= kit.price

            val desc = mutableListOf<Component>()
            desc.add(kit.description.asMini())
            desc.add("".asMini())

            if (owned) {
                desc.add(TranslationManager.translate("shop.kits.owned").asMini())
                desc.add(TranslationManager.translate("shop.kits.click_to_select").asMini())
            } else {
                desc.add(TranslationManager.translate("shop.kits.price", "price" to kit.price).asMini())
                if (canAfford) {
                    desc.add(TranslationManager.translate("shop.kits.click_to_purchase").asMini())
                } else {
                    desc.add(TranslationManager.translate("shop.kits.not_enough_coins").asMini())
                }
            }

            kitItems.add(
                PaginatedGui.PaginatedItem(
                    material,
                    kit.displayName.asMini(),
                    desc.map { it },
                    1
                )
            )
        }

        kitItems.forEach { item ->
            builder.addItem(item.material, item.name, item.description, item.amount)
        }

        builder.onItemClick { _, _, index ->
            val kitName = KnockBackFFA.kitManager.getAllKitNames().getOrNull(index) ?: return@onItemClick
            val owned = KitOwnership.ownsKit(player.uniqueId, kitName)

            if (owned) {
                playerData.set("kit", kitName)
                PlayerData.getInstance(plugin).savePlayerData(player.uniqueId, playerData)
                player.message(TranslationManager.translate("shop.kits.selected", "kit_name" to kitName))
                player.closeInventory()
            } else {
                KitPurchaseConfirmation(plugin, player, kitName)
            }
        }

        builder.customizeGui { gui ->
            gui.item(Material.GOLD_INGOT) {
                name(TranslationManager.translate("shop.common.your_coins", "coins" to coins).asMini())
                slots(49)
                onClick {  event ->
                    event.isCancelled = true
                }
            }
            gui.item(Material.ARROW) {
                name(TranslationManager.translate("shop.common.back").asMini())
                slots(45)
                onClick { event ->
                    event.isCancelled = true
                    ShopCategorySelector(plugin, player)
                }
            }
        }

        return builder.build()
    }
}