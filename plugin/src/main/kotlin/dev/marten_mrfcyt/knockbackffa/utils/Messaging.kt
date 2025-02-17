package dev.marten_mrfcyt.knockbackffa.utils

import dev.marten_mrfcyt.knockbackffa.KnockBackFFA
import me.clip.placeholderapi.PlaceholderAPI
import mlib.api.utilities.asMini
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import kotlin.random.Random

private val messagesConfig = getMessagesConfig()

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