package net.rk4z.s1.swiftbase.fabric

import net.minecraft.text.Text
import net.rk4z.s1.swiftbase.core.CB
import net.rk4z.s1.swiftbase.core.Core
import net.rk4z.s1.swiftbase.fabric.ClientModEntry.Companion.instance
import java.nio.file.Path

object Common {
    internal fun initSystem(
        packageName: String,
        isDebug: Boolean,
        dataFolder: Path,
        configFile: String?,
        configResourceRoot: String,
        availableLang: List<String>?,
        langDir: String?,
        langResourceRoot: String,
        logger: org.slf4j.Logger,
        enableUpdateChecker: Boolean,
        modrinthID: String,
        version: String,
        languageManagerInfo: net.rk4z.s1.swiftbase.core.LanguageManagerInfo<FabricPlayer, Text>?,
        onInstanceInitialized: () -> Unit = {},
        onDirectoriesAndFilesInitialized: () -> Unit = {},
        onInitialized: () -> Unit = {}
    ) {
        Core.initialize<FabricPlayer, Text>(
            packageName,
            isDebug,
            dataFolder.toFile(),
            configFile,
            configResourceRoot,
            availableLang,
            langDir,
            langResourceRoot,
            S2Executor(),
            logger,
            modrinthID,
            version,
            languageManagerInfo
        )

        onInstanceInitialized()

        CB.initializeDirectories()
        if (languageManagerInfo != null) {
            if (!isDebug) {
                CB.updateLanguageFilesIfNeeded()
            }
            CB.loadLanguageFiles()
        }

        onDirectoriesAndFilesInitialized()

        if (enableUpdateChecker) {
            CB.checkUpdate()
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            CB.executor.shutdown()
        })

        onInitialized()
    }
}