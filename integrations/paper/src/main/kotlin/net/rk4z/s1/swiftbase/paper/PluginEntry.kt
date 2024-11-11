package net.rk4z.s1.swiftbase.paper

import net.kyori.adventure.text.Component
import net.rk4z.s1.swiftbase.core.Core
import net.rk4z.s1.swiftbase.core.LanguageManager
import net.rk4z.s1.swiftbase.core.SystemHelper
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.NotNull
import org.slf4j.LoggerFactory

@Suppress("unused", "MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
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
        lateinit var instance: PluginEntry
            private set
        lateinit var core: Core
            private set

        fun <I : PluginEntry> get(): I {
            return instance as? I
                ?: throw IllegalStateException("PluginEntry is not properly initialized with the correct types")
        }

        val paperTextComponentFactory = { text: String -> Component.text(text) }
    }

    val version = description.version

    override fun onLoad() {
        instance = getPlugin(this::class.java)
        Core.languageManager = LanguageManager.initialize(paperTextComponentFactory, PaperMessageKey::class)
        Core.logger = LoggerFactory.getLogger(this::class.java.simpleName)
        core = SystemHelper.createCore(
            packageName,
            dataFolder,
            version,
            modrinthID,
            useConfigFile,
            useLanguageSystem,
            isDebug,
            availableLang,
            S1Executor(this),
            configFileRoot,
            langDirRoot,
        )
    }
}