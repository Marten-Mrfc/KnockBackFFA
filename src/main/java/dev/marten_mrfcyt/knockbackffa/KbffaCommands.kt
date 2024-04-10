package dev.marten_mrfcyt.knockbackffa

import dev.marten_mrfcyt.knockbackffa.utils.error
import dev.marten_mrfcyt.knockbackffa.utils.sendMini
import org.bukkit.plugin.Plugin
import lirand.api.dsl.command.builders.LiteralDSLBuilder
import lirand.api.dsl.command.builders.command
import com.mojang.brigadier.arguments.StringArgumentType.string
import com.mojang.brigadier.suggestion.Suggestions
import dev.marten_mrfcyt.knockbackffa.arena.ArenaHandler
import dev.marten_mrfcyt.knockbackffa.arena.createArena
import dev.marten_mrfcyt.knockbackffa.arena.deleteArena
import dev.marten_mrfcyt.knockbackffa.arena.listArena
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import lirand.api.dsl.command.builders.BrigadierCommandContext
import org.bukkit.command.CommandSender
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
fun getArenaNamesSuggestions(builder: SuggestionsBuilder, arenaHandler: ArenaHandler): CompletableFuture<Suggestions> {
    return arenaHandler.getArenaNames().thenApply { names ->
        names.forEach {
            builder.suggest(it)
        }
        builder.build()
    }
}