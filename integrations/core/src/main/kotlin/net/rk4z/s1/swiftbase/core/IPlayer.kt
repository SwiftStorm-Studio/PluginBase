package net.rk4z.s1.swiftbase.core

/**
 * Represents a player in the system, providing access to player-specific
 * information and functionality, such as the player's language.
 *
 * This interface is intended to be implemented by classes that manage player
 * data, enabling localization and other language-based functionalities.
 */
interface IPlayer {
    /**
     * Retrieves the language code used by this player, typically for localization
     * and language-specific messages.
     *
     * @return A [String] representing the player's language code, for example, "en" for English.
     */
    fun getLanguage(): String
}
