package dev.marten_mrfcyt.knockbackffa.utils


import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import kotlin.random.Random
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

private val mm = MiniMessage.builder().build()
private val messagesConfig = getMessagesConfig()

fun String.asMini(player: Player? = null): Component {
    val formattedMessage = if (player != null) PlaceholderAPI.setPlaceholders(player, this) else this
    return mm.deserialize(formattedMessage)
}

fun Component.notMiniText(): String {
    return mm.serialize(this).replace("\\<", "<")
}

fun Component.notMini(): String {
    return mm.serialize(this)
}
fun String.toLegacyMini(player: Player? = null): String {
    // Parse the string with MiniMessage
    val component = this.asMini(player)

    // Convert the component to legacy text (with § color codes)
    return LegacyComponentSerializer.legacySection().serialize(component)
}

// Optional: You can also add an extension for Component to get the legacy version
fun Component.toLegacy(): String {
    return LegacyComponentSerializer.legacySection().serialize(this)
}

fun CommandSender.sendMini(message: String, player: Player? = null) {
    val formattedMessage = PlaceholderAPI.setPlaceholders(player, message)
    sendMessage(formattedMessage.asMini())
}

fun CommandSender.error(message: String, player: Player? = null) {
    val formattedMessage = PlaceholderAPI.setPlaceholders(player, "<red><bold>Error</bold><gray> | <white> $message")
    sendMessage(formattedMessage.asMini())
}

fun CommandSender.message(message: String, player: Player? = null) {
    val formattedMessage =
        PlaceholderAPI.setPlaceholders(player, "<gold><bold>KnockBackFFA</bold><gray> | <white> $message")
    sendMessage(formattedMessage.asMini())
}

fun message(message: String, player: Player? = null): Component {
    val formattedMessage =
        PlaceholderAPI.setPlaceholders(player, "<gold><bold>KnockBackFFA</bold><gray> | <white> $message")
    return formattedMessage.asMini()
}

fun cmessage(messageKey: String, player: Player? = null, vararg args: String) =
    "<gold><bold>KnockBackFFA</bold><gray> | <white> ${getMessage(messageKey, player, *args)}".asMini()

fun getMessage(messageKey: String, player: Player? = null, vararg args: String): String {
    return when (val message = messagesConfig.get(messageKey)) {
        is String -> {
            var formattedMessage = PlaceholderAPI.setPlaceholders(player, message)
            formattedMessage = replaceArgs(formattedMessage, args)
            formattedMessage
        }

        is List<*> -> {
            if (message.isNotEmpty()) {
                val randomIndex = Random.nextInt(message.size)
                var formattedMessage = PlaceholderAPI.setPlaceholders(player, message[randomIndex].toString())
                formattedMessage = replaceArgs(formattedMessage, args)
                formattedMessage
            } else {
                "Empty list for message key '$messageKey'"
            }
        }

        else -> {
            "Message key '$messageKey' not found"
        }
    }
}

fun replaceArgs(message: String, args: Array<out String>): String {
    val regex = "%(.*?)%".toRegex()
    var result = message
    var counter = 0
    regex.findAll(message).forEach { _ ->
        if (counter < args.size) {
            result = result.replaceFirst(regex, args[counter])
            counter++
        }
    }
    return result
}

fun getMessagesConfig(): YamlConfiguration {
    val messagesFile = File(KnockBackFFA.instance.dataFolder, "messages.yml")
    if (!messagesFile.exists()) {
        KnockBackFFA.instance.saveResource("messages.yml", false)
    }
    return YamlConfiguration.loadConfiguration(messagesFile)
}