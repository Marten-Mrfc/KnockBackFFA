package dev.marten_mrfcyt.knockbackffa.utils

import org.bukkit.Bukkit

private val serverVersion: String = Bukkit.getBukkitVersion().split("-")[0]
// Helper method to compare server versions
fun isBelowVersion(version: String): Boolean {
    val serverParts = serverVersion.split(".").map { it.toInt() }
    val compareParts = version.split(".").map { it.toInt() }

    for (i in compareParts.indices) {
        if (i >= serverParts.size || serverParts[i] < compareParts[i]) {
            return true
        } else if (serverParts[i] > compareParts[i]) {
            return false
        }
    }
    return false
}