package net.rk4z.s1.swiftbase.core

import java.io.File

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
        langDirRoot: String
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
            langDirRoot
        )
    }

    fun <P : IPlayer, T> createLanguageManager(textComponentFactory: (String) -> T): LanguageManager<P, T> {
        return LanguageManager.initialize(textComponentFactory)
    }
}