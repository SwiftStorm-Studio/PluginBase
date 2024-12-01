package net.rk4z.s1.swiftbase.fabric

import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModMetadata
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.rk4z.s1.swiftbase.core.CB
import net.rk4z.s1.swiftbase.core.LanguageManagerInfo
import net.rk4z.s1.swiftbase.fabric.Common.initSystem
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import java.nio.file.Path

@Suppress("DuplicatedCode", "UNCHECKED_CAST", "unused")
open class ModEntry(
    @NotNull
    val id: String,
    @NotNull
    val packageName: String,

    val isDebug: Boolean = false,

    val configFile: String? = null,
    val configResourceRoot: String = "assets/${id}/config",

    val languageManagerInfo: LanguageManagerInfo<FabricPlayer, Text>? = LanguageManagerInfo<FabricPlayer, Text>(
        textComponentFactory = fabricTextComponent,
        expectedType = FabricMessageKey::class,
    ),
    val availableLang: List<String>? = null,
    val langDir: String? = null,
    val langResourceRoot: String = "assets/${id}/lang",

    val logger: Logger,

    var enableUpdateChecker: Boolean = true,
    val modrinthID: String = "",
) : ModInitializer {
    companion object {
        lateinit var instance: ModEntry

        fun <I : ModEntry> get() : I? {
            return instance as? I
        }

        internal fun get() : ModEntry? {
            return get<ModEntry>()
        }

        val fabricTextComponent = { text: String -> Text.of(text) }

        fun isInitialized(): Boolean {
            return this::instance.isInitialized
        }
    }

    val loader: FabricLoader = FabricLoader.getInstance()
    var client = if (loader.environmentType == EnvType.CLIENT) MinecraftClient.getInstance() else null
    val gameDir: Path = loader.gameDir.toRealPath()
    val dataFolder: Path = gameDir.resolve(id)
    val description: ModMetadata = loader.getModContainer(id).get().metadata

    override fun onInitialize() {
        initSystem(
            packageName,
            isDebug,
            dataFolder,
            configFile,
            configResourceRoot,
            availableLang,
            langDir,
            langResourceRoot,
            logger,
            enableUpdateChecker,
            modrinthID,
            description.version.friendlyString,
            languageManagerInfo,
            onInstanceInitialized = this::onInstanceInitialized,
            onDirectoriesAndFilesInitialized = this::onDirectoriesAndFilesInitialized,
            onInitialized = this::onInitialized
        )
        instance = this
    }

    // This is a wrapper for the core's lc method
    // (I just don't want to write `core.lc<T>(key)` every time)
    inline fun <reified T> lc(key: String): T? {
        return CB.lc<T>(key)
    }

    fun loadLanguageFileFromResourcePacks() {
        throw NotImplementedError("This method is not implemented for Fabric Mod yet")
    }

    /**
     * This method is called after the instance is initialized.
     * Usually, this method is used for initializing some variables.
     */
    open fun onInstanceInitialized() {}

    /**
     * This method is called after the directories and files are initialized.
     * Usually, this method is used for loading configurations.
     */
    open fun onDirectoriesAndFilesInitialized() {}

    /**
     * This method is called after the initialization is done.
     * Usually, this method is used for registering events, commands, and so on
     */
    open fun onInitialized() {}
}