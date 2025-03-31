package dev.marten_mrfcyt.knockbackffa.kits.models

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import mlib.api.utilities.asMini
import mlib.api.utilities.setCustomValue
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.io.File

abstract class ModifyObject(
    open val id: String,
    open val name: String,
    open val description: List<String>,
    open val icon: Material,
    open val args: List<Pair<String, Class<*>>> = emptyList(),
    open val plugin: KnockBackFFA
) {
    abstract fun handle(player: Player, item: ItemStack, args: Map<String, Any>)

    fun createGuiItem(kitName: String, slot: Int, modifyObject: ModifyObject): ItemStack {
        val descriptionWithStatus = description.toMutableList()
        val kitConfig = YamlConfiguration.loadConfiguration(File("${plugin.dataFolder}/kits.yml"))
        val item = ItemStack(icon)
        val meta: ItemMeta = item.itemMeta

        if (kitConfig.getBoolean("kit.$kitName.items.$slot.modifiers.$id", false)) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            descriptionWithStatus.add("<!i><green>Enabled")
            args.forEach { (key, _) ->
                val value = kitConfig.get("kit.$kitName.items.$slot.modifiers.$key")
                descriptionWithStatus.add("<dark_gray>$key:<white> $value")
            }
        } else {
            descriptionWithStatus.add("<!i><red>Disabled")
        }

        meta.displayName("<!italic><white>$name".asMini())
        meta.lore(descriptionWithStatus.map { "<reset><gray>$it".asMini() })
        setCustomValue(meta, plugin, "type", "modifier")
        setCustomValue(meta, plugin, "modifier", modifyObject.id)
        setCustomValue(meta, plugin, "kit_name", kitName)
        setCustomValue(meta, plugin, "slot", slot)
        if (modifyObject.args.isNotEmpty()) {
            setCustomValue(meta, plugin, "args", modifyObject.args)
        }
        item.itemMeta = meta
        return item
    }
}