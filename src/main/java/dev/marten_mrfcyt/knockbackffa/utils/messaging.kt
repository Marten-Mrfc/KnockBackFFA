package dev.marten_mrfcyt.knockbackffa.utils
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender

private val mm = MiniMessage.builder().build()
fun String.asMini() = mm.deserialize(this)
fun CommandSender.sendMini(message: String) = sendMessage(message.asMini())

fun CommandSender.error(message: String) = sendMessage("<red><bold>Error</bold><gray> | <white> $message".asMini())
fun CommandSender.message(message: String) = sendMessage("<gold><bold>KnockBackFFA</bold><gray> | <white> $message".asMini())

fun message(message: String) = "<gold><bold>KnockBackFFA</bold><gray> | <white> $message".asMini()