package net.rk4z.s1.swiftbase.core

import net.rk4z.s1.swiftbase.core.Core.Companion.logger
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Manages localization and message keys for the plugin, supporting multiple languages
 * and dynamically loading message keys from YAML files. This class allows for efficient
 * access to localized messages based on player preferences and system defaults.
 *
 * @param P The player type that implements [IPlayer].
 * @param T The text component type used to display messages.
 * @property textComponentFactory A factory function to create text components of type [T].
 */
@Suppress("UNCHECKED_CAST", "unused")
class LanguageManager<P : IPlayer, T> private constructor(
    internal val textComponentFactory: (String) -> T
) {
    companion object {
        private lateinit var instance: LanguageManager<*, *>

        /**
         * Initializes the [LanguageManager] with the specified text component factory.
         * This method must be called before accessing the [LanguageManager] instance.
         *
         * @param textComponentFactory A factory function for creating text components.
         * @throws IllegalStateException If the [LanguageManager] is already initialized.
         */
        internal fun <P : IPlayer, T> initialize(textComponentFactory: (String) -> T): LanguageManager<P, T> {
            if (::instance.isInitialized) {
                throw IllegalStateException("LanguageManager is already initialized.")
            }
            instance = LanguageManager<P, T>(textComponentFactory)
            return instance as LanguageManager<P, T>
        }

        /**
         * Retrieves the current instance of [LanguageManager].
         *
         * @return The initialized [LanguageManager] instance.
         * @throws IllegalStateException If the [LanguageManager] is not initialized.
         */
        internal fun <P : IPlayer, T> get(): LanguageManager<P, T> {
            if (!::instance.isInitialized) {
                throw IllegalStateException("LanguageManager is not initialized.")
            }
            return instance as LanguageManager<P, T>
        }
    }

    val messages: MutableMap<String, MutableMap<out MessageKey<*, *>, String>> = mutableMapOf()

    /**
     * Finds any missing message keys for a given language.
     *
     * @param lang The language code to check for missing keys.
     * @param expectedType The expected type of the message key.
     * @return A list of paths for keys that are missing translations in the specified language.
     */
    fun findMissingKeys(lang: String, expectedType: KClass<out MessageKey<P, T>>): List<String> {
        val messageKeyMap: MutableMap<String, MessageKey<P, T>> = mutableMapOf()
        scanForMessageKeys(messageKeyMap, expectedType)
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

    /**
     * Processes YAML data and maps message keys to their respective messages.
     *
     * @param data The YAML data to process.
     * @param messageMap A mutable map to store the message keys and their corresponding messages.
     * @param expectedType The expected type of the message key.
     */
    fun <P : IPlayer, T> processYamlAndMapMessageKeys(
        data: Map<String, Any>,
        messageMap: MutableMap<MessageKey<P, T>, String>,
        expectedType: KClass<out MessageKey<P, T>>
    ) {
        val messageKeyMap: MutableMap<String, MessageKey<P, T>> = mutableMapOf()
        scanForMessageKeys(messageKeyMap, expectedType)
        processYamlData("", data, messageKeyMap, messageMap)
    }

    // Private helper methods
    private fun <P : IPlayer, T> scanForMessageKeys(
        messageKeyMap: MutableMap<String, MessageKey<P, T>>,
        expectedType: KClass<out MessageKey<P, T>>
    ) {
        val reflections = Reflections(
            ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(Core.get().packageName))
                .setScanners(Scanners.SubTypes)
        )
        val messageKeyClasses = reflections.getSubTypesOf(MessageKey::class.java)
        messageKeyClasses.forEach { clazz ->
            mapMessageKeys(clazz.kotlin, expectedType, "", messageKeyMap)
        }
    }

    private fun <P : IPlayer, T> mapMessageKeys(
        clazz: KClass<out MessageKey<*, *>>,
        expectedType: KClass<out MessageKey<P, T>>,
        currentPath: String = "",
        messageKeyMap: MutableMap<String, MessageKey<P, T>>
    ) {
        val className = clazz.simpleName?.lowercase() ?: return
        val fullPath = if (currentPath.isEmpty()) className else "$currentPath.$className"

        if (clazz == expectedType || clazz.isSubclassOf(expectedType)) {
            clazz.nestedClasses.forEach { nestedClass ->
                if (nestedClass.isSubclassOf(MessageKey::class)) {
                    mapMessageKeys(nestedClass as KClass<out MessageKey<*, *>>, expectedType, currentPath, messageKeyMap)
                }
            }
            return
        }

        val objectInstance = clazz.objectInstance
        if (expectedType.isInstance(objectInstance)) {
            messageKeyMap[fullPath] = objectInstance as MessageKey<P, T>
            if (Core.get().isDebug) {
                logger.info("Mapped class: $fullPath -> ${clazz.simpleName}")
            }
        }

        clazz.nestedClasses.forEach { nestedClass ->
            if (nestedClass.isSubclassOf(MessageKey::class)) {
                mapMessageKeys(nestedClass as KClass<out MessageKey<*, *>>, expectedType, fullPath, messageKeyMap)
            }
        }
    }

    private fun <P : IPlayer, T> processYamlData(
        prefix: String,
        data: Map<String, Any>,
        messageKeyMap: Map<String, MessageKey<P, T>>,
        messageMap: MutableMap<MessageKey<P, T>, String>
    ) {
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

    /**
     * Retrieves a localized message for the given player and message key, formatting
     * it with the specified arguments.
     *
     * @param player The player for whom the message is intended.
     * @param key The message key to retrieve.
     * @param args The arguments to format the message.
     * @return The localized message as a text component.
     */
    fun getMessage(player: P, key: MessageKey<P, T>, vararg args: Any): T {
        val lang = player.getLanguage()
        val message = messages[lang]?.get(key)
        val text = message?.let { String.format(it, *args) } ?: return key.c()
        return textComponentFactory(text)
    }

    /**
     * Retrieves a localized message for the player or returns a default message if the key is missing.
     *
     * @param player The player for whom the message is intended.
     * @param key The message key to retrieve.
     * @param defaultMessage The default message to use if the key is missing.
     * @param args The arguments to format the message.
     * @return The localized message as a text component.
     */
    fun getMessageOrDefault(player: P, key: MessageKey<P, T>, defaultMessage: String, vararg args: Any): T {
        val lang = player.getLanguage()
        val message = messages[lang]?.get(key)
        val text = message?.let { String.format(it, *args) } ?: defaultMessage
        return textComponentFactory(text)
    }

    /**
     * Retrieves the raw, untranslated message string for a given player and key.
     *
     * @param player The player for whom the message is intended.
     * @param key The message key to retrieve.
     * @return The untranslated message as a string.
     */
    fun getRawMessage(player: P, key: MessageKey<P, T>): String {
        return messages[player.getLanguage()]?.get(key) ?: key.rc()
    }

    /**
     * Retrieves a system-level message in the default locale.
     *
     * @param key The message key to retrieve.
     * @param args The arguments to format the message.
     * @return The system message as a string.
     */
    fun getSysMessage(key: MessageKey<P, T>, vararg args: Any): String {
        val lang = Locale.getDefault().language
        val message = messages[lang]?.get(key)
        val text = message?.let { String.format(it, *args) } ?: return key.rc()
        return text
    }

    /**
     * Checks if a localized message exists for a given player and key.
     *
     * @param player The player for whom the message is intended.
     * @param key The message key to check.
     * @return `true` if a localized message exists; otherwise, `false`.
     */
    fun hasMessage(player: P, key: MessageKey<P, T>): Boolean {
        val lang = player.getLanguage()
        return messages[lang]?.containsKey(key) ?: false
    }
}
