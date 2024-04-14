package dev.marten_mrfcyt.knockbackffa

import com.mojang.brigadier.arguments.StringArgumentType.string
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.marten_mrfcyt.knockbackffa.arena.ArenaHandler
import dev.marten_mrfcyt.knockbackffa.arena.createArena
import dev.marten_mrfcyt.knockbackffa.arena.deleteArena
import dev.marten_mrfcyt.knockbackffa.arena.listArena
import dev.marten_mrfcyt.knockbackffa.kits.KitEditor
import dev.marten_mrfcyt.knockbackffa.kits.guis.editor.KitModifier
import dev.marten_mrfcyt.knockbackffa.utils.asMini
import dev.marten_mrfcyt.knockbackffa.utils.error
import dev.marten_mrfcyt.knockbackffa.utils.sendMini
import lirand.api.dsl.command.builders.LiteralDSLBuilder
import lirand.api.dsl.command.builders.command
import org.bukkit.plugin.Plugin
import java.util.concurrent.CompletableFuture

fun Plugin.kbffaCommand(arenaHandler: ArenaHandler) = command("kbffa") {
    setup(arenaHandler)
}

private fun LiteralDSLBuilder.setup(arenaHandler: ArenaHandler) {
    literal("arena") {
        literal("create") {
            argument("name", string()) {
                executes {
                    plugin.createArena(source, getArgument("name"))
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
                    getArenaNamesSuggestions(builder, arenaHandler)
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
        literal("create") {
            argument("name", string()) { // Fixed line
                argument("lore", string()) {
                    executes {
                        val name = getArgument<String>("name")
                        val lore = getArgument<String>("lore")
                        KitModifier(KnockBackFFA()).openNewKitGUI(
                            source,
                            name.asMini(),
                            lore.asMini()
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
        executes {
            KitEditor(KnockBackFFA()).openKitCreationGui(source)
        }
    }
}

fun getArenaNamesSuggestions(builder: SuggestionsBuilder, arenaHandler: ArenaHandler): CompletableFuture<Suggestions> {
    return arenaHandler.getArenaNames().thenApply { names ->
        names.forEach {
            builder.suggest(it)
        }
        builder.build()
    }
}