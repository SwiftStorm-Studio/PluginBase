package net.rk4z.s1.pluginBase;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JLanguageManager {
    private static final JLanguageManager INSTANCE = new JLanguageManager();

    private final Map<String, Map<JMessageKey, String>> messages = new ConcurrentHashMap<>();

    private JLanguageManager() {}

    public static JLanguageManager getInstance() {
        return INSTANCE;
    }

    public List<String> findMissingKeys(String lang) {
        Map<String, JMessageKey> messageKeyMap = new HashMap<>();
        scanForMessageKeys(messageKeyMap);

        Map<JMessageKey, String> currentMessages = messages.getOrDefault(lang, new HashMap<>());
        List<String> missingKeys = new ArrayList<>();

        for (Map.Entry<String, JMessageKey> entry : messageKeyMap.entrySet()) {
            if (!currentMessages.containsKey(entry.getValue())) {
                missingKeys.add(entry.getKey());
                PluginEntry.getLogger().warn("Missing key: {} for language: {}", entry.getKey(), lang);
            }
        }

        return missingKeys;
    }

    private void scanForMessageKeys(Map<String, JMessageKey> messageKeyMap) {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setUrls(ClasspathHelper.forPackage(PluginEntry.get().getPackageName()))
                        .setScanners(Scanners.SubTypes)
        );

        Set<Class<? extends JMessageKey>> messageKeyClasses = reflections.getSubTypesOf(JMessageKey.class);
        for (Class<? extends JMessageKey> clazz : messageKeyClasses) {
            mapMessageKeys(clazz, "", messageKeyMap);
        }
    }

    private void mapMessageKeys(@NotNull Class<? extends JMessageKey> clazz, @NotNull String currentPath, Map<String, JMessageKey> messageKeyMap) {
        String className = clazz.getSimpleName().toLowerCase();
        String fullPath = currentPath.isEmpty() ? className : currentPath + "." + className;

        try {
            JMessageKey instance = clazz.getDeclaredConstructor().newInstance();
            messageKeyMap.put(fullPath, instance);

            if (PluginEntry.get().isDebug()) {
                PluginEntry.getLogger().info("Mapped class: {} -> {}", fullPath, clazz.getSimpleName());
            }
        } catch (Exception e) {
            PluginEntry.getLogger().error("Failed to instantiate message key: {}", clazz.getName(), e);
        }
    }

    public TextComponent getMessage(Player player, JMessageKey key, Object... args) {
        String lang = getPlayerLanguage(player);
        String message = messages.getOrDefault(lang, new HashMap<>()).get(key);

        if (message != null) {
            return Component.text(String.format(message, args));
        }
        return key.c();
    }

    public TextComponent getMessageOrDefault(Player player, JMessageKey key, String defaultMessage, Object... args) {
        String lang = getPlayerLanguage(player);
        String message = messages.getOrDefault(lang, new HashMap<>()).get(key);

        if (message != null) {
            return Component.text(String.format(message, args));
        }
        return Component.text(defaultMessage);
    }

    public String getRawMessage(Player player, JMessageKey key) {
        return messages.getOrDefault(getPlayerLanguage(player), new HashMap<>()).getOrDefault(key, key.rc());
    }

    public String getSysMessage(JMessageKey key, Object... args) {
        String lang = Locale.getDefault().getLanguage();
        String message = messages.getOrDefault(lang, new HashMap<>()).get(key);

        if (message != null) {
            return String.format(message, args);
        }
        return key.rc();
    }

    public boolean hasMessage(Player player, JMessageKey key) {
        String lang = getPlayerLanguage(player);
        return messages.getOrDefault(lang, new HashMap<>()).containsKey(key);
    }

    public String getPlayerLanguage(@NotNull Player player) {
        return player.locale().getLanguage() != null ? player.locale().getLanguage() : "en";
    }
}
