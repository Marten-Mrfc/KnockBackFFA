package dev.marten_mrfcyt.knockbackffa.guis.editor.boosts

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.boosts.models.Boost
import mlib.api.gui.GuiSize
import mlib.api.gui.types.PaginatedGui
import mlib.api.gui.types.builder.PaginatedGuiBuilder
import mlib.api.utilities.asMini
import org.bukkit.Material
import org.bukkit.entity.Player

class BoostSelector(private val plugin: KnockBackFFA, private val player: Player) {

    init {
        val gui = createGui()
        gui.open(player)
    }

    private fun createGui(): PaginatedGui {
        val builder = PaginatedGuiBuilder()
            .title("<dark_gray>Boosts <gray>» <white>Editor".asMini())
            .size(GuiSize.ROW_SIX)
            .setBackground(Material.BLACK_STAINED_GLASS_PANE)

        val boosts = try {
            plugin.boostManager.getAllBoosts()
        } catch (_: UninitializedPropertyAccessException) {
            player.sendMessage("<red>Boost manager is not initialized yet!".asMini())
            emptyList<Boost>()
        }

        boosts.forEach { boost ->
            builder.addItem(
                boost.icon,
                "<yellow>${boost.name}".asMini(),
                listOf(
                    "<gray>${boost.description.joinToString(" ")}".asMini(),
                    "".asMini(),
                    "<gray>ID: <white>${boost.id}".asMini(),
                    "<gray>Price: <gold>${boost.price} coins".asMini(),
                    "".asMini(),
                    "<yellow>Click to edit!".asMini()
                ),
                1
            )
        }

        builder.onItemClick { clickedPlayer, _, index ->
            val boostsList = try {
                plugin.boostManager.getAllBoosts().toList()
            } catch (_: UninitializedPropertyAccessException) {
                emptyList()
            }

            val boost = boostsList.getOrNull(index) ?: return@onItemClick

            clickedPlayer.sendMessage("<yellow>Editing boost: ${boost.name}".asMini())
            BoostEditor(plugin, clickedPlayer, boost)
        }

        builder.customizeGui { gui ->
            gui.item(Material.BARRIER) {
                name("<red>Close".asMini())
                slots(49)
                onClick { event ->
                    event.isCancelled = true
                    player.closeInventory()
                }
            }
        }

        return builder.build()
    }
}