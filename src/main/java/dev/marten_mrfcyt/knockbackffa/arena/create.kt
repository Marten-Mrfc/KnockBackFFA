package dev.marten_mrfcyt.knockbackffa.arena

import dev.marten_mrfcyt.knockbackffa.utils.error
import dev.marten_mrfcyt.knockbackffa.utils.sendMini
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

fun Plugin.createArena(source: CommandSender, name: String) {
    source.sendMini("Arena CREATED")
    if (source is Player) {
        val location = source.location
        source.sendMini("location: $location")
    }
    else {
        source.error("You must be a player to create an arena!")
    }
}
