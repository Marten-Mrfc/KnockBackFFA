package dev.marten_mrfcyt.knockbackffa.guis.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import mlib.api.gui.GuiItemProcessor
import dev.marten_mrfcyt.knockbackffa.utils.*
import mlib.api.utilities.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.io.File
import kotlin.math.ceil

class EditKitSelector(private val plugin: KnockBackFFA, source: CommandSender) {
    private val config = File("${plugin.dataFolder}/kits.yml")
    private val kitConfig = YamlConfiguration.loadConfiguration(config)
    private val kits = kitConfig.getConfigurationSection("kit")?.getKeys(false)
    private val kitCount = kits?.size ?: 0
    private val inventorySize = ceil((kitCount + 1) / 9.0).toInt() * 9
    private val inventory: Inventory = Bukkit.createInventory(null, inventorySize, "<gray>Please select or create kit</gray>".asMini())

    init {
        if (source is Player) {
            kits?.forEachIndexed { index, kit ->
                val item = loadKitGUI(kitConfig, kit)
                inventory.setItem(index, item)
                GuiItemProcessor.registerClickHandler(inventory, index, this::openKitEditor)            }
            source.openInventory(inventory)
        } else {
            source.error("You must be a player to use this command!")
        }
    }

    private fun loadKitGUI(kitConfig: YamlConfiguration, kit: String): ItemStack {
        val kitSection = kitConfig.getConfigurationSection("kit.$kit.show") ?: return ItemStack(Material.BARRIER)
        val displayName = "<!italic>${kitSection.getString("DisplayName")}".asMini()
        val lore = kitSection.getString("Lore")?.asMini() ?: return ItemStack(Material.BARRIER)
        val displayItemMaterial = Material.getMaterial(kitSection.getString("DisplayItem.item") ?: return ItemStack(Material.BARRIER))
        val item = ItemStack(displayItemMaterial ?: Material.BARRIER)
        val meta = item.itemMeta ?: return ItemStack(Material.BARRIER)
        meta.displayName(displayName)
        meta.lore(listOf(lore))
        setCustomValue(meta, plugin, "kit_name", kit)
        item.itemMeta = meta
        return item
    }

    fun openKitEditor(event: InventoryClickEvent) {
        val item = event.currentItem ?: return
        val player = event.whoClicked as? Player ?: return
        val kitName = getCustomValue(item.itemMeta, plugin, "kit_name") as String
        val kitDisplayName = item.itemMeta?.displayName() ?: return
        val kitLore = item.itemMeta?.lore()?.joinToString("\n")?.asMini() ?: return

        EditKit(plugin).kitEditor(player, kitDisplayName, kitLore, kitName, new = false)
    }
}