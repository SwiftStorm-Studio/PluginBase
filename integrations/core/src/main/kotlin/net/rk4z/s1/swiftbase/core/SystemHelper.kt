package net.rk4z.s1.swiftbase.core

import org.slf4j.Logger
import java.io.File
import kotlin.reflect.KClass

object SystemHelper {
    fun createCore(
        packageName: String,
        dataFolder: File,
        version: String,
        modrinthID: String? = null,
        useConfigFile: Boolean = true,
        useLanguageSystem: Boolean = true,
        isDebug: Boolean = false,
        availableLang: List<String>? = null,
        executor: S0Executor,
        configFileRoot: String,
        langDirRoot: String,
        pl: Logger
    ): Core {
        return Core.initialize(
            packageName,
            dataFolder,
            version,
            modrinthID,
            useConfigFile,
            useLanguageSystem,
            isDebug,
            availableLang,
            executor,
            configFileRoot,
            langDirRoot,
            pl
        )
    }

    fun <P : IPlayer, T> createLanguageManager(textComponentFactory: (String) -> T, expectedType: KClass<out MessageKey<P, T>>): LanguageManager<P, T> {
        return LanguageManager.initialize(textComponentFactory, expectedType)
    }
}