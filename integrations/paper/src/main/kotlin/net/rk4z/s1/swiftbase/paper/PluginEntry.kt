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
    val configResourceRoot: String = "assets/${id}/config",

    val languageManagerInfo: LanguageManagerInfo<PaperPlayer, TextComponent>? = LanguageManagerInfo<PaperPlayer, TextComponent>(
        textComponentFactory = paperTextComponentFactory,
        expectedType = PaperMessageKey::class,
    ),
    val availableLang: List<String>? = null,
    val langDir: String? = null,
    val langResourceRoot: String = "assets/${id}/lang",

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
        Core.initialize<PaperPlayer, TextComponent>(
            packageName,
            isDebug,
            dataFolder,
            configFile,
            configResourceRoot,
            availableLang,
            langDir,
            langResourceRoot,
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