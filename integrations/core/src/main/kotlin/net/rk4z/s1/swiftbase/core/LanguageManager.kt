package net.rk4z.s1.swiftbase.core

import net.rk4z.s1.swiftbase.core.dummy.DummyMessageKey
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf

@Suppress("UNCHECKED_CAST")
open class LanguageManager<P : IPlayer<C>, C> private constructor(
    val textComponentFactory: (String) -> C,
    val expectedMKType: KClass<out MessageKey<*, *>>
) {
    companion object {
        var instance: LanguageManager<*, *> = LanguageManager<IPlayer<Nothing>, Nothing>(
            // ダミーファクトリー。このキャストは必ず失敗する。
            { it as Nothing },
            DummyMessageKey::class
        )

        /**
         * Create a new LanguageManager instance.
         * If you're using [Core], the language manager will automatically be created.
         *
         * @param textComponentFactory The factory to create a new text component.
         * @param expectedType The expected type of the message key.
         * @return The created language manager.
         * @throws IllegalStateException If the language manager is already created by [Core].
         */
        fun <P : IPlayer<C>, C> initialize(
            textComponentFactory: (String) -> C,
            expectedType: KClass<out MessageKey<P, C>>
        ): LanguageManager<P, C> {
            if (Core.isInitialized()) {
                throw IllegalStateException("LanguageManager already created by Core.")
            }

            val languageManager: LanguageManager<P, C> = LanguageManager(textComponentFactory, expectedType)

            instance = languageManager

            return languageManager
        }

        @JvmStatic
        fun <P : IPlayer<C>, C> get(): LanguageManager<P, C> {
            return instance as LanguageManager<P, C>
        }

        @JvmStatic
        fun isInitialized(): Boolean {
            return instance.expectedMKType != DummyMessageKey::class
        }
    }

    val messages: MutableMap<String, MutableMap<MessageKey<P, C>, String>> = mutableMapOf()

    fun findMissingKeys(lang: String) {
        Logger.logIfDebug("Starting findMissingKeysForLanguage for language: $lang")

        val messageKeyMap: MutableMap<String, MessageKey<P, C>> = mutableMapOf()
        scanForMessageKeys(messageKeyMap)

        val yamlData = messages[lang]
        if (yamlData == null) {
            Logger.logIfDebug("No YAML data found for language: $lang")
            return
        }

        val yamlKeys: MutableSet<String> = mutableSetOf()
        collectYamlKeysFromMessages(yamlData, yamlKeys)

        Logger.logIfDebug("Keys in messageKeyMap: ${messageKeyMap.keys.joinToString(", ")}")
        Logger.logIfDebug("Keys in YAML for language '$lang': ${yamlKeys.joinToString(", ")}")

        val missingKeys = messageKeyMap.keys.filter { it !in yamlKeys }

        if (missingKeys.isNotEmpty()) {
            Logger.logIfDebug("Missing keys for language '$lang': ${missingKeys.joinToString(", ")}")
        } else {
            Logger.logIfDebug("No missing keys found for language '$lang'. All class keys are present in YAML.")
        }
    }

    fun processYamlAndMapMessageKeys(
        data: Map<String, Any>,
        lang: String = "en"
    ) {
        Logger.logIfDebug("Starting to process YAML and map message keys for language: $lang")

        val messageKeyMap: MutableMap<String, MessageKey<P, C>> = mutableMapOf()
        val messageMap: MutableMap<MessageKey<P, C>, String> = mutableMapOf()

        // クラス構造に基づいてメッセージキーを探索
        scanForMessageKeys(messageKeyMap)
        Logger.logIfDebug("MessageKey map generated with ${messageKeyMap.size} keys for language: $lang")

        // YAMLデータをマッピング
        processYamlData("", data, messageKeyMap, messageMap)
        Logger.logIfDebug("YAML data processed for language: $lang with ${messageMap.size} entries")

        // messagesにマップを格納
        messages[lang] = messageMap
        Logger.logIfDebug("Message map stored for language: $lang")
    }

    // Private helper functions
    private fun scanForMessageKeys(
        messageKeyMap: MutableMap<String, MessageKey<P, C>>
    ) {
        Logger.logIfDebug("Starting scan for message keys of expected type: ${expectedMKType.simpleName}")

        val reflections = Reflections(
            ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(Core.getInstance().packageName))
                .setScanners(Scanners.SubTypes)
        )

        val messageKeyClasses = reflections.getSubTypesOf(MessageKey::class.java)
        Logger.logIfDebug("Found ${messageKeyClasses.size} potential MessageKey classes to examine")

        messageKeyClasses.forEach { clazz ->
            Logger.logIfDebug("Examining class: ${clazz.kotlin.qualifiedName}")
            mapMessageKeys(clazz.kotlin, "", messageKeyMap)
        }

        Logger.logIfDebug("Completed scanning for message keys.")
        Logger.logIfDebug("Final MessageKey map contains: ${messageKeyMap.keys.joinToString(", ")}")
    }

    private fun mapMessageKeys(
        clazz: KClass<out MessageKey<*, *>>,
        currentPath: String = "",
        messageKeyMap: MutableMap<String, MessageKey<P, C>>
    ) {
        val castedExpectedMKType = expectedMKType as KClass<out MessageKey<P, C>>

        val className = clazz.simpleName ?: return
        val fullPath = if (currentPath.isEmpty()) className else "$currentPath.$className"

        val normalizedKey = normalizeKey(fullPath)

        Logger.logIfDebug("Mapping key for class: $className, normalized: $normalizedKey")

        val objectInstance = clazz.objectInstance ?: clazz.createInstanceOrNull()
        if (objectInstance != null && castedExpectedMKType.isInstance(objectInstance)) {
            if (!messageKeyMap.containsKey(normalizedKey)) {
                messageKeyMap[normalizedKey] = objectInstance as MessageKey<P, C>
                Logger.logIfDebug("Registered key: $normalizedKey")
            }
        }

        clazz.nestedClasses.forEach { nestedClass ->
            if (nestedClass.isSubclassOf(castedExpectedMKType)) {
                mapMessageKeys(nestedClass as KClass<out MessageKey<P, C>>, fullPath, messageKeyMap)
            }
        }
    }

    private fun processYamlData(
        prefix: String,
        data: Map<String, Any>,
        messageKeyMap: Map<String, MessageKey<P, C>>,
        messageMap: MutableMap<MessageKey<P, C>, String>
    ) {
        Logger.logIfDebug("Starting YAML data processing with prefix: '$prefix'")
        Logger.logIfDebug("Available keys in MessageKey map: ${messageKeyMap.keys.joinToString(", ")}")

        for ((key, value) in data) {
            val currentPrefix = if (prefix.isEmpty()) key else "$prefix.$key"
            val normalizedPrefix = normalizeKey(currentPrefix) // 正規化されたキー
            Logger.logIfDebug("Processing key: $key, currentPrefix: $currentPrefix, normalized: $normalizedPrefix")

            if (key == "langVersion") {
                Logger.logIfDebug("Skipping langVersion key")
                continue
            }

            when (value) {
                is String -> {
                    val messageKey = messageKeyMap[normalizedPrefix]
                    if (messageKey != null) {
                        Logger.logIfDebug("Mapping message: $normalizedPrefix -> $value")
                        messageMap[messageKey] = value
                    } else {
                        Logger.logIfDebug("No message key found for YAML path: $normalizedPrefix", LogLevel.WARN)
                    }
                }

                is List<*> -> {
                    Logger.logIfDebug("Processing list at path: $normalizedPrefix with ${value.size} items")
                    value.forEachIndexed { index, element ->
                        val listPrefix = "$currentPrefix.item_$index"
                        val normalizedListPrefix = normalizeKey(listPrefix)

                        when (element) {
                            is String -> {
                                val messageKey = messageKeyMap[normalizedListPrefix]
                                if (messageKey != null) {
                                    Logger.logIfDebug("Mapping list item: $normalizedListPrefix -> $element")
                                    messageMap[messageKey] = element
                                } else {
                                    Logger.logIfDebug("No message key found for list item path: $normalizedListPrefix", LogLevel.WARN)
                                }
                            }
                            is Map<*, *> -> {
                                Logger.logIfDebug("Encountered nested map in list at path: $normalizedListPrefix; diving deeper")
                                processYamlData(listPrefix, element as Map<String, Any>, messageKeyMap, messageMap)
                            }
                            else -> {
                                Logger.logIfDebug("Unexpected value type in list at path $normalizedListPrefix: ${element?.let { it::class.simpleName } ?: "null"}")
                            }
                        }
                    }
                }

                is Map<*, *> -> {
                    Logger.logIfDebug("Encountered nested structure at path: $normalizedPrefix; diving deeper")
                    processYamlData(currentPrefix, value as Map<String, Any>, messageKeyMap, messageMap)
                }

                else -> {
                    Logger.logIfDebug("Unexpected value type at path $normalizedPrefix: ${value::class.simpleName}")
                }
            }
        }

        Logger.logIfDebug("Completed YAML data processing for prefix: '$prefix'")
    }

    private fun collectYamlKeysFromMessages(
        messages: Map<MessageKey<P, C>, String>,
        yamlKeys: MutableSet<String>
    ) {
        for (messageKey in messages.keys) {
            val normalizedKey = normalizeKey(messageKey.rc())
            yamlKeys.add(normalizedKey)
        }
    }

    /**
     * Get a system message from the language manager.
     * The language will be determined by the system's default locale.
     *
     * @param key The message key to retrieve.
     * @param args The arguments to format the message with.
     */
    fun getSysMessage(key: MessageKey<*, *>, vararg args: Any): String {
        val lang = Locale.getDefault().language
        val message = messages[lang]?.get(key)
        val text = message?.let { String.format(it, *args) } ?: return key.rc()
        return text
    }

    /**
     * Get a system message from the language manager.
     * The language will be determined by the system's default locale.
     *
     * @param key The message key to retrieve.
     * @param args The arguments to format the message with.
     */
    fun getSysMessageByLangCode(key: MessageKey<*, *>, lang: String, vararg args: Any): String {
        val message = messages[lang]?.get(key)
        val text = message?.let { String.format(it, *args) } ?: return key.rc()
        return text
    }

    private fun normalizeKey(key: String): String {
        return key.lowercase().replace("_", "")
    }

    private fun generateKeyVariations(key: String): List<String> {
        val normalizedKey = normalizeKey(key)
        return listOf(
            key,
            normalizedKey,
            normalizedKey.uppercase()
        ).distinct()
    }

    private fun toSnakeCase(key: String): String {
        return key.replace(Regex("([a-z])([A-Z]+)"), "$1_$2").lowercase()
    }
}
