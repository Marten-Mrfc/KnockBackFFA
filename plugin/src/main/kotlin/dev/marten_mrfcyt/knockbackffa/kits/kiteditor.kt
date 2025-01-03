package dev.marten_mrfcyt.knockbackffa.kits

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.guis.editor.KitSelector
import org.bukkit.command.CommandSender
class KitEditor(private val plugin: KnockBackFFA) {
    fun openKitCreationGui(source: CommandSender) {
        KitSelector(plugin).editKitSelector(source)
    }
}