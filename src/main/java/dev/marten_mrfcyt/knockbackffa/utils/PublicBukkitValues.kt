package dev.marten_mrfcyt.knockbackffa.utils

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import org.bukkit.NamespacedKey
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

// Enter value into `https://www.rapidtables.com/convert/number/ascii-to-hex.html` and use result as id.
// or use id:
// - is_draggable: true/false
fun setCustomValue(meta: ItemMeta, plugin: KnockBackFFA, id: String, value: Any) {
    val key = NamespacedKey(plugin, id)
    when (value) {
        is String -> meta.persistentDataContainer.set(key, PersistentDataType.STRING, value)
        is Boolean -> meta.persistentDataContainer.set(key, PersistentDataType.BOOLEAN, value)
    }
}

fun checkCustomValue(meta: ItemMeta, plugin: KnockBackFFA, id: String, value: Any): Boolean {
    val key = NamespacedKey(plugin, id)
    return when (value) {
        is String -> meta.persistentDataContainer.getOrDefault(key, PersistentDataType.STRING, "") == value
        is Boolean -> meta.persistentDataContainer.getOrDefault(key, PersistentDataType.BOOLEAN, true) == value
        else -> false
    }
}
fun getCustomValue(meta: ItemMeta, plugin: KnockBackFFA, id: String): Any? {
    val key = NamespacedKey(plugin, id)
    return when {
        meta.persistentDataContainer.has(key, PersistentDataType.STRING) -> meta.persistentDataContainer.get(key, PersistentDataType.STRING)
        meta.persistentDataContainer.has(key, PersistentDataType.BOOLEAN) -> meta.persistentDataContainer.get(key, PersistentDataType.BOOLEAN)
        else -> null
    }
}
