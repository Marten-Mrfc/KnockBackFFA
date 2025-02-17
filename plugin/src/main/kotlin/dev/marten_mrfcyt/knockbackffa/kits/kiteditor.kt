package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.guis.editor.ItemModifierGUI
import dev.marten_mrfcyt.knockbackffa.guis.editor.KitSelector
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class KitEditor(private val plugin: KnockBackFFA) {
    fun openKitCreationGui(source: CommandSender) {
        KitSelector(plugin, source as Player)
    }
}