import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import mlib.api.utilities.asMini
import mlib.api.utilities.setCustomValue
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import kotlin.collections.component1
import kotlin.collections.component2

data class KitItem(
    val name: String,
    val material: String,
    val amount: Int,
    val lore: List<String>,
    val enchantments: Map<String, Int>,
    val metadata: Map<String, Any>,
    val modifiers: Map<String, Any>,
    val kitName: String,
    val slot: Int
) {
    fun build(plugin: KnockBackFFA): ItemStack {
        val item = Material.valueOf(material).let { ItemStack(it) }
        val meta = item.itemMeta ?: plugin.server.itemFactory.getItemMeta(item.type) ?: return item

        // Apply basic properties
        meta.displayName(name.asMini())
        if (lore.isNotEmpty()) {
            meta.lore(lore.map { it.asMini() })
        }

        // Apply custom model data
        metadata["model"]?.let {
            if (it is Int && it >= 0) {
                meta.setCustomModelData(it)
            }
        }

        // Apply enchantments
        enchantments.forEach { (enchantName, level) ->
            val enchant = Enchantment.getByKey(NamespacedKey.minecraft(enchantName.lowercase())) ?: return@forEach
            meta.addEnchant(enchant, level, true)
        }

        // Apply metadata
        if (meta is Damageable && metadata.containsKey("durability")) {
            meta.damage = metadata["durability"] as Int
        }

        if (metadata.containsKey("unbreakable")) {
            meta.isUnbreakable = metadata["unbreakable"] as Boolean
        }

        if (metadata.containsKey("itemFlags")) {
            (metadata["itemFlags"] as List<*>).forEach {
                try {
                    meta.addItemFlags(ItemFlag.valueOf(it.toString()))
                } catch (e: Exception) {
                    plugin.logger.warning("Invalid item flag: $it")
                }
            }
        }

        // Apply modifiers
        if (modifiers.isNotEmpty()) {
            val enabledModifiers = modifiers.filter { it.value == true }.keys.toList()
            if (enabledModifiers.isNotEmpty()) {
                setCustomValue(meta, plugin, "modify", enabledModifiers)
            }
        }

        // Kit identification
        setCustomValue(meta, plugin, "kit_name", kitName)
        setCustomValue(meta, plugin, "slot", slot)

        item.itemMeta = meta
        item.amount = amount

        return item
    }

    companion object {
        fun fromConfig(section: ConfigurationSection, kitName: String, slot: Int): KitItem {
            val material = section.getString("item", "STONE")!!
            val name = section.getString("name", "")!!
            val amount = section.getInt("amount", 1)
            val lore = section.getStringList("lore")

            val enchantments = mutableMapOf<String, Int>()
            section.getConfigurationSection("enchants")?.let { enchants ->
                enchants.getKeys(false).forEach { enchantName ->
                    enchantments[enchantName] = enchants.getInt(enchantName)
                }
            }

            val metadata = mutableMapOf<String, Any>()
            section.getConfigurationSection("meta")?.let { metaSection ->
                metaSection.getKeys(false).forEach { key ->
                    metadata[key] = metaSection.get(key)!!
                }
            }

            val modifiers = mutableMapOf<String, Any>()
            section.getConfigurationSection("modifiers")?.let { modifiersSection ->
                modifiersSection.getKeys(false).forEach { key ->
                    modifiers[key] = modifiersSection.get(key)!!
                }
            }

            return KitItem(
                name = name,
                material = material,
                amount = amount,
                lore = lore,
                enchantments = enchantments,
                metadata = metadata,
                modifiers = modifiers,
                kitName = kitName,
                slot = slot
            )
        }

        fun fromItemStack(item: ItemStack, kitName: String, slot: Int): KitItem {
            val material = item.type.name
            val meta = item.itemMeta

            val name = meta?.displayName() ?: ""
            val lore = meta?.lore()?.map { it.toString() } ?: emptyList()

            val enchantments = mutableMapOf<String, Int>()
            meta?.enchants?.forEach { (enchant, level) ->
                enchantments[enchant.key.key] = level
            }

            val metadata = mutableMapOf<String, Any>()
            if (meta?.hasCustomModelData() == true) {
                metadata["model"] = meta.customModelData
            }

            if (meta?.isUnbreakable == true) {
                metadata["unbreakable"] = true
            }

            if (meta is Damageable) {
                metadata["durability"] = meta.damage
            }

            if (meta?.itemFlags?.isNotEmpty() == true) {
                metadata["itemFlags"] = meta.itemFlags.map { it.name }
            }

            return KitItem(
                name = name.toString(),
                material = material,
                amount = item.amount,
                lore = lore,
                enchantments = enchantments,
                metadata = metadata,
                modifiers = emptyMap(), // Modifiers are handled separately
                kitName = kitName,
                slot = slot
            )
        }
    }
}