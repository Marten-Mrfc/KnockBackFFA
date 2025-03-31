package dev.marten_mrfcyt.knockbackffa.guis.editor.kit

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import mlib.api.gui.GuiSize
import mlib.api.gui.types.builder.PaginatedGuiBuilder
import mlib.api.gui.types.builder.StandardGuiBuilder
import mlib.api.utilities.asMini
import mlib.api.utilities.message
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

class KitBoostManager(
    private val plugin: KnockBackFFA,
    private val player: Player,
    private val kitName: String
) {
    fun openBoostManager() {
        val kit = KnockBackFFA.kitManager.getKit(kitName)

        val gui = StandardGuiBuilder()
            .title("<gray>Kit Boosts:</gray><white> ${kit.displayName}".asMini())
            .size(GuiSize.ROW_FOUR)
            .setup { standardGui ->
                standardGui.fill(Material.GRAY_STAINED_GLASS_PANE) {
                    name(Component.text(""))
                    onClick { event -> event.isCancelled = true }
                }

                standardGui.item(Material.EXPERIENCE_BOTTLE) {
                    name("<green>Add Boost".asMini())
                    description(listOf(
                        "<gray>Click to add a boost".asMini(),
                        "<gray>to this kit".asMini()
                    ))
                    slots(11)
                    onClick { event ->
                        event.isCancelled = true
                        BoostSelectorForKit(plugin, player, kitName).openSelector()
                    }
                }

                standardGui.item(Material.ENCHANTED_BOOK) {
                    name("<yellow>Current Boosts".asMini())
                    description(
                        if (kit.boosts.isEmpty()) {
                            listOf("<gray>No boosts assigned to this kit".asMini())
                        } else {
                            listOf("<gray>Click to view and manage".asMini(),
                                "<gray>current boosts".asMini())
                        }
                    )
                    slots(15)
                    onClick { event ->
                        event.isCancelled = true
                        openCurrentBoosts()
                    }
                }

                standardGui.item(Material.ARROW) {
                    name("<yellow>« Back".asMini())
                    slots(31)
                    onClick { event ->
                        event.isCancelled = true
                        EditKit(plugin).kitEditor(
                            player,
                            kit.displayName.asMini(),
                            kit.description.asMini(),
                            kitName,
                            false
                        )
                    }
                }
            }
            .build()

        gui.open(player)
    }

    private fun openCurrentBoosts() {
        val kit = KnockBackFFA.kitManager.getKit(kitName)

        if (kit.boosts.isEmpty()) {
            player.message("<red>This kit has no boosts!")
            return
        }

        val builder = PaginatedGuiBuilder()
            .title("<dark_gray>Manage Kit Boosts <gray>» <white>${kit.displayName}".asMini())
            .size(GuiSize.ROW_SIX)
            .setBackground(Material.BLACK_STAINED_GLASS_PANE)

        kit.boosts.forEachIndexed { _, boostId ->
            try {
                val boost = plugin.boostManager.getBoost(boostId)
                builder.addItem(
                    boost.icon,
                    "<yellow>${boost.name}".asMini(),
                    listOf(
                        *boost.description.map { "<gray>$it".asMini() }.toTypedArray(),
                        "".asMini(),
                        "<red>Click to remove from kit".asMini()
                    ),
                    1
                )
            } catch (_: Exception) {
                builder.addItem(
                    Material.BARRIER,
                    "<red>Invalid Boost: $boostId".asMini(),
                    listOf("<gray>This boost no longer exists".asMini(),
                        "<red>Click to remove from kit".asMini()),
                    1
                )
            }
        }

        builder.onItemClick { clickedPlayer, _, index ->
            val boostId = kit.boosts.getOrNull(index) ?: return@onItemClick
            kit.removeBoost(boostId)

            clickedPlayer.message("<yellow>Removed boost $boostId from kit ${kit.name}")

            openBoostManager()
        }

        builder.customizeGui { gui ->
            gui.item(Material.ARROW) {
                name("<yellow>« Back".asMini())
                slots(49)
                onClick { event ->
                    event.isCancelled = true
                    openBoostManager()
                }
            }
        }

        builder.build().open(player)
    }
}