package dev.marten_mrfcyt.knockbackffa.guis.editor.arena

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.arena.utils.ArenaModel
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager.Companion.translate
import mlib.api.gui.GuiSize
import mlib.api.gui.types.StandardGui
import mlib.api.utilities.asMini
import mlib.api.utilities.debug
import mlib.api.utilities.sendMini
import mlib.api.utilities.setCustomValue
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.*
import kotlin.math.ceil
import kotlin.math.min

class ArenaSettingsSelector(private val plugin: KnockBackFFA, private val player: Player) {
    
    init {
        val arenaNames = plugin.arenaHandler.getArenaNames()
        if (arenaNames.isEmpty()) {
            player.sendMini(translate("arena.settings.no_arenas"))
        }
        
        val rows = ceil(arenaNames.size / 9.0).toInt().coerceIn(1, 6)
        val gui = StandardGui(translate("arena.settings.selector_title").asMini(), GuiSize.fromRows(rows))
        
        setupGui(gui, arenaNames)
        gui.open(player)
    }
    
    private fun setupGui(gui: StandardGui, arenaNames: List<String>) {
        gui.fill(Material.BLACK_STAINED_GLASS_PANE) {}

        arenaNames.forEachIndexed { index, arenaName ->
            val arena = plugin.arenaHandler.loadArenaByName(arenaName) ?: return@forEachIndexed
            val item = createArenaItem(arena)
            
            gui.item(item.type) {
                name(item.itemMeta?.displayName() ?: arenaName.asMini())
                description(item.itemMeta?.lore() ?: listOf())
                slots(index)
                onClick { event -> 
                    event.isCancelled = true
                    ArenaSettingsGUI(plugin, player, arenaName)
                }
                meta(item.itemMeta)
            }
        }

        // Add a "close" button at the bottom
        val lastRow = (gui.size.slots - 5)
        gui.item(Material.BARRIER) {
            name(translate("arena.settings.close").asMini())
            slots(lastRow)
            onClick { event ->
                event.isCancelled = true
                player.closeInventory()
            }
        }
    }
    
    private fun createArenaItem(arena: ArenaModel): ItemStack {
        val item = ItemStack(arena.killBlock)
        val meta = item.itemMeta ?: run {
            val fallbackItem = ItemStack(Material.STONE)
            return fallbackItem.apply {
                itemMeta = itemMeta?.apply {
                    displayName(arena.name.asMini())
                }
            }
        }
        
        meta.displayName("<!italic><gold>${arena.name}</gold>".asMini())
        
        val lore = mutableListOf<String>()
        lore.add("<gray>Kill Block: <white>${arena.killBlock.name}")
        lore.add("<gray>Click to edit settings</gray>")
        
        meta.lore(lore.map { it.asMini() })
        setCustomValue(meta, plugin, "arena_name", arena.name)
        
        item.itemMeta = meta
        return item
    }
}
