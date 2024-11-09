package net.rk4z.s1.swiftbase.fabric

import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModMetadata
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.rk4z.s1.swiftbase.core.Core
import net.rk4z.s1.swiftbase.core.SystemHelper
import net.rk4z.s1.swiftbase.core.LanguageManager
import net.rk4z.s1.swiftbase.core.MessageKey
import org.jetbrains.annotations.NotNull
import org.reflections.Reflections.log
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

@Suppress("DuplicatedCode", "MemberVisibilityCanBePrivate", "unused", "UNCHECKED_CAST")
open class ModEntry(
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
) : ModInitializer {
    companion object {
        internal lateinit var instance: ModEntry
            private set

        @JvmStatic
        lateinit var core: Core
            private set

        @JvmStatic
        lateinit var languageManager: LanguageManager<*, *>
            private set

        fun <I : ModEntry> get() : I? {
            return instance as? I
        }

        internal fun get() : ModEntry? {
            return get<ModEntry>()
        }

        val fabricTextComponentFactory = { text: String -> Text.of(text) }

        val isInitialized: Boolean
            get() = ::instance.isInitialized
    }

    var onCheckUpdate: () -> Unit = {}
    var onAllVersionsRetrieved: (versionCount: Int) -> Unit = {}
    var onNewVersionFound: (latestVersion: String, newerVersionCount: Int) -> Unit = { _, _ -> }
    var onNoNewVersionFound: () -> Unit = {}
    var onUpdateCheckFailed: (responseCode: Int) -> Unit = {}
    var onUpdateCheckError: (e: Exception) -> Unit = {}

    val loader: FabricLoader = FabricLoader.getInstance()
    var client = if (loader.environmentType == EnvType.CLIENT) MinecraftClient.getInstance() else null
    val dataFolder: File = loader.gameDir.resolve(id).toFile()
    val meta: ModMetadata = loader.getModContainer(id).get().metadata
    val version: String = meta.version.friendlyString

    override fun onInitialize() {
        if (loader.environmentType == EnvType.CLIENT) {

            ClientLifecycleEvents.CLIENT_STARTED.register {
                onClientStartedPre()

                initSystem()

                if (enableUpdateChecker) {
                    onCheckUpdate()
                    core.checkUpdate()
                }

                onClientStartedPost()
            }

            ClientLifecycleEvents.CLIENT_STOPPING.register {
                onClientStoppingPre()

                stopSystem()

                onClientStoppingPost()
            }

        } else if (loader.environmentType == EnvType.SERVER) {

            ServerLifecycleEvents.SERVER_STARTING.register {
                onServerStartingPre()

                initSystem()

                if (enableUpdateChecker) {
                    onCheckUpdate()
                    core.checkUpdate()
                }

                onServerStartingPost()
            }

            ServerLifecycleEvents.SERVER_STARTED.register {
                onServerStartedPre()

                if (enableUpdateChecker) {
                    onCheckUpdate()
                    core.checkUpdate()
                }

                onServerStartedPost()
            }

            ServerLifecycleEvents.SERVER_STOPPING.register {
                onServerStopping()
            }

            ServerLifecycleEvents.SERVER_STOPPED.register {
                onServerStopped()
            }

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

    open fun onClientStartedPre() {}
    open fun onClientStartedPost() {}
    open fun onClientStoppingPre() {}
    open fun onClientStoppingPost() {}

    open fun onServerStartingPre() {}
    open fun onServerStartingPost() {}
    open fun onServerStartedPre() {}
    open fun onServerStartedPost() {}
    open fun onServerStopping() {}
    open fun onServerStopped() {}
}