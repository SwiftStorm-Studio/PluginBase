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

    fun findMissingKeys(lang: String): List<String> {
        val messageKeyMap: MutableMap<String, MessageKey<P, C>> = mutableMapOf()
        scanForMessageKeys(messageKeyMap)
        val currentMessages = messages[lang] ?: return emptyList()
        val missingKeys = mutableListOf<String>()

        messageKeyMap.forEach { (path, key) ->
            if (!currentMessages.containsKey(key)) {
                missingKeys.add(path)
                Logger.warn("Missing key: $path for language: $lang")
            }
        }

        return missingKeys
    }

    fun processYamlAndMapMessageKeys(
        data: Map<String, Any>,
        // default fallback
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
        val fullPath = if (currentPath.isEmpty()) className.lowercase() else "$currentPath.${className.lowercase()}"

        val normalizedKey = normalizeKey(fullPath)
        val keysToRegister = generateKeyVariations(normalizedKey)

        Logger.logIfDebug("Mapping keys for class: ${clazz.simpleName}, fullPath: $fullPath")

          // Old code
//        val objectInstance = clazz.objectInstance
//        if (objectInstance == null) {
//            try {
//                val newInstance = clazz.createInstance()
//                if (castedExpectedMKType.isInstance(newInstance)) {
//                    messageKeyMap[fullPath] = newInstance as MessageKey<P, C>
//                    Logger.logIfDebug("Dynamically instantiated and added: $fullPath")
//                } else {
//                    Logger.logIfDebug("Skipping dynamically created instance: $fullPath is not of expected type", LogLevel.WARN)
//                }
//            } catch (e: Exception) {
//                Logger.logIfDebug("Failed to instantiate class: ${clazz.simpleName}, reason: ${e.message}")
//            }
//        } else if (castedExpectedMKType.isInstance(objectInstance)) {
//            messageKeyMap[fullPath] = objectInstance as MessageKey<P, C>
//        } else {
//            Logger.logIfDebug("Skipping ${clazz.simpleName}: not an instance of expected type", LogLevel.WARN)
//        }

        // 1. 該当クラスのインスタンスを取得または動的生成
        val objectInstance = clazz.objectInstance
        if (objectInstance == null) {
            try {
                val newInstance = clazz.createInstance()
                if (castedExpectedMKType.isInstance(newInstance)) {
                    keysToRegister.forEach { key ->
                        if (!messageKeyMap.containsKey(key)) {
                            messageKeyMap[key] = newInstance as MessageKey<P, C>
                            Logger.logIfDebug("Dynamically instantiated and added: $key")
                        }
                    }
                } else {
                    Logger.logIfDebug("Skipping dynamically created instance: $normalizedKey is not of expected type", LogLevel.WARN)
                }
            } catch (e: Exception) {
                Logger.logIfDebug("Failed to instantiate class: ${clazz.simpleName}, reason: ${e.message}")
            }
        } else if (castedExpectedMKType.isInstance(objectInstance)) {
            keysToRegister.forEach { key ->
                if (!messageKeyMap.containsKey(key)) {
                    messageKeyMap[key] = objectInstance as MessageKey<P, C>
                    Logger.logIfDebug("Registered key: $key")
                }
            }
        } else {
            Logger.logIfDebug("Skipping ${clazz.simpleName}: not an instance of expected type", LogLevel.WARN)
        }

        // Old code
//        if (!messageKeyMap.containsKey(fullPath)) {
//            Logger.logIfDebug("Registering intermediate path: $fullPath")
//            if (clazz.isSubclassOf(castedExpectedMKType)) {
//                try {
//                    val intermediateInstance = clazz.createInstance()
//                    if (castedExpectedMKType.isInstance(intermediateInstance)) {
//                        messageKeyMap[fullPath] = intermediateInstance as MessageKey<P, C>
//                        Logger.logIfDebug("Intermediate instance added: $fullPath")
//                    } else {
//                        Logger.logIfDebug("Intermediate instance is not of expected type: $fullPath", LogLevel.WARN)
//                    }
//                } catch (e: Exception) {
//                    Logger.logIfDebug("Intermediate path registration failed for: $fullPath, reason: ${e.message}")
//                }
//            } else {
//                Logger.logIfDebug("Class $fullPath is not a subclass of expected type ${castedExpectedMKType.simpleName}, skipping registration")
//            }
//        }

        // 2. 中間パスも登録 (キーとしてアクセスできるようにする)
        if (!messageKeyMap.containsKey(normalizedKey)) {
            Logger.logIfDebug("Registering intermediate path: $normalizedKey")
            if (clazz.isSubclassOf(castedExpectedMKType)) {
                try {
                    val intermediateInstance = clazz.createInstance()
                    if (castedExpectedMKType.isInstance(intermediateInstance)) {
                        keysToRegister.forEach { key ->
                            if (!messageKeyMap.containsKey(key)) {
                                messageKeyMap[key] = intermediateInstance as MessageKey<P, C>
                                Logger.logIfDebug("Intermediate instance added: $key")
                            }
                        }
                    } else {
                        Logger.logIfDebug("Intermediate instance is not of expected type: $normalizedKey", LogLevel.WARN)
                    }
                } catch (e: Exception) {
                    Logger.logIfDebug("Intermediate path registration failed for: $normalizedKey, reason: ${e.message}")
                }
            } else {
                Logger.logIfDebug("Class $normalizedKey is not a subclass of expected type ${castedExpectedMKType.simpleName}, skipping registration")
            }
        }

        // 3. 再帰的にネストされたクラスを探索
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
            Logger.logIfDebug("Processing key: $key, currentPrefix: $currentPrefix")
            if (key == "langVersion") {
                Logger.logIfDebug("Skipping langVersion key")
                continue
            }

            when (value) {
                is String -> {
                    val messageKey = messageKeyMap[currentPrefix]
                    if (messageKey != null) {
                        Logger.logIfDebug("Mapping message: $currentPrefix -> $value")
                        messageMap[messageKey] = value
                    } else {
                        Logger.logIfDebug("No message key found for YAML path: $currentPrefix", LogLevel.WARN)
                    }
                }
                is List<*> -> {
                    Logger.logIfDebug("Processing list at path: $currentPrefix with ${value.size} items")

                    // リストの要素を順に処理
                    value.forEachIndexed { index, element ->
                        val listPrefix = "$currentPrefix.item_$index"

                        when (element) {
                            is String -> {
                                // ITEM_0, ITEM_1... と対応する MessageKey を取得
                                val messageKey = messageKeyMap[listPrefix]
                                if (messageKey != null) {
                                    Logger.logIfDebug("Mapping list item: $listPrefix -> $element")
                                    messageMap[messageKey] = element
                                } else {
                                    Logger.logIfDebug("No message key found for list item path: $listPrefix", LogLevel.WARN)
                                }
                            }
                            is Map<*, *> -> {
                                Logger.logIfDebug("Encountered nested map in list at path: $listPrefix; diving deeper")
                                processYamlData(listPrefix, element as Map<String, Any>, messageKeyMap, messageMap)
                            }
                            else -> {
                                Logger.logIfDebug("Unexpected value type in list at path $listPrefix: ${element?.let { it::class.simpleName } ?: "null"}")
                            }
                        }
                    }
                }
                is Map<*, *> -> {
                    Logger.logIfDebug("Encountered nested structure at path: $currentPrefix; diving deeper")
                    processYamlData(currentPrefix, value as Map<String, Any>, messageKeyMap, messageMap)
                }
                else -> {
                    Logger.logIfDebug("Unexpected value type at path $currentPrefix: ${value::class.simpleName}")
                }
            }
        }

        Logger.logIfDebug("Completed YAML data processing for prefix: '$prefix'")
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

    private fun generateKeyVariations(normalizedKey: String): List<String> {
        return listOf(
            normalizedKey,
            normalizedKey.lowercase(),
            normalizedKey.uppercase()
        ).distinct()
    }

}
