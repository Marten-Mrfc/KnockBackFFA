package dev.marten_mrfcyt.knockbackffa.utils

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import org.bukkit.NamespacedKey
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

/**
 * Sets a custom value in the item's persistent data container.
 *
 * @param meta The ItemMeta of the item.
 * @param plugin The instance of the plugin.
 * @param id The key to store the value under.
 * @param value The value to store, can be String, Boolean, Int, or List<String>.
 */
fun setCustomValue(meta: ItemMeta, plugin: KnockBackFFA, id: String, value: Any) {
    val key = NamespacedKey(plugin, id)
    when (value) {
        is String -> meta.persistentDataContainer.set(key, PersistentDataType.STRING, value)
        is Boolean -> meta.persistentDataContainer.set(key, PersistentDataType.BOOLEAN, value)
        is Int -> meta.persistentDataContainer.set(key, PersistentDataType.INTEGER, value)
        is List<*> -> {
            val stringList = value.filterIsInstance<String>()
            val combinedString = stringList.joinToString(",")
            meta.persistentDataContainer.set(key, PersistentDataType.STRING, combinedString)
        }
    }
}

/**
 * Checks if a custom value in the item's persistent data container matches the given value.
 *
 * @param meta The ItemMeta of the item.
 * @param plugin The instance of the plugin.
 * @param id The key to check the value under.
 * @param value The value to check against, can be String, Boolean, Int, or List<String>.
 * @return True if the stored value matches the given value, false otherwise.
 */
fun checkCustomValue(meta: ItemMeta, plugin: KnockBackFFA, id: String, value: Any): Boolean {
    val key = NamespacedKey(plugin, id)
    return when (value) {
        is String -> {
            val storedValue = meta.persistentDataContainer.get(key, PersistentDataType.STRING)
            val storedList = storedValue?.split(",") ?: emptyList()
            storedList.contains(value)
        }
        is Boolean -> meta.persistentDataContainer.get(key, PersistentDataType.BOOLEAN) == value
        is Int -> meta.persistentDataContainer.get(key, PersistentDataType.INTEGER) == value
        is List<*> -> {
            val storedValue = meta.persistentDataContainer.get(key, PersistentDataType.STRING)
            val storedList = storedValue?.split(",") ?: emptyList()
            storedList.containsAll(value)
        }
        else -> false
    }
}

/**
 * Retrieves a custom value from the item's persistent data container.
 *
 * @param meta The ItemMeta of the item.
 * @param plugin The instance of the plugin.
 * @param id The key to retrieve the value from.
 * @return The stored value, can be String, Boolean, Int, List<String>, or null if not found.
 */
fun getCustomValue(meta: ItemMeta, plugin: KnockBackFFA, id: String): Any? {
    val key = NamespacedKey(plugin, id)
    return when {
        meta.persistentDataContainer.has(key, PersistentDataType.STRING) -> {
            val value = meta.persistentDataContainer.get(key, PersistentDataType.STRING)
            if (value?.contains(",") == true) {
                value.split(",")
            } else {
                value ?: ""
            }
        }
        meta.persistentDataContainer.has(key, PersistentDataType.BOOLEAN) -> meta.persistentDataContainer.get(key, PersistentDataType.BOOLEAN) ?: false
        meta.persistentDataContainer.has(key, PersistentDataType.INTEGER) -> meta.persistentDataContainer.get(key, PersistentDataType.INTEGER) ?: 0
        else -> null
    }
}