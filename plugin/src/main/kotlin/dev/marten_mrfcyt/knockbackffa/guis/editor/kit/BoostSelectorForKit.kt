package dev.marten_mrfcyt.knockbackffa.guis.editor.kit

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import mlib.api.gui.GuiSize
import mlib.api.gui.types.builder.PaginatedGuiBuilder
import mlib.api.utilities.asMini
import mlib.api.utilities.message
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File

class BoostSelectorForKit(
    private val plugin: KnockBackFFA,
    private val player: Player,
    private val kitName: String
) {
    fun openSelector() {
        val boosts = plugin.boostManager.getAllBoosts()

        val builder = PaginatedGuiBuilder()
            .title("<dark_gray>Select Boost for Kit <gray>» <white>$kitName".asMini())
            .size(GuiSize.ROW_SIX)
            .setBackground(Material.BLACK_STAINED_GLASS_PANE)

        boosts.forEach { boost ->
            builder.addItem(
                boost.icon,
                "<yellow>${boost.name}".asMini(),
                buildList {
                    boost.description.forEach { line ->
                        add("<gray>$line".asMini())
                    }
                    add("".asMini())
                    add("<gray>ID: <white>${boost.id}".asMini())
                    add("<gray>Price: <gold>${boost.price} coins".asMini())
                    add("".asMini())
                    add("<yellow>Click to add to kit!".asMini())
                },
                1
            )
        }

        builder.onItemClick { clickedPlayer, _, index ->
            if (index < 0 || index >= boosts.size) return@onItemClick
            val boost = boosts.toList()[index]
            addBoostToKit(clickedPlayer, kitName, boost.id)
        }

        builder.customizeGui { gui ->
            gui.item(Material.ARROW) {
                name("<yellow>« Back".asMini())
                slots(49)
                onClick { event ->
                    event.isCancelled = true
                    KitBoostManager(plugin, player, kitName).openBoostManager()
                }
            }
        }

        builder.build().open(player)
    }

    private fun addBoostToKit(player: Player, kitName: String, boostId: String) {
        val configFile = File(plugin.dataFolder, "kits.yml")
        val kitConfig = YamlConfiguration.loadConfiguration(configFile)

        val boostsList = kitConfig.getStringList("kit.$kitName.boosts").toMutableList()

        if (!boostsList.contains(boostId)) {
            boostsList.add(boostId)
            kitConfig.set("kit.$kitName.boosts", boostsList)
            kitConfig.save(configFile)

            player.message("<green>Added boost $boostId to kit ${kitName}!")

            KnockBackFFA.kitManager.reloadKits()
        } else {
            player.message("<red>This boost is already in the kit!")
        }

        openSelector()
    }
}