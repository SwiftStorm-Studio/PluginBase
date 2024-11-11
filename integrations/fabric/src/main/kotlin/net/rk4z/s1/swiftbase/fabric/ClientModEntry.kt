package net.rk4z.s1.swiftbase.fabric

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModMetadata
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.rk4z.s1.swiftbase.core.Core
import net.rk4z.s1.swiftbase.core.LanguageManager
import net.rk4z.s1.swiftbase.core.MessageKey
import net.rk4z.s1.swiftbase.core.SystemHelper
import org.jetbrains.annotations.NotNull
import org.reflections.Reflections.log
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

@Suppress("DuplicatedCode", "UNCHECKED_CAST", "unused", "MemberVisibilityCanBePrivate")
open class ClientModEntry(
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
    var enableUpdateChecker: Boolean = true,
    val modrinthID: String? = null,
    val availableLang: List<String>? = null,
    val autoLanguageUpdate: Boolean = true,
    val useConfigFile: Boolean = true,
    val useLanguageSystem: Boolean = true
) : ClientModInitializer {
    companion object {
        private lateinit var instance: ClientModEntry

        @JvmStatic
        lateinit var core: Core
            private set

        @JvmStatic
        lateinit var languageManager: LanguageManager<*, *>
            private set

        fun <I : ClientModEntry> get() : I? {
            return instance as? I
        }

        internal fun get() : ClientModEntry? {
            return get<ClientModEntry>()
        }

        val fabricTextComponentFactory = { text: String -> Text.of(text) }
    }

    var onCheckUpdate: () -> Unit = {}
    var onAllVersionsRetrieved: (versionCount: Int) -> Unit = {}
    var onNewVersionFound: (latestVersion: String, newerVersionCount: Int) -> Unit = { _, _ -> }
    var onNoNewVersionFound: () -> Unit = {}
    var onUpdateCheckFailed: (responseCode: Int) -> Unit = {}
    var onUpdateCheckError: (e: Exception) -> Unit = {}

    val loader: FabricLoader = FabricLoader.getInstance()
    var client: MinecraftClient = MinecraftClient.getInstance()
    val dataFolder: File = loader.gameDir.resolve(id).toFile()
    val meta: ModMetadata = loader.getModContainer(id).get().metadata
    val version: String = meta.version.friendlyString

    override fun onInitializeClient()  {
        if (!ModEntry.isInitialized) {
            initSystem()

            if (enableUpdateChecker) {
                onCheckUpdate()
                core.checkUpdate()
            }

            ClientLifecycleEvents.CLIENT_STOPPING.register {
                onClientStoppingPre()

                stopSystem()

                onClientStoppingPost()
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
            S2Executor(),
            configFileRoot,
            langDirRoot
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
                        val messageMap: MutableMap<MessageKey<FabricPlayerAdapter, Text>, String> = mutableMapOf()
                        languageManager.processYamlAndMapMessageKeys(data, messageMap, FabricMessageKey::class)
                        languageManager.messages[lang] = messageMap as MutableMap<FabricMessageKey, String>
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

    open fun onClientStoppingPre() {}
    open fun onClientStoppingPost() {}
}