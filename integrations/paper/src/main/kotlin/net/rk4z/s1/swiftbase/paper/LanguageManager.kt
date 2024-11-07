package net.rk4z.s1.swiftbase.paper

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.rk4z.s1.swiftbase.core.LanguageManager
import net.rk4z.s1.swiftbase.core.MessageKey
import org.bukkit.entity.Player
import java.util.*

/**
 * Retrieves the player's language settings.
 *
 * @return The language code of the player's locale, or "en" if the locale is not set.
 */
fun Player.getLanguage(): String {
    return this.locale().language ?: "en"
}

/**
 * Retrieves a text message based on the player's language settings and the provided message key.
 * If the message is not found for the player's language, the key name itself is returned as the default message.
 *
 * @param player The player whose language settings will be used to determine the message language.
 * @param key The message key used to look up the appropriate message.
 * @param args Optional arguments to format the message if it contains placeholders.
 * @return A [TextComponent] representing the localized message or the key name itself if no localized message is found.
 */
fun LanguageManager.getMessage(player: Player, key: MessageKey, vararg args: Any): TextComponent {
    val lang = player.getLanguage()
    val message = messages[lang]?.get(key)

    val t = message?.let { String.format(it, *args) } ?: return key.c()

    return Component.text(t)
}

/**
 * Retrieves the localized message for the given player, or returns a default message if not found.
 *
 * @param player The player whose language settings will be used to retrieve the message.
 * @param key The message key used to look up the message.
 * @param defaultMessage A fallback message to use if the localized message is not found.
 * @param args Optional arguments to format the message.
 * @return A [TextComponent] containing the localized message or the default message.
 */
fun LanguageManager.getMessageOrDefault(player: Player, key: MessageKey, defaultMessage: String, vararg args: Any): TextComponent {
    val lang = player.getLanguage()
    val message = messages[lang]?.get(key)

    val t = message?.let { String.format(it, *args) } ?: defaultMessage

    return Component.text(t)
}

/**
 * Retrieves the raw message for the given player and message key without formatting or placeholders.
 *
 * @param player The player whose language settings will be used to retrieve the raw message.
 * @param key The message key used to look up the message.
 * @return The raw message string if found, or the key name if not found.
 */
fun LanguageManager.getRawMessage(player: Player, key: MessageKey): String {
    return messages[player.getLanguage()]?.get(key) ?: key.c().content()
}

/**
 * Retrieves a system message based on the server's default locale and the provided message key.
 * If the message is not found for the system's language, the key name itself is returned as the default message.
 *
 * @param key The message key used to look up the appropriate system message.
 * @param args Optional arguments to format the message, if it contains placeholders.
 * @return A [String] representing the localized system message or the key name itself if no localized message is found.
 */
fun LanguageManager.getSysMessage(key: MessageKey, vararg args: Any): String {
    val lang = Locale.getDefault().language
    val message = messages[lang]?.get(key)

    val t = message?.let { String.format(it, *args) } ?: return key.c().content()

    return t
}

/**
 * Checks if a localized message exists for the given message key and player's language.
 *
 * @param player The player whose language settings will be used to check for the message.
 * @param key The message key to check.
 * @return `true` if a message exists for the player's language, otherwise `false`.
 */
fun LanguageManager.hasMessage(player: Player, key: MessageKey): Boolean {
    val lang = player.getLanguage()
    return messages[lang]?.containsKey(key) ?: false
}

/**
 * Retrieves the localized message for the given player based on their language settings.
 * If no message is found for the player's language, the key name itself is returned as a fallback.
 *
 * @param player The player whose language settings will be used to retrieve the localized message.
 * @return A [TextComponent] containing the localized message or the key name if no message is found.
 */
fun MessageKey.t(player: Player): TextComponent {
    return LanguageManager.getMessage(player, this)
}