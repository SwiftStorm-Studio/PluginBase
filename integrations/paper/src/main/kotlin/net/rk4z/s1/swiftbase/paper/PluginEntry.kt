package net.rk4z.s1.swiftbase.paper

import net.rk4z.s1.swiftbase.bstats.Metrics
import net.rk4z.s1.swiftbase.core.Core
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.NotNull

@Suppress("UNCHECKED_CAST")
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
        private var instance: PluginEntry? = null

        @JvmStatic
        lateinit var core: Core
            private set

        @JvmStatic
        lateinit var key: NamespacedKey
            private set

        fun <I : PluginEntry> get() : I {
            return instance as? I ?: throw IllegalStateException("Plugin instance is not initialized yet")
        }
    }

    var onCheckUpdate: () -> Unit = {}
    var onAllVersionsRetrieved: (versionCount: Int) -> Unit = {}
    var onNewVersionFound: (latestVersion: String, newerVersionCount: Int) -> Unit = { _, _ -> }
    var onNoNewVersionFound: () -> Unit = {}
    var onUpdateCheckFailed: (responseCode: Int) -> Unit = {}
    var onUpdateCheckError: (e: Exception) -> Unit = {}

    @Deprecated("Do not override this method. Use event system instead.")
    override fun onLoad() {
        core = Core(
            packageName,
            this.dataFolder,
            useConfigFile,
            useLanguageSystem,
            isDebug,
            availableLang,
            description.version,
            modrinthID
        )
        instance = getPlugin(this::class.java)
        key = NamespacedKey(this, id)
        Core.executor = S1Executor(this)

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
            core.loadLanguageFiles()
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

        Core.executor.shutdown()

        onDisablePost()
    }

    // This is a wrapper for the core's lc method
    // (I just don't want to write `core.lc<T>(key)` every time)
    inline fun <reified T> lc(key: String): T? {
        return core.lc<T>(key)
    }

    // You can override these methods to handle the update check results
    open fun onLoadPre() {}
    open fun onLoadPost() {}
    open fun onEnablePre() {}
    open fun onEnablePost() {}
    open fun onDisablePre() {}
    open fun onDisablePost() {}
}