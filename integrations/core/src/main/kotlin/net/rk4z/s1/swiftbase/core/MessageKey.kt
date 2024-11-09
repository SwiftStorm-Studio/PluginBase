package net.rk4z.s1.swiftbase.core

import net.rk4z.s1.swiftbase.core.Core.Companion.logger

/**
 * A interface representing a message key, used to generate structured messages
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
@Suppress("unused")
interface MessageKey<P : IPlayer, T> {

    /**
     * Provides a default fallback localized text component for this message key.
     *
     * @return A default text component generated from the key's simple name.
     */
    fun c(): T {
        return LanguageManager.get<P, T>().textComponentFactory(this.javaClass.simpleName)
    }

    /**
     * Returns the raw string representation of the key's simple name.
     *
     * @return The key's simple name as a raw string.
     */
    fun rc(): String {
        return this.javaClass.simpleName
    }

    /**
     * Logs the key's raw name at the specified log level.
     *
     * @param level The log level at which to log this key's name. Defaults to "INFO".
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
     * Retrieves the localized message for a given player.
     *
     * @param player The player for whom to retrieve the localized message.
     * @return The localized message as a text component.
     */
    fun t(player: P): T {
        return LanguageManager.get<P, T>().getMessage(player, this)
    }
}