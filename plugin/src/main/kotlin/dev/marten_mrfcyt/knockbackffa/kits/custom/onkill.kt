package dev.marten_mrfcyt.knockbackffa.kits.custom
// on kill

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import dev.marten_mrfcyt.knockbackffa.utils.checkCustomValue
import dev.marten_mrfcyt.knockbackffa.utils.getCustomValue
import lirand.api.extensions.inventory.set
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import java.io.File

class OnKill(private val plugin: KnockBackFFA) : Listener {
    @EventHandler
    fun onKill(event: PlayerDeathEvent) {
        val source = event.entity.killer ?: return
        println("Checking items for player: ${source.name}")
        for (item in source.inventory.contents) {
            println("Checking item: $item")
            if (item == null) continue
            val itemMeta = item.itemMeta ?: continue
            if (checkCustomValue(itemMeta, KnockBackFFA.instance, "modify", listOf("onKill"))) {
                println("Item is onKill item")
                val slot = (getCustomValue(itemMeta, KnockBackFFA.instance, "slot") as? Int) ?: 0
                val kitName = (getCustomValue(itemMeta, KnockBackFFA.instance, "kit_name") as? String) ?: continue
                val config = File("${KnockBackFFA.instance.dataFolder}/kits.yml")
                val kitConfig = YamlConfiguration.loadConfiguration(config)
                val amount = kitConfig.get("kit.$kitName.items.$slot.amount") as Int
                item.amount = amount
                source.inventory[slot] = item
            }
        }
    }
}


