package dev.marten_mrfcyt.knockbackffa.kits.guis.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.handlers.ModifyHandler
import dev.marten_mrfcyt.knockbackffa.utils.asMini
import dev.marten_mrfcyt.knockbackffa.utils.error
import dev.marten_mrfcyt.knockbackffa.utils.setCustomValue
import lirand.api.extensions.inventory.set
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.io.File

class ItemModifier(private val plugin: KnockBackFFA) {
    private val modifyHandler = ModifyHandler()

    fun editKitItem(source: CommandSender, kitName: String, slot: Int) {
        val config = File("${plugin.dataFolder}/kits.yml")
        val kitConfig = YamlConfiguration.loadConfiguration(config)
        if (source is Player) {
            val inventorySize = 18
            val edittext = "<!italic><gray>Editing slot:</gray><white> $slot".asMini()
            val inventory = Bukkit.createInventory(null, inventorySize, edittext)
            for (i in 0..17) {
                if (i < 8 || i > 8) {
                    val glassPane = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
                    val glassMeta: ItemMeta = glassPane.itemMeta
                    glassMeta.displayName("".asMini())
                    setCustomValue(glassMeta, plugin, "is_draggable", false)
                    glassPane.itemMeta = glassMeta
                    inventory[i] = glassPane
                }
            }
            // show item
            val kitItem = kitConfig.getConfigurationSection("kit.$kitName.items.$slot")

            if (kitItem != null) {
                val item = KitModifier(plugin).loadItemData(kitItem, kitName, true)
                val itemMeta = item?.itemMeta
                if (itemMeta != null) {
                    setCustomValue(itemMeta, plugin, "slot", slot)
                    setCustomValue(itemMeta, plugin, "edit_kit_item", true)
                    item.itemMeta = itemMeta
                }
                inventory[13] = item
            }

            // delete button
            val deleteButton = ItemStack(Material.RED_CONCRETE)
            val deleteButtonMeta: ItemMeta = deleteButton.itemMeta
            deleteButtonMeta.displayName("<!italic><red>Delete Item".asMini())
            setCustomValue(deleteButtonMeta, plugin, "64656C6574655F6974656D", "delete_item")
            setCustomValue(deleteButtonMeta, plugin, "kit_name", kitName)
            setCustomValue(deleteButtonMeta, plugin, "slot", slot)
            deleteButton.itemMeta = deleteButtonMeta
            inventory[7] = deleteButton

            // go back button
            val goBack = ItemStack(Material.BARRIER)
            val goBackMeta: ItemMeta = goBack.itemMeta
            goBackMeta.displayName("<!italic><gray>Go Back".asMini())
            setCustomValue(goBackMeta, plugin, "kit_name", kitName)
            setCustomValue(goBackMeta, plugin, "676F5F6261636B5F627574746F6E", "go_back_button")
            setCustomValue(goBackMeta, plugin, "menu", "edit_kit_gui")
            goBack.itemMeta = goBackMeta
            inventory[8] = goBack

            // Load dynamic GUI elements
            modifyHandler.getModifyObjects().forEach { modifyObject ->
                val guiItem = modifyObject.createGuiItem(kitName, slot, modifyObject)

                // Find the first available slot that is either empty or contains a glassPane
                for (i in 0 until inventory.size) {
                    val item = inventory.getItem(i)
                    if (item == null || item.type == Material.GRAY_STAINED_GLASS_PANE) {
                        inventory.setItem(i, guiItem)
                        break
                    }
                }
            }
            kitConfig.save(config)
            source.openInventory(inventory)
        } else {
            source.error("You must be a player to use this command!")
        }
    }
}