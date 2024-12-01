package dev.marten_mrfcyt.knockbackffa.kits.custom
// on kill

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.checkCustomValue
import dev.marten_mrfcyt.knockbackffa.utils.getCustomValue
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import java.io.File

class OnKillExecute : Listener {
    @EventHandler
    fun onKillExecute(event: PlayerDeathEvent) {
        val source = event.entity.killer ?: return
        for (item in source.inventory.contents) {
            if (item == null) continue
            val itemMeta = item.itemMeta ?: continue
            if (checkCustomValue(itemMeta, KnockBackFFA.instance, "modify", listOf("onKillExecute"))) {
                val config = File("${KnockBackFFA.instance.dataFolder}/kits.yml")
                val kitConfig = YamlConfiguration.loadConfiguration(config)
                val slot = (getCustomValue(item.itemMeta, KnockBackFFA.instance, "slot") as? Int) ?: 0
                val kitName = (getCustomValue(item.itemMeta, KnockBackFFA.instance, "kit_name") as? String) ?: return
                val command = kitConfig.get("kit.$kitName.items.$slot.modifiers.command") as String
                PlaceholderAPI.setPlaceholders(source, command)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            }
        }
    }
}


