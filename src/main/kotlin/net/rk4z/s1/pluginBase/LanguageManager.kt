package net.rk4z.s1.pluginBase

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.rk4z.s1.pluginBase.PluginEntry.Companion.logger
import org.bukkit.entity.Player
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Suppress("unused", "UNCHECKED_CAST", "MemberVisibilityCanBePrivate")
object LanguageManager {
    val messages: MutableMap<String, MutableMap<MessageKey, String>> = mutableMapOf()

    /**
     * Finds and returns a list of missing message keys for the specified language. This method
     * compares the keys in the system with the ones defined in the YAML file and logs a warning
     * for each missing key.
     *
     * It scans for all `MessageKey` implementations in the specified package, retrieves the
     * corresponding messages for the specified language, and checks if any keys are missing.
     *
     * @param lang The language for which to check the message keys (for example, "en", "fr", etc).
     *
     * @return A list of missing message keys. If no keys are missing, an empty list is returned.
     *
     * **Note:** This method logs a warning for each missing key it detects.
     */
    fun findMissingKeys(lang: String): List<String> {
        val messageKeyMap: MutableMap<String, MessageKey> = mutableMapOf()

        scanForMessageKeys(messageKeyMap)

        val currentMessages = messages[lang] ?: return emptyList()

        val missingKeys = mutableListOf<String>()

        messageKeyMap.forEach { (path, key) ->
            if (!currentMessages.containsKey(key)) {
                missingKeys.add(path)
                logger.warn("Missing key: $path for language: $lang")
            }
        }

        return missingKeys
    }

    internal fun processYamlAndMapMessageKeys(data: Map<String, Any>, messageMap: MutableMap<MessageKey, String>) {
        val messageKeyMap: MutableMap<String, MessageKey> = mutableMapOf()

        scanForMessageKeys(messageKeyMap)

        processYamlData("", data, messageKeyMap, messageMap)
    }

    private fun scanForMessageKeys(messageKeyMap: MutableMap<String, MessageKey>) {
        val reflections = Reflections(
            ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(PluginEntry.get().packageName))
                .setScanners(Scanners.SubTypes)
        )

        val messageKeyClasses = reflections.getSubTypesOf(MessageKey::class.java)

        messageKeyClasses.forEach { clazz ->
            mapMessageKeys(clazz.kotlin, "", messageKeyMap)
        }
    }

    private fun mapMessageKeys(clazz: KClass<out MessageKey>, currentPath: String = "", messageKeyMap: MutableMap<String, MessageKey>) {
        val className = clazz.simpleName?.lowercase() ?: return

        val fullPath = if (currentPath.isEmpty()) className else "$currentPath.$className"

        val objectInstance = clazz.objectInstance
        if (objectInstance != null) {
            messageKeyMap[fullPath] = objectInstance
            if (PluginEntry.get().isDebug) {
                logger.info("Mapped class: $fullPath -> ${clazz.simpleName}")
            }
        }

        clazz.nestedClasses.forEach { nestedClass ->
            if (nestedClass.isSubclassOf(MessageKey::class)) {
                mapMessageKeys(nestedClass as KClass<out MessageKey>, fullPath, messageKeyMap)
            }
        }
    }

    private fun processYamlData(prefix: String, data: Map<String, Any>, messageKeyMap: Map<String, MessageKey>, messageMap: MutableMap<MessageKey, String>) {
        for ((key, value) in data) {
            val currentPrefix = if (prefix.isEmpty()) key else "$prefix.$key"
            if (PluginEntry.get().isDebug) {
                logger.info("Processing YAML path: $currentPrefix")
            }

            when (value) {
                is String -> {
                    val messageKey = messageKeyMap[currentPrefix]
                    if (messageKey != null) {
                        messageMap[messageKey] = value
                        if (PluginEntry.get().isDebug) {
                            logger.info("Mapped: $messageKey -> $value")
                        }
                    } else {
                        if (PluginEntry.get().isDebug) {
                            logger.warn("MessageKey not found for path: $currentPrefix")
                        }
                    }
                }
                is Map<*, *> -> {
                    processYamlData(currentPrefix, value as Map<String, Any>, messageKeyMap, messageMap)
                }
            }
        }
    }

    private fun Player.getLanguage(): String {
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
    fun getMessage(player: Player, key: MessageKey, vararg args: Any): TextComponent {
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
    fun getMessageOrDefault(player: Player, key: MessageKey, defaultMessage: String, vararg args: Any): TextComponent {
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
    fun getRawMessage(player: Player, key: MessageKey): String {
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
    fun getSysMessage(key: MessageKey, vararg args: Any): String {
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
    fun hasMessage(player: Player, key: MessageKey): Boolean {
        val lang = player.getLanguage()
        return messages[lang]?.containsKey(key) ?: false
    }

}

/**
 * A sealed interface representing a message key, used to generate structured messages
 * and handle localization within the plugin. This interface allows message keys to be
 * nested, following a structured hierarchy that corresponds to YAML structure.
 *
 * Implementing classes can define their own message keys as nested objects.
 * The `c()` method provides a default fallback, returning the key's simple name,
 * and the `t()` method retrieves the localized message for a given player.
 *
 * Example usage:
 *
 * ```kotlin
 * open class Main : MessageKey {
 *     open class Gui : Main() {
 *         open class Title : Gui() {
 *             object MAIN_BOARD : Title()
 *             [...]
 *         }
 *     }
 * }
 * ```
 *
 * By structuring message keys this way, the key paths are automatically recognized,
 * and corresponding YAML files can be created to match the hierarchy. For example,
 * `Main.Gui.Title.MAIN_BOARD` will map to a key in the YAML file following that structure.
 *
 * **Note:** YAML keys should all be written in lowercase, as the system automatically
 * converts class names to lowercase for key matching. If the keys in the YAML file are
 * not lowercase, they will not be recognized correctly.
 */
interface MessageKey {
    /**
     * Returns the default message or key name as a [TextComponent], using the simple name of the class.
     * This is used as a fallback when no localized message is found.
     *
     * @return A [TextComponent] containing the simple name of the message key class.
     */
    fun c(): TextComponent {
        return Component.text(this.javaClass.simpleName)
    }

    /**
     * Retrieves the raw string representation of the message key, without any formatting.
     * This can be useful when working with systems that do not support TextComponent.
     *
     * @return A [String] representing the raw message key or the key's simple name if no message is found.
     */
    fun rc(): String {
        return this.javaClass.simpleName
    }

    /**
     * Logs the message key as a message to the console.
     *
     * @param level The logging level (INFO, WARN, ERROR, etc.).
     */
    fun log(level: String = "INFO") {
        val message = rc()
        when (level.uppercase()) {
            "INFO" -> logger.info(message)
            "WARN" -> logger.warn(message)
            "ERROR" -> logger.error(message)
            else -> logger.debug(message)
        }
    }

    /**
     * Retrieves the localized message for the given player based on their language settings.
     * If no message is found for the player's language, the key name itself is returned as a fallback.
     *
     * @param player The player whose language settings will be used to retrieve the localized message.
     * @return A [TextComponent] containing the localized message or the key name if no message is found.
     */
    fun t(player: Player): TextComponent {
        return LanguageManager.getMessage(player, this)
    }
}
