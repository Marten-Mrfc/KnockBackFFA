package dev.marten_mrfcyt.knockbackffa

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.string
import dev.marten_mrfcyt.knockbackffa.arena.createArena
import dev.marten_mrfcyt.knockbackffa.arena.deleteArena
import dev.marten_mrfcyt.knockbackffa.arena.listArena
import dev.marten_mrfcyt.knockbackffa.arena.ArenaHandler
import dev.marten_mrfcyt.knockbackffa.guis.editor.EditKit
import dev.marten_mrfcyt.knockbackffa.guis.editor.EditKitSelector
import dev.marten_mrfcyt.knockbackffa.guis.editor.KitSelector
import dev.marten_mrfcyt.knockbackffa.kits.listKits
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

fun Plugin.kbffaCommand(arenaHandler: ArenaHandler) = command("kbffa") {
    requiresPermissions("kbffa.command")
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
    source.message("Debug command")
    source.message(source.inventory.itemInMainHand.toString())
}

private fun LiteralDSLBuilder.setup(arenaHandler: ArenaHandler) {
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
            source.message("<green>Successfully reloaded translations!")
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
                    source.error("Please insert a killblock for the arena!")
                }
            }
            executes {
                source.error("Please insert a name for the arena!")
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
                source.error("Please insert a name for the arena!")
            }
        }
        executes {
            // List all commands in help command format
            source.sendMini(
                """
                <gold><bold>KnockBackFFA<reset> <gray>- <white>Arena Command
                <white><bold>*</bold> <green>/kbffa arena create<gray>: <white>Create an arena
                <white><bold>*</bold> <green>/kbffa arena list<gray>: <white>List all arenas
                <white><bold>*</bold> <green>/kbffa arena delete<gray>: <white>Delete an arena
            """.trimIndent()
            )
        }
    }
    literal("kit") {
        requiresPermissions("kbffa.kit")
        literal("create") {
            argument("name", string()) { // Fixed line
                argument("lore", string()) {
                    executes {
                        val name = getArgument<String>("name")
                        val lore = getArgument<String>("lore")
                        EditKit(KnockBackFFA()).kitEditor(
                            source as Player,
                            name.asMini(),
                            lore.asMini(),
                            name,
                            true
                        )
                    }
                }
                executes {
                    source.error("Please insert a lore for the kit!")
                }
            }
            executes {
                source.error("Please insert a name for the kit!")
            }
        }
        literal("edit") {
            executes {
                EditKitSelector(KnockBackFFA(), source as Player)
            }
        }
        literal("delete") {
            argument("name", string()) {
                suggests { builder ->
                    listKits(KnockBackFFA.instance).forEach { builder.suggest(it) }
                }
                executes {
                    val name = getArgument<String>("name")
                    if (name == "default") {
                        source.error("You can't delete the default kit!")
                    } else {
                        val config = File("${plugin.dataFolder}/kits.yml")
                        val kitConfig = YamlConfiguration.loadConfiguration(config)
                        if (kitConfig.contains("kit.$name")) {
                            kitConfig.set("kit.$name", null)
                            kitConfig.save(config)
                            source.message("Successfully deleted kit $name")
                        } else {
                            source.error("Kit $name does not exist!")
                        }
                    }
                }
            }
            executes {
                source.error("Please insert a name for the kit!")
            }
        }
        executes {
            EditKitSelector(KnockBackFFA(), source as Player)
        }
    }
}

fun Plugin.kitSelectorCommand() = command("kit") {
    executes {
        KitSelector(KnockBackFFA(), source as Player)
    }
}