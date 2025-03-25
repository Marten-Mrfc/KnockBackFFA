// src/main/kotlin/dev/marten_mrfcyt/knockbackffa/guis/shop/ShopCategorySelector.kt
package dev.marten_mrfcyt.knockbackffa.guis.shop

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
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
        // Fill with glass panes
        gui.fill(Material.BLACK_STAINED_GLASS_PANE) {}

        // Kits category
        gui.item(Material.DIAMOND_SWORD) {
            name("<yellow>Kits".asMini())
            description(listOf(
                "<gray>Buy and select different kits".asMini(),
                "<gray>to use in the game.".asMini(),
                "".asMini(),
                "<white>Click to view!".asMini()
            ))
            slots(11)
            onClick { event ->
                event.isCancelled = true
                KitShop(plugin, player)
            }
        }

        // Cosmetics category (coming soon)
        gui.item(Material.NETHER_STAR) {
            name("<yellow>Cosmetics".asMini())
            description(listOf(
                "<gray>Customize your appearance".asMini(),
                "<gray>with special effects.".asMini(),
                "".asMini(),
                "<red>Coming soon!".asMini()
            ))
            slots(13)
            onClick { event ->
                event.isCancelled = true
                player.sendMessage("Coming soon!")
            }
        }

        // Boosts category (coming soon)
        gui.item(Material.EXPERIENCE_BOTTLE) {
            name("<yellow>Boosts".asMini())
            description(listOf(
                "<gray>Get temporary advantages".asMini(),
                "<gray>and bonuses.".asMini(),
                "".asMini(),
                "<red>Coming soon!".asMini()
            ))
            slots(15)
            onClick { event ->
                event.isCancelled = true
                player.sendMessage("Coming soon!")
            }
        }

        // Back button
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