// src/main/kotlin/dev/marten_mrfcyt/knockbackffa/guis/editor/KitSelector.kt
package dev.marten_mrfcyt.knockbackffa.guis.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.*
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
import mlib.api.gui.GuiSize
import mlib.api.gui.types.builder.StandardGuiBuilder
import mlib.api.utilities.*
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import kotlin.math.ceil

class KitSelector(private val plugin: KnockBackFFA, source: Player) {
    private val kitNames = KnockBackFFA.kitManager.getAllKitNames()
    private val kitCount = kitNames.size
    private val inventorySize = ceil((kitCount + 1) / 9.0).toInt() * 9

    init {
        val guiSize = GuiSize.fromRows(inventorySize / 9)

        val gui = StandardGuiBuilder()
            .title("<gray>Please select a kit</gray>".asMini())
            .size(guiSize)
            .setup { standardGui ->
                kitNames.forEachIndexed { index, kitName ->
                    val kit = KnockBackFFA.kitManager.getKit(kitName) ?: return@forEachIndexed

                    standardGui.item(kit.displayIcon) {
                        name("<!italic>${kit.displayName}".asMini())
                        description(listOf(kit.description.asMini()))
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

        // Get and apply the kit
        KnockBackFFA.kitManager.applyKit(player, kitName)

        player.closeInventory()
        player.message(translate("player.kit_selected", "kit_name" to kitName))
    }
}