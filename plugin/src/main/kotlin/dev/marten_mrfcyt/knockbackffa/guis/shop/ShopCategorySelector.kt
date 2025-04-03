package dev.marten_mrfcyt.knockbackffa.guis.shop

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.guis.shop.boosts.BoostShop
import dev.marten_mrfcyt.knockbackffa.guis.shop.kit.KitShop
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import mlib.api.gui.GuiSize
import mlib.api.gui.types.StandardGui
import mlib.api.utilities.asMini
import org.bukkit.Material
import org.bukkit.entity.Player

class ShopCategorySelector(private val plugin: KnockBackFFA, private val player: Player) {
    private val gui: StandardGui = StandardGui(TranslationManager.translate("shop.categories.title").asMini(), GuiSize.ROW_THREE)

    init {
        setupGui()
        gui.open(player)
    }

    private fun setupGui() {
        gui.fill(Material.BLACK_STAINED_GLASS_PANE) {}

        gui.item(Material.DIAMOND_SWORD) {
            name(TranslationManager.translate("shop.categories.kits.name").asMini())
            description(TranslationManager.getStringList("shop.categories.kits.description").map { it.asMini() })
            slots(12)
            onClick { event ->
                event.isCancelled = true
                KitShop(plugin, player)
            }
        }

        gui.item(Material.EXPERIENCE_BOTTLE) {
            name(TranslationManager.translate("shop.categories.boosts.name").asMini())
            description(TranslationManager.getStringList("shop.categories.boosts.description").map { it.asMini() })
            slots(14)
            onClick { event ->
                event.isCancelled = true
                BoostShop(plugin, player)
            }
        }

        gui.item(Material.BARRIER) {
            name(TranslationManager.translate("shop.categories.close").asMini())
            slots(22)
            onClick { event ->
                event.isCancelled = true
                player.closeInventory()
            }
        }
    }
}