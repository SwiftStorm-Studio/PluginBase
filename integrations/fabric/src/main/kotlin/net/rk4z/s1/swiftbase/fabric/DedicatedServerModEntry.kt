package net.rk4z.s1.swiftbase.fabric

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.text.Text
import net.rk4z.s1.swiftbase.core.Core
import net.rk4z.s1.swiftbase.core.LanguageManager
import net.rk4z.s1.swiftbase.core.MessageKey
import net.rk4z.s1.swiftbase.core.SystemHelper
import org.jetbrains.annotations.NotNull
import org.reflections.Reflections.log
import java.nio.charset.StandardCharsets
import java.nio.file.Files

@Suppress("DuplicatedCode", "UNCHECKED_CAST", "unused")
open class DedicatedServerModEntry(
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
    var enableUpdateChecker: Boolean = true,
    val modrinthID: String? = null,
    val availableLang: List<String>? = null,
    val autoLanguageUpdate: Boolean = true,
    val useConfigFile: Boolean = true,
    val useLanguageSystem: Boolean = true
) : DedicatedServerModInitializer {
    companion object {
        private lateinit var instance: DedicatedServerModEntry

        @JvmStatic
        lateinit var core: Core
            private set

        @JvmStatic
        lateinit var languageManager: LanguageManager<*, *>
            private set

        fun <I : DedicatedServerModEntry> get() : I? {
            return instance as? I
        }

        internal fun get() : DedicatedServerModEntry? {
            return get<DedicatedServerModEntry>()
        }

        val fabricTextComponentFactory = { text: String -> Text.of(text) }
    }

    var onCheckUpdate: () -> Unit = {}
    var onAllVersionsRetrieved: (versionCount: Int) -> Unit = {}
    var onNewVersionFound: (latestVersion: String, newerVersionCount: Int) -> Unit = { _, _ -> }
    var onNoNewVersionFound: () -> Unit = {}
    var onUpdateCheckFailed: (responseCode: Int) -> Unit = {}
    var onUpdateCheckError: (e: Exception) -> Unit = {}

    val loader = FabricLoader.getInstance()
    val dataFolder = loader.gameDir.resolve(id).toFile()
    val meta = loader.getModContainer(id).get().metadata
    val version = meta.version.friendlyString

    override fun onInitializeServer() {
        if (!ModEntry.isInitialized) {
            initSystem()

            if (enableUpdateChecker) {
                onCheckUpdate()
                core.checkUpdate()
            }

            ServerLifecycleEvents.SERVER_STOPPED.register {
                onServerStoppingPre()

                stopSystem()

                onServerStoppingPost()
            }
        } else {
            instance = this
            core = ModEntry.core
            languageManager = ModEntry.languageManager
        }
    }

    private fun initSystem() {
        core = SystemHelper.createCore(
            packageName,
            dataFolder,
            version,
            modrinthID,
            useConfigFile,
            useLanguageSystem,
            isDebug,
            availableLang,
            S2Executor()
        )
        instance = this
        languageManager = SystemHelper.createLanguageManager<FabricPlayerAdapter, Text>(fabricTextComponentFactory)

        this.onCheckUpdate = core.onCheckUpdate
        this.onAllVersionsRetrieved = core.onAllVersionsRetrieved
        this.onNewVersionFound = core.onNewVersionFound
        this.onNoNewVersionFound = core.onNoNewVersionFound
        this.onUpdateCheckFailed = core.onUpdateCheckFailed
        this.onUpdateCheckError = core.onUpdateCheckError

        onInitializingSystemPre()

        core.initializeDirectories()
        if (!isDebug) {
            if (autoLanguageUpdate) {
                core.updateLanguageFilesIfNeeded()
            }
        }
        if (useLanguageSystem) {
            loadLanguageFiles()
        }

        onInitializingSystemPost()
    }

    private fun stopSystem() {
        onShuttingDownSystemPre()

        core.executor.shutdown()

        onShuttingDownSystemPost()
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
                        val messageMap: MutableMap<MessageKey<*, *>, String> = mutableMapOf()
                        languageManager.processYamlAndMapMessageKeys(data, messageMap)
                        languageManager.messages[lang] = messageMap
                    }
                } else {
                    log.warn("Language file for '$lang' not found.")
                }
            }
        }
    }

    open fun onInitializingSystemPre() {}
    open fun onInitializingSystemPost() {}
    open fun onShuttingDownSystemPre() {}
    open fun onShuttingDownSystemPost() {}

    open fun onServerStoppingPre() {}
    open fun onServerStoppingPost() {}
}