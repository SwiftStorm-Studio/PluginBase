package net.rk4z.s1.pluginBase;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;

/**
 * Abstract class representing a message key, used to generate structured messages
 * and handle localization within the plugin.
 */
public abstract class JMessageKey {
    /**
     * Returns the default message or key name as a TextComponent, using the simple name of the class.
     * This is used as a fallback when no localized message is found.
     *
     * @return A TextComponent containing the simple name of the message key class.
     */
    public TextComponent c() {
        return Component.text(getClass().getSimpleName());
    }

    /**
     * Retrieves the raw string representation of the message key, without any formatting.
     * This can be useful when working with systems that do not support TextComponent.
     *
     * @return A String representing the raw message key or the key's simple name if no message is found.
     */
    public String rc() {
        return getClass().getSimpleName();
    }

    /**
     * Logs the message key as a message to the console.
     *
     * @param level The logging level (INFO, WARN, ERROR, etc.).
     */
    public void log(String level) {
        String message = rc();
        switch (level.toUpperCase()) {
            case "INFO" -> PluginEntry.getLogger().info(message);
            case "WARN" -> PluginEntry.getLogger().warn(message);
            case "ERROR" -> PluginEntry.getLogger().error(message);
            default -> PluginEntry.getLogger().debug(message);
        }
    }

    /**
     * Logs the message key as a message to the console with default INFO level.
     */
    public void log() {
        log("INFO");
    }

    /**
     * Retrieves the localized message for the given player based on their language settings.
     * If no message is found for the player's language, the key name itself is returned as a fallback.
     *
     * @param player The player whose language settings will be used to retrieve the localized message.
     * @return A TextComponent containing the localized message or the key name if no message is found.
     */
    public TextComponent t(Player player) {
        return JLanguageManager.getInstance().getMessage(player, this);
    }
}