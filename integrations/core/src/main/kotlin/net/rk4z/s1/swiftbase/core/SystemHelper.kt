package net.rk4z.s1.swiftbase.core


import java.io.File

//TODO: createCore関数のKDocを細かく書く
object SystemHelper {

    /**
     * Create a new core system.
     * @see Core
     */
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
    ): Core {
        if (Core.isInitialized()) throw IllegalStateException("Core is already initialized.")

        return Core(
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
}