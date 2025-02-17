package dev.marten_mrfcyt.knockbackffa.guis.editor

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import mlib.api.gui.Gui
import dev.marten_mrfcyt.knockbackffa.utils.*
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import mlib.api.gui.GuiSize
import mlib.api.utilities.*
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import java.io.File
import java.util.Locale
import kotlin.text.toInt

class EditKitItemSelector(private val plugin: KnockBackFFA, private val source: Player, private val kitName: String) {
    private val config = File("${plugin.dataFolder}/kits.yml")
    private val kitConfig = YamlConfiguration.loadConfiguration(config)
    private val inventoryTitle = "<!italic><gray>Editing kit:</gray><white> $kitName".asMini()

    fun initialize() {
        val gui = Gui(inventoryTitle, GuiSize.ROW_TWO).apply {
            item(Material.GRAY_STAINED_GLASS_PANE) {
                name("<gray>Click to edit slot</gray>".asMini())
                description(listOf("<dark_purple>Drag an item onto me".asMini(), "<dark_purple>to change me completely!".asMini()))
                slots(0, 1, 2, 3, 4, 5, 6,7, 9, 10, 11, 12, 13, 14, 15, 16, 17)
                onClick { event -> addNewItem(event, kitName) }
            }
            loadKitItems(this, kitName)
            item(Material.BARRIER) {
                name("<!italic><gray>Go Back".asMini())
                description(listOf("Go back to the previous menu".asMini()))
                slots(8)
                onClick { event -> onGoBackClick(event, kitName) }
            }
        }
        gui.open(source)
    }

    private fun loadKitItems(gui: Gui, kitName: String) {
        val kitItemsSection = kitConfig.getConfigurationSection("kit.$kitName.items")
        kitItemsSection?.getKeys(false)?.forEach { slot ->
            val itemSection = kitItemsSection.getConfigurationSection(slot)
            if (itemSection != null) {
                val item = loadItemData(itemSection, kitName, true)
                val itemMeta = item?.itemMeta
                if (itemMeta != null) {
                    setCustomValue(itemMeta, plugin, "type", "edit_kit_item")
                    setCustomValue(itemMeta, plugin, "kit_name", kitName)
                    setCustomValue(itemMeta, plugin, "slot", slot.toInt())
                    item.itemMeta = itemMeta

                    val adjustedSlot = when (slot.toInt()) {
                        in 9..18 -> slot.toInt() - 9
                        in 0..8 -> slot.toInt() + 9
                        else -> return@forEach
                    }
                    println("adding item to slot $adjustedSlot")
                    gui.item(item.type) {
                        name(itemMeta.displayName() ?: "".asMini())
                        description(itemMeta.lore()?.map { it } ?: listOf())
                        amount(item.amount)
                        slots(adjustedSlot)
                        onClick { event -> onItemClick(event, kitName) }
                        meta(itemMeta)
                    }
                }
            }
        }
    }

    fun loadItemData(itemSelector: ConfigurationSection?, kitName: String, gui: Boolean): ItemStack? {
        val itemName = "<!italic>${itemSelector?.getString("name")}".asMini()
        val itemType = itemSelector?.getString("item")?.let { Material.getMaterial(it) }
        val itemAmount = itemSelector?.getInt("amount")
        val itemMetaModel = itemSelector?.getInt("meta.model")
        val itemMetaDurability = itemSelector?.getInt("meta.durability")
        val itemMetaUnbreakable = itemSelector?.getBoolean("meta.unbreakable")
        val itemMetaItemFlags = itemSelector?.getStringList("meta.itemFlags")?.map { ItemFlag.valueOf(it) }
        val enchantments = itemSelector?.getConfigurationSection("enchants")

        val itemStack = itemType?.let { ItemStack(it, itemAmount ?: 0) }
        val itemMeta: ItemMeta = itemStack?.itemMeta ?: return null
        itemMeta.lore(
            if (gui) {
                val line = "<gray>------------------<reset>".asMini()
                val toplore = "<dark_purple>Drag an item onto me".asMini()
                val bottomlore = "<dark_purple>to change me completely!".asMini()
                val lore = itemSelector.getStringList("lore").map { it.asMini() }
                lore.plus(line).plus(toplore).plus(bottomlore)
            } else {
                itemSelector.getStringList("lore").map { it.asMini() }
            })
        itemMeta.displayName(itemName)
        itemMeta.setCustomModelData(itemMetaModel)
        if (itemMeta is Damageable) {
            if (itemMetaDurability != null) {
                itemMeta.damage = itemMetaDurability
            }
        }
        if (itemMetaUnbreakable != null) {
            itemMeta.isUnbreakable = itemMetaUnbreakable
        }
        itemMetaItemFlags?.forEach { itemMeta.addItemFlags(it) }
        enchantments?.let { getEnchantments(it, itemMeta) }
        setCustomValue(itemMeta, plugin, "kit_name", kitName)
        itemStack.itemMeta = itemMeta

        return itemStack
    }

    private fun onItemClick(event: InventoryClickEvent, kitName: String) {
        println("Item clicked")
        val item = event.currentItem ?: return
        println("Item: $item")
        val slot = getCustomValue(item.itemMeta, plugin, "slot") as Int
        println("Slot: $slot")
        val player = event.whoClicked as Player
        println("Player: $player")
        ItemModifierGUI(plugin, player, kitName, slot)
    }

    private fun onGoBackClick(event: InventoryClickEvent, kitName: String) {
        val lore = kitConfig.getString("kit.$kitName.show.lore")
        (event.whoClicked as? Player)?.apply {
            EditKit(plugin).kitEditor(this, kitName.asMini(), lore?.asMini() ?: "".asMini(), kitName, new = false)
        }
    }

    fun getEnchantments(enchantments: ConfigurationSection?, itemMeta: ItemMeta) {
        val enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)

        enchantments?.getKeys(false)?.forEach { enchantmentKey ->
            val namespacedKey = NamespacedKey.minecraft(enchantmentKey.lowercase(Locale.getDefault()))

            @Suppress("UnstableApiUsage")
            val enchantment = enchantmentRegistry.getOrThrow(TypedKey.create(RegistryKey.ENCHANTMENT, namespacedKey))
            val level = enchantments.getInt(enchantmentKey)

            itemMeta.addEnchant(enchantment, level, true)
        }
    }

    private fun addNewItem(event: InventoryClickEvent, kitName: String) {
        val player = event.whoClicked as? Player ?: return
        val clickedItem = event.cursor
        val slot = event.slot

        event.isCancelled = true

        val adjustedSlot = when (slot.toInt()) {
            in 9..18 -> slot.toInt() - 9
            in 0..8 -> slot.toInt() + 9
            else -> return
        }

        val itemSection = kitConfig.createSection("kit.$kitName.items.$adjustedSlot")
        itemSection.set("name", clickedItem.itemMeta?.displayName()?.notMiniText())
        itemSection.set("lore", clickedItem.itemMeta?.lore()?.map { it.notMiniText() })
        itemSection.set("item", clickedItem.type.name)
        itemSection.set("amount", clickedItem.amount)
        itemSection.set("meta.durability", (clickedItem.itemMeta as? Damageable)?.damage)
        itemSection.set("meta.unbreakable", clickedItem.itemMeta?.isUnbreakable)
        itemSection.set("meta.itemFlags", clickedItem.itemMeta?.itemFlags?.map { it.name })
        itemSection.set("meta.model", clickedItem.itemMeta?.customModelData)

        kitConfig.save(config)

        player.message("Item added to the kit in slot $slot.")

        EditKitItemSelector(plugin, player, kitName).initialize()
    }
}