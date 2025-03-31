package dev.marten_mrfcyt.knockbackffa.guis.editor.kit

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.KitOwnership
import dev.marten_mrfcyt.knockbackffa.utils.*
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
import mlib.api.gui.GuiSize
import mlib.api.gui.types.builder.StandardGuiBuilder
import mlib.api.utilities.*
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import kotlin.math.ceil

class KitSelector(private val plugin: KnockBackFFA, source: Player) {
    private val kitNames = KitOwnership.getOwnedKits(source.uniqueId)
    private val kitCount = kitNames.size
    private val inventorySize = ceil((kitCount + 1) / 9.0).toInt() * 9

    init {
        val guiSize = GuiSize.fromRows(inventorySize / 9)

        val gui = StandardGuiBuilder()
            .title(translate("kit.selector.title").asMini())
            .size(guiSize)
            .setup { standardGui ->
                kitNames.forEachIndexed { index, kitName ->
                    val kit = KnockBackFFA.kitManager.getKit(kitName)

                    standardGui.item(kit.displayIcon) {
                        name(translate("kit.selector.item_name", "name" to kit.displayName).asMini())
                        description(listOf(translate("kit.selector.item_description", "description" to kit.description).asMini()))
                        slots(index)
                        onClick { event -> openKit(event, kitName) }
                    }
                }
            }
            .build()

        gui.open(source)
    }

    private fun openKit(event: InventoryClickEvent, kitName: String) {
        val player = event.whoClicked as Player
        event.isCancelled = true

        val playerData = PlayerData.getInstance(plugin)
        val playerDataConfig = playerData.getPlayerData(player.uniqueId)

        playerDataConfig.set("kit", kitName)
        playerData.savePlayerData(player.uniqueId, playerDataConfig)

        if (KnockBackFFA.kitManager.applyKit(player, kitName)) {
            player.message(translate("player.kit_applied", "kit_name" to kitName))
        }

        player.closeInventory()
    }
}