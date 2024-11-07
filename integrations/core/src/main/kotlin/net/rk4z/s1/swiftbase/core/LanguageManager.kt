package net.rk4z.s1.swiftbase.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.rk4z.s1.swiftbase.core.Core.Companion.logger
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate", "unused")
object LanguageManager {
    val messages: MutableMap<String, MutableMap<MessageKey, String>> = mutableMapOf()

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
                .setUrls(ClasspathHelper.forPackage(Core.get().packageName))
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
            if (Core.get().isDebug) {
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
            if (Core.get().isDebug) {
                logger.info("Processing YAML path: $currentPrefix")
            }

            when (value) {
                is String -> {
                    val messageKey = messageKeyMap[currentPrefix]
                    if (messageKey != null) {
                        messageMap[messageKey] = value
                        if (Core.get().isDebug) {
                            logger.info("Mapped: $messageKey -> $value")
                        }
                    } else {
                        if (Core.get().isDebug) {
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
}
