package net.rk4z.s1.swiftbase.paper

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.rk4z.s1.swiftbase.bstats.Metrics
import net.rk4z.s1.swiftbase.core.CB
import net.rk4z.s1.swiftbase.core.Core
import net.rk4z.s1.swiftbase.core.LanguageManagerInfo
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger

open class PluginEntry(
    @NotNull
    val id: String,
    @NotNull
    val packageName: String,

    val isDebug: Boolean = false,

    val configFile: String? = null,
    val configResourceRoot: String = "assets/config",

    val languageManagerInfo: LanguageManagerInfo<PaperPlayer, TextComponent>? = LanguageManagerInfo<PaperPlayer, TextComponent>(
        textComponentFactory = paperTextComponentFactory,
        expectedType = PaperMessageKey::class,
    ),
    val availableLang: List<String>? = null,
    val langDir: String? = null,
    val langResourceRoot: String = "assets/lang",

    val logger: Logger,

    var enableUpdateChecker: Boolean = true,
    val modrinthID: String = "",
    val serviceId: Int? = null,
) : JavaPlugin() {
    companion object {
        lateinit var metrics: Metrics
        lateinit var instance: PluginEntry

        @JvmStatic
        lateinit var key: NamespacedKey
            private set

        fun <I : PluginEntry> get() : I? {
            return instance as? I
        }

        val paperTextComponentFactory = { text: String -> Component.text(text) }
    }

    override fun onLoad() {
        val crrf = formatResourceRoot(configResourceRoot, id)
        val lrrf = formatResourceRoot(langResourceRoot, id)
        Core.initialize<PaperPlayer, TextComponent>(
            packageName,
            isDebug,
            dataFolder,
            configFile,
            crrf,
            availableLang,
            langDir,
            lrrf,
            S1Executor(this),
            logger,
            modrinthID,
            description.version,
            languageManagerInfo
        )
        instance = getPlugin(this::class.java)
        key = NamespacedKey(this, id)

        onLoadPre()

        CB.initializeDirectories()
        if (languageManagerInfo != null) {
        if (!isDebug) {
            CB.updateLanguageFilesIfNeeded()
        }
            CB.loadLanguageFiles()
        }

        onLoadPost()
    }

    override fun onEnable() {
        onEnablePre()

        if (serviceId != null) {
            metrics = Metrics(this, serviceId)
        } else {
            throw IllegalStateException("Service ID must be provided to enable metrics")
        }

        if (enableUpdateChecker) {
            CB.checkUpdate()
        }

        onEnablePost()
    }

    override fun onDisable() {
        onDisablePre()

        CB.executor.shutdown()

        onDisablePost()
    }

    /**
     * Formats the given resource root path by ensuring the provided `id` is inserted
     * as the second segment of the path, regardless of its original structure.
     *
     * The first segment of the path (before the first `/`) remains unchanged, and the `id`
     * is inserted directly after it.
     *
     * Examples:
     * ```
     * formatResourceRoot("assets/config", "myID") -> "assets/myID/config"
     * formatResourceRoot("example/path", "myID") -> "example/myID/path"
     * formatResourceRoot("singleSegment", "myID") -> "singleSegment/myID"
     * ```
     *
     * @param configResourceRoot The original resource root path to be formatted.
     * @param id The identifier to be inserted into the path.
     * @return The formatted resource root path with the `id` inserted as the second segment.
     */
    fun formatResourceRoot(configResourceRoot: String, id: String): String {
        // Split the path into segments using "/"
        val parts = configResourceRoot.split("/").filter { it.isNotEmpty() }

        // If there are no segments, return just the id
        if (parts.isEmpty()) return id

        // Insert id after the first segment
        val result = mutableListOf(parts[0], id)
        if (parts.size > 1) result.addAll(parts.subList(1, parts.size))

        // Join the modified list back into a string
        return result.joinToString("/")
    }

    // This is a wrapper for the core's lc method
    // (I just don't want to write `core.lc<T>(key)` every time)
    inline fun <reified T> lc(key: String): T? {
        return CB.lc<T>(key)
    }

    fun loadLanguageFileFromResourcePacks() {
        throw NotImplementedError("This method is not implemented for Paper plugins yet")
    }

    open fun onLoadPre() {}
    open fun onLoadPost() {}
    open fun onEnablePre() {}
    open fun onEnablePost() {}
    open fun onDisablePre() {}
    open fun onDisablePost() {}
}