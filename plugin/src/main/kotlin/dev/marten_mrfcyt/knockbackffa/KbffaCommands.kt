package dev.marten_mrfcyt.knockbackffa

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.string
import dev.marten_mrfcyt.knockbackffa.arena.createArena
import dev.marten_mrfcyt.knockbackffa.arena.deleteArena
import dev.marten_mrfcyt.knockbackffa.arena.listArena
import dev.marten_mrfcyt.knockbackffa.arena.ArenaHandler
import dev.marten_mrfcyt.knockbackffa.guis.editor.boosts.BoostSelector
import dev.marten_mrfcyt.knockbackffa.guis.editor.kit.EditKit
import dev.marten_mrfcyt.knockbackffa.guis.editor.kit.EditKitSelector
import dev.marten_mrfcyt.knockbackffa.guis.editor.kit.KitSelector
import dev.marten_mrfcyt.knockbackffa.guis.shop.ShopCategorySelector
import dev.marten_mrfcyt.knockbackffa.guis.shop.boosts.ActiveBoostsGUI
import dev.marten_mrfcyt.knockbackffa.utils.TranslationManager
import mlib.api.commands.builders.LiteralDSLBuilder
import mlib.api.commands.builders.command
import mlib.api.utilities.*
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File

private val blockSuggestions: List<String> by lazy {
    Registry.MATERIAL.stream()
        .filter { it.isBlock }
        .map { it.name }
        .toList()
}
internal val bypassMode = mutableMapOf<Player, Boolean>()

fun Plugin.kbffaCommand(arenaHandler: ArenaHandler) = command("kbffa") {
    requiresPermissions("kbffa.command")
    alias("knockbackffa", "knbffa")
    setup(arenaHandler)
    executes {
        source.message("<gray>---<reset> <gold>KnockBackFFA<reset> <gray>---")
        @Suppress("UnstableApiUsage")
        source.message("<white>Version: <gold>${pluginMeta.version}")
        source.message("<white>Author: <gold>Marten_mrfcyt")
        source.message("<gray>---<reset> <gold>Commands<reset> <gray>---")
        source.sendMessage("<green>* <white>/kbffa arena <green>create/list/delete<gray>: <gold>Arena commands".asMini())
        source.sendMessage("<green>* <white>/kbffa kit <green>create/edit/delete<gray>: <gold>Kit commands".asMini())
        source.sendMessage("<green>* <white>/kit<gray>: <gold>Select a kit as a player".asMini())
        source.sendMessage("<green>* <white>/kbffa debug<gray>: <gold>Debug command".asMini())
        source.message("<gray>-------------------")
    }
}

fun debug(source: Player) {
    source.message(TranslationManager.translate("commands.debug.title"))
    source.message(source.inventory.itemInMainHand.toString())
}

private fun LiteralDSLBuilder.setup(arenaHandler: ArenaHandler) {
    literal("bypass") {
        requiresPermissions("kbffa.bypass")
        executes {
            val player = source as Player
            val isBypassing = bypassMode.getOrDefault(player, false)
            bypassMode[player] = !isBypassing
            source.message(TranslationManager.translate("commands.bypass.toggle", "status" to if (!isBypassing) "enabled" else "disabled"))
        }
    }
    literal("debug") {
        requiresPermissions("kbffa.debug")
        executes {
            debug(source as Player)
        }
    }
    literal("reload") {
        requiresPermissions("kbffa.reload")
        executes {
            TranslationManager.reload(plugin)

            KnockBackFFA.kitManager.reloadKits()
            KnockBackFFA.instance.boostManager.reloadBoosts()
            KnockBackFFA.instance.modifierManager.reloadModifiers()

            source.message(TranslationManager.translate("commands.reload.success"))
        }
    }
    literal("arena") {
        requiresPermissions("kbffa.arena")
        literal("create") {
            argument("name", string()) {
                argument("killBlock", StringArgumentType.greedyString()) {
                    suggests { builder ->
                        blockSuggestions.forEach { builder.suggest(it) }
                        builder.build()
                    }
                    executes {
                        plugin.createArena(source, getArgument("name"), Material.valueOf(getArgument("killBlock")))
                    }
                }
                executes {
                    source.error(TranslationManager.translate("commands.arena.create.missing_killblock"))
                }
            }
            executes {
                source.error(TranslationManager.translate("commands.arena.create.missing_name"))
            }
        }
        literal("list") {
            executes {
                plugin.listArena(source)
            }
        }
        literal("delete") {
            argument("name", string()) {
                suggests { builder ->
                    arenaHandler.getArenaNames().forEach { builder.suggest(it) }
                }
                executes {
                    plugin.deleteArena(source, getArgument("name"))
                }
            }
            executes {
                source.error(TranslationManager.translate("commands.arena.delete.missing_name"))
            }
        }
        executes {
            source.sendMini(TranslationManager.translate("commands.arena.help"))
        }
    }
    literal("kit") {
        requiresPermissions("kbffa.kit")
        literal("create") {
            argument("name", string()) {
                argument("lore", string()) {
                    executes {
                        val name = getArgument<String>("name")
                        val lore = getArgument<String>("lore")
                        EditKit(KnockBackFFA.instance).kitEditor(
                            source as Player,
                            name.asMini(),
                            lore.asMini(),
                            name,
                            true
                        )
                    }
                }
                executes {
                    source.error(TranslationManager.translate("commands.kit.create.missing_lore"))
                }
            }
            executes {
                source.error(TranslationManager.translate("commands.kit.create.missing_name"))
            }
        }
        literal("edit") {
            executes {
                EditKitSelector(KnockBackFFA.instance, source as Player)
            }
        }
        literal("delete") {
            argument("name", string()) {
                suggests { builder ->
                    KnockBackFFA.kitManager.getAllKitNames().forEach { builder.suggest(it) }
                }
                executes {
                    val name = getArgument<String>("name")
                    if (name == "default") {
                        source.error(TranslationManager.translate("commands.kit.delete.default_kit"))
                    } else {
                        if(KnockBackFFA.kitManager.deleteKit(name)) {
                            source.sendMessage(TranslationManager.translate("commands.kit.delete.success", "name" to name))
                        } else {
                            source.error(TranslationManager.translate("commands.kit.delete.not_found", "name" to name))
                        }
                    }
                }
            }
            executes {
                source.error(TranslationManager.translate("commands.kit.delete.missing_name"))
            }
        }
        executes {
            EditKitSelector(KnockBackFFA.instance, source as Player)
        }
    }
    literal("boosts") {
        requiresPermissions("kbffa.boosts")
        executes {
            BoostSelector(KnockBackFFA.instance, source as Player)
        }
    }
}

fun Plugin.kitSelectorCommand() = command("kit") {
    executes {
        KitSelector(KnockBackFFA.instance, source as Player)
    }
}

fun Plugin.shopCommand() = command("shop") {
    executes {
        ShopCategorySelector(KnockBackFFA.instance, source as Player)
    }
}

fun Plugin.boostsCommand() = command("boosts") {
    requiresPermissions("kbffa.boosts")
    executes {
        ActiveBoostsGUI(KnockBackFFA.instance, source as Player)
    }
}