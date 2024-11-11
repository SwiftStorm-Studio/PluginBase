package net.rk4z.s1.swiftbase.core

import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST", "unused")
class LanguageManager<P : IPlayer, T>(
    val textComponentFactory: (String) -> T,
    val expectedType: KClass<out MessageKey<P, T>>
) {
    companion object {
        lateinit var instance: LanguageManager<*, *>

        fun <P : IPlayer, T> initialize(
            textComponentFactory: (String) -> T,
            expectedType: KClass<out MessageKey<P, T>>
        ): LanguageManager<P, T> {
            if (::instance.isInitialized) {
                throw IllegalStateException("LanguageManager is already initialized.")
            }

            instance = LanguageManager(textComponentFactory, expectedType)
            return instance as? LanguageManager<P, T>
                ?: throw IllegalStateException("LanguageManager is not properly initialized with the correct types")
        }

        fun <P : IPlayer, T> get(): LanguageManager<P, T> {
            if (!::instance.isInitialized) {
                throw IllegalStateException("LanguageManager has not been initialized.")
            }

            return instance as? LanguageManager<P, T>
                ?: throw IllegalStateException("LanguageManager is not properly initialized with the correct types")
        }
    }

    val messages: MutableMap<String, MutableMap<out MessageKey<P, T>, String>> = mutableMapOf()

    fun getMessage(player: P, key: MessageKey<P, T>, vararg args: Any): T {
        val language = player.getLanguage()
        val languageMessages = messages[language]
            // If the language is not found, return the key's simple name
            ?: return key.c()

        val messageTemplate = languageMessages[key]
            // If the key is not found, return the key's simple name
            ?: return key.c()

        val formattedMessage = messageTemplate.format(*args)
        return textComponentFactory(formattedMessage)
    }
}
