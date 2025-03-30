// src/main/kotlin/dev/marten_mrfcyt/knockbackffa/guis/shop/ShopCategorySelector.kt
package dev.marten_mrfcyt.knockbackffa.guis.shop

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.guis.shop.boosts.BoostShop
import dev.marten_mrfcyt.knockbackffa.guis.shop.kit.KitShop
import mlib.api.gui.GuiSize
import mlib.api.gui.types.StandardGui
import mlib.api.utilities.asMini
import org.bukkit.Material
import org.bukkit.entity.Player

class ShopCategorySelector(private val plugin: KnockBackFFA, private val player: Player) {
    private val gui: StandardGui = StandardGui("<dark_gray>Shop <gray>» <white>Categories".asMini(), GuiSize.ROW_THREE)

    init {
        setupGui()
        gui.open(player)
    }

    private fun setupGui() {
        gui.fill(Material.BLACK_STAINED_GLASS_PANE) {}

        gui.item(Material.DIAMOND_SWORD) {
            name("<yellow>Kits".asMini())
            description(listOf(
                "<gray>Buy and select different kits".asMini(),
                "<gray>to use in the game.".asMini(),
                "".asMini(),
                "<white>Click to view!".asMini()
            ))
            slots(12)
            onClick { event ->
                event.isCancelled = true
                KitShop(plugin, player)
            }
        }

        gui.item(Material.EXPERIENCE_BOTTLE) {
            name("<yellow>Boosts".asMini())
            description(listOf(
                "<gray>Get temporary advantages".asMini(),
                "<gray>and bonuses.".asMini(),
                "".asMini(),
                "<white>Click to view!".asMini()
            ))
            slots(14)
            onClick { event ->
                event.isCancelled = true
                BoostShop(plugin, player)
            }
        }

        gui.item(Material.BARRIER) {
            name("<red>Close".asMini())
            slots(22)
            onClick { event ->
                event.isCancelled = true
                player.closeInventory()
            }
        }
    }
}