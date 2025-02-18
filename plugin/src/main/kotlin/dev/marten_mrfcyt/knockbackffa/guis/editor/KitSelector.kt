package dev.marten_mrfcyt.knockbackffa.guis.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.kits.loadKit
import dev.marten_mrfcyt.knockbackffa.utils.*
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
import mlib.api.gui.Gui
import mlib.api.gui.GuiSize
import mlib.api.utilities.*
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import java.io.File
import kotlin.math.ceil

@Suppress("unused")
class KitSelector(private val plugin: KnockBackFFA, source: Player) {
    private val config = File("${plugin.dataFolder}/kits.yml")
    private val kitConfig = YamlConfiguration.loadConfiguration(config)
    private val kits = kitConfig.getConfigurationSection("kit")?.getKeys(false)
    private val kitCount = kits?.size ?: 0
    private val inventorySize = ceil((kitCount + 1) / 9.0).toInt()

    init {
        val gui = Gui("<gray>Please select a kit</gray>".asMini(), GuiSize.fromRows(inventorySize)).apply {
            kits?.forEachIndexed { index, kit ->
                item(Material.valueOf(kitConfig.getString("kit.$kit.show.DisplayItem.item") ?: "BARRIER")) {
                    name("<!italic>${kitConfig.getString("kit.$kit.show.DisplayName")}".asMini())
                    description(listOf((kitConfig.getString("kit.$kit.show.Lore"))?.asMini() ?: "".asMini()))
                    slots(index)
                    onClick { event ->
                        openKit(event, kit)
                    }
                }
            }
        }
        gui.open(source)
    }

    private fun openKit(event: InventoryClickEvent, kitName: String) {
        val player = event.whoClicked as Player
        event.isCancelled = true
        val playerData = PlayerData.getInstance(plugin)
        val playerDataConfig = playerData.getPlayerData(player.uniqueId)
        playerDataConfig.set("kit", kitName)
        playerData.savePlayerData(player.uniqueId, playerDataConfig)
        loadKit(plugin, player)
        player.inventory.close()
        player.message(translate("player.kit_selected", "kit_name" to kitName))
    }
}