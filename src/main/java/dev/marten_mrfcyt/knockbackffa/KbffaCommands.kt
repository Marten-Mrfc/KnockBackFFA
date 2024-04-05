package dev.marten_mrfcyt.knockbackffa

import dev.marten_mrfcyt.knockbackffa.utils.error
import dev.marten_mrfcyt.knockbackffa.utils.sendMini
import org.bukkit.plugin.Plugin
import lirand.api.dsl.command.builders.LiteralDSLBuilder
import lirand.api.dsl.command.builders.command
import dev.marten_mrfcyt.knockbackffa.arena.createArena
import dev.marten_mrfcyt.knockbackffa.utils.message
import com.mojang.brigadier.arguments.StringArgumentType.string
fun Plugin.kbffaCommand() = command("kbffa") {
    setup()
}

private fun LiteralDSLBuilder.setup() {
    literal("arena") {
        literal("create") {
            argument("name", string()) {
                executes {
                    source.message("Arena create command")
                    plugin.createArena(source, name)
                }
            }
            executes {
                source.error("Please insert a name for the arena!")
            }
        }
        literal("list") {
            executes {
                source.message("Arena list command")
            }
        }
        literal("delete") {
            executes {
                source.message("Arena delete command")
            }
        }
        executes {
            // List all commands in help command format
            source.sendMini("""
                <gold><bold>KnockBackFFA<reset> <gray>- <white>Arena Command
                <white><bold>*</bold> <green>/kbffa arena create<gray>: <white>Create an arena
                <white><bold>*</bold> <green>/kbffa arena list<gray>: <white>List all arenas
                <white><bold>*</bold> <green>/kbffa arena delete<gray>: <white>Delete an arena
            """.trimIndent())
        }
    }
}