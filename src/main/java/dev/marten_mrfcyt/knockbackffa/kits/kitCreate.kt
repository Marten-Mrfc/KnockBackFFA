package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.asMini
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

// Kit creation via gui!
class KitCreate(private val plugin: KnockBackFFA) {
    fun openKitCreationGUI(source: CommandSender, name: String) {
        if (source is Player) {
            val inventory = Bukkit.createInventory(null, 18, "<gray>Creating kit:</gray><white> $name</white>".asMini())
            source.openInventory(inventory)
        } else {
            source.sendMessage("You must be a player to use this command!")
        }
    }
}