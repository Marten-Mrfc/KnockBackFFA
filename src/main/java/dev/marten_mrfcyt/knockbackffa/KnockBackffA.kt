package dev.marten_mrfcyt.knockbackffa

import org.bukkit.plugin.java.JavaPlugin

class KnockBackFFA : JavaPlugin() {
    override fun onEnable() {
        logger.info("KnockBackFFA has been enabled!")
        kbffaCommand()
    }

    override fun onDisable() {
        logger.info("KnockBackFFA has been disabled!")
    }
}
