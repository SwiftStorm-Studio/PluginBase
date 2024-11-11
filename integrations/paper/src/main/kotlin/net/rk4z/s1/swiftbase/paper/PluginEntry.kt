package net.rk4z.s1.swiftbase.paper

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.rk4z.s1.swiftbase.bstats.Metrics
import net.rk4z.s1.swiftbase.core.*
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files

@Suppress("UNCHECKED_CAST", "unused", "DEPRECATION", "MemberVisibilityCanBePrivate")
open class PluginEntry(
    @NotNull
    val id: String,
    /**
     * This package name is used for event translation key detection.
     *
     * You should set the package name of the main class of your plugin.
     */
    @NotNull
    val packageName: String,

    @NotNull
    val configFileRoot: String,

    @NotNull
    val langDirRoot: String,

    val isDebug: Boolean = false,
    var enableMetrics: Boolean = false,
    val serviceId: Int? = null,
    var enableUpdateChecker: Boolean = true,
    val modrinthID: String? = null,
    val availableLang: List<String>? = null,
    val autoLanguageUpdate: Boolean = true,
    val useConfigFile: Boolean = true,
    val useLanguageSystem: Boolean = true
) : JavaPlugin() {
    companion object {
        private lateinit var metrics: Metrics
        private lateinit var instance: PluginEntry

        @JvmStatic
        lateinit var core: Core
            private set

        @JvmStatic
        lateinit var key: NamespacedKey
            private set

        @JvmStatic
        lateinit var languageManager: LanguageManager<*, *>
            private set

        fun <I : PluginEntry> get() : I? {
            return instance as? I
        }

        val paperTextComponentFactory = { text: String -> Component.text(text) }
    }

    var onCheckUpdate: () -> Unit = {}
    var onAllVersionsRetrieved: (versionCount: Int) -> Unit = {}
    var onNewVersionFound: (latestVersion: String, newerVersionCount: Int) -> Unit = { _, _ -> }
    var onNoNewVersionFound: () -> Unit = {}
    var onUpdateCheckFailed: (responseCode: Int) -> Unit = {}
    var onUpdateCheckError: (e: Exception) -> Unit = {}

    @Deprecated("Do not override this method. Use event system instead.")
    override fun onLoad() {
        core = SystemHelper.createCore(
            packageName,
            this.dataFolder,
            description.version,
            modrinthID,
            useConfigFile,
            useLanguageSystem,
            isDebug,
            availableLang,
            S1Executor(this),
            configFileRoot,
            langDirRoot,
            LoggerFactory.getLogger(this::class.java.simpleName)
        )
        instance = getPlugin(this::class.java)
        key = NamespacedKey(this, id)
        languageManager = SystemHelper.createLanguageManager(paperTextComponentFactory, PaperMessageKey::class)
        Core.Companion.platformLogger = LoggerFactory.getLogger(this::class.java.simpleName)

        this.onCheckUpdate = core.onCheckUpdate
        this.onAllVersionsRetrieved = core.onAllVersionsRetrieved
        this.onNewVersionFound = core.onNewVersionFound
        this.onNoNewVersionFound = core.onNoNewVersionFound
        this.onUpdateCheckFailed = core.onUpdateCheckFailed
        this.onUpdateCheckError = core.onUpdateCheckError

        onLoadPre()

        core.initializeDirectories()
        if (!isDebug) {
            if (autoLanguageUpdate) {
                core.updateLanguageFilesIfNeeded()
            }
        }
        if (useLanguageSystem) {
            loadLanguageFiles()
        }

        onLoadPost()
    }

    @Deprecated("Do not override this method. Use event system instead.")
    override fun onEnable() {
        onEnablePre()

        if (enableMetrics) {
            if (serviceId != null) {
                metrics = Metrics(this, serviceId)
            } else {
                throw IllegalStateException("Service ID must be provided to enable metrics")
            }
        }

        if (enableUpdateChecker) {
            core.onCheckUpdate()
            core.checkUpdate()
        }

        onEnablePost()
    }

    @Deprecated("Do not override this method. Use event system instead.")
    override fun onDisable() {
        onDisablePre()

        core.executor.shutdown()

        onDisablePost()
    }

    // This is a wrapper for the core's lc method
    // (I just don't want to write `core.lc<T>(key)` every time)
    inline fun <reified T> lc(key: String): T? {
        return core.lc<T>(key)
    }

    fun loadLanguageFiles() {
        availableLang?.let {
            it.forEach { lang ->
                val langFile = core.langDir.resolve("$lang.yml")
                if (Files.exists(langFile.toPath())) {
                    Files.newBufferedReader(langFile.toPath(), StandardCharsets.UTF_8).use { reader ->
                        val data: Map<String, Any> = core.yaml.load(reader)
                        val messageMap: MutableMap<MessageKey<PaperPlayerAdapter, TextComponent>, String> = mutableMapOf()
                        languageManager.processYamlAndMapMessageKeys(data, messageMap, PaperMessageKey::class)
                        languageManager.messages[lang] = messageMap as MutableMap<PaperMessageKey, String>
                    }
                } else {
                    Logger.warn("Language file for '$lang' not found.")
                }
            }
        }
    }

    // You can override these methods to handle the update check results
    open fun onLoadPre() {}
    open fun onLoadPost() {}
    open fun onEnablePre() {}
    open fun onEnablePost() {}
    open fun onDisablePre() {}
    open fun onDisablePost() {}
}