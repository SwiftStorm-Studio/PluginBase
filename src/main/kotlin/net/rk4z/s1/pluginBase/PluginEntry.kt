package net.rk4z.s1.pluginBase

import net.rk4z.s1.pluginBase.bstats.Metrics
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.NotNull
import org.json.JSONArray
import org.reflections.Reflections.log
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.notExists

@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
abstract class PluginEntry(
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
    val autoLanguageUpdate: Boolean = true
) : JavaPlugin() {
    companion object {
        private lateinit var metrics: Metrics

        /**
         * Instance of the logger for the plugin. This is initialized later and is accessible only within
         * this class or its subclasses.
         *
         * @property logger The instance of [Logger] used for logging messages.
         */
        @JvmStatic
        lateinit var logger: Logger
            private set

        /**
         * The key used for storing data in the plugin's namespace. This is initialized later and is accessible
         * only within this class or its subclasses.
         *
         * @property key The [NamespacedKey] used for storing data in the plugin's namespace.
         */
        @JvmStatic
        lateinit var key: NamespacedKey
            private set

        /**
         * The executor used for running asynchronous tasks. This is initialized later and is accessible only
         * within this class or its subclasses.
         *
         * @property executor The [S1Executor] used for running asynchronous tasks.
         */
        @JvmStatic
        lateinit var executor: S1Executor
            private set

        /**
         * The Java helper instance used for registering Java event listeners. This is initialized later and is
         * accessible only within this class or its subclasses.
         *
         * @property jHelper The [PluginEntryJavaHelper] used for registering Java event listeners.
         */
        @JvmStatic
        lateinit var jHelper: PluginEntryJavaHelper
            private set

        /**
         * Singleton instance of the plugin. This holds the current instance of the plugin, which is set
         * when the plugin is loaded and cleared when the plugin is disabled.
         *
         * @property instance The singleton instance of [PluginEntry], or `null` if the plugin has not been initialized.
         */
        private var instance: PluginEntry? = null

        /**
         * Retrieves the singleton instance of the plugin. This method uses generics to return the plugin
         * as the specified type. It will throw an [IllegalStateException] if the plugin instance has not
         * been initialized (e.g., if called before the plugin is loaded).
         *
         * Example usage:
         * ```kotlin
         * val pluginInstance = MyPlugin.get<MyPlugin>()
         * ```
         *
         * @throws IllegalStateException If the plugin instance has not been initialized.
         * @return The singleton instance of the plugin, cast to the specified type [I].
         */
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <I : PluginEntry> get(): I {
            return instance as? I ?: throw IllegalStateException("Plugin instance not initialized")
        }

        /**
         * Internal method for retrieving the singleton instance of the plugin. This method returns
         * the instance as a [PluginEntry] type and is intended for internal use where the plugin
         * type does not need to be specified explicitly.
         *
         * @throws IllegalStateException If the plugin instance has not been initialized.
         * @return The singleton instance of the plugin as a [PluginEntry].
         */
        internal fun get(): PluginEntry {
            return get<PluginEntry>()
        }
    }

    @Suppress("PropertyName")
    val MODRINTH_API_URL = "https://api.modrinth.com/v2/project/${modrinthID}/version"
    @Suppress("PropertyName")
    val MODRINTH_DOWNLOAD_URL = "https://modrinth.com/plugin/${modrinthID}/versions/"

    val yaml = Yaml()
    val configFile = dataFolder.resolve("config.yml")
    private val langDir = dataFolder.resolve("lang")

    @Deprecated("Do not override this method. Use event system instead.")
    override fun onLoad() {
        instance = getPlugin(this::class.java)
        jHelper = PluginEntryJavaHelper(this)
        key = NamespacedKey(this, id)
        executor = S1Executor(get())
        Companion.logger = LoggerFactory.getLogger(this::class.java.simpleName)

        onLoadPre()

        initializeDirectories()
        if (!isDebug) {
            updateLanguageFilesIfNeeded()
        }
        loadLanguageFiles()

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
            onCheckUpdate()
            checkUpdate()
        }

        onEnablePost()
    }

    @Deprecated("Do not override this method. Use event system instead.")
    override fun onDisable() {
        onDisablePre()

        executor.shutdown()

        onDisablePost()
    }

    fun String.toBooleanOrNull(): Boolean? {
        return when (this.trim().lowercase()) {
            "true", "1", "t" -> true
            "false", "0", "f" -> false
            else -> null
        }
    }

    inline fun <reified T> lc(key: String): T? {
        val config: Map<String, Any> = Files.newInputStream(configFile.toPath()).use { inputStream ->
            yaml.load(inputStream)
        }

        val value = config[key]

        return when (T::class) {
            String::class -> value as? T
            Int::class -> value?.toString()?.toIntOrNull() as? T
            Boolean::class -> value?.toString()?.toBooleanOrNull() as? T
            Double::class -> value?.toString()?.toDoubleOrNull() as? T
            Short::class -> value?.toString()?.toShortOrNull() as? T
            Long::class -> value?.toString()?.toLongOrNull() as? T
            Float::class -> value?.toString()?.toFloatOrNull() as? T

            // ** More specialized types are here :) **

            Byte::class -> value?.toString()?.toByteOrNull() as? T
            Char::class -> (value as? String)?.singleOrNull() as? T
            List::class -> value as? List<*> as? T
            Array::class -> (value as? List<*>)?.toTypedArray() as? T
            Map::class -> value as? Map<*, *> as? T
            BigInteger::class -> (value?.toString())?.let { BigInteger(it) } as? T
            BigDecimal::class -> (value?.toString())?.let { BigDecimal(it) } as? T
            else -> value as? T
        }
    }

    protected fun initializeDirectories() {
        if (dataFolder.toPath().notExists()) {
            dataFolder.mkdirs()
        }
        saveDefaultConfig()
        if (!langDir.exists()) {
            langDir.mkdirs()
        }
        availableLang?.let {
            it.forEach { s ->
                val langFile = langDir.resolve("$s.yml")
                if (langFile.toPath().notExists()) {
                    saveResource("lang/$s.yml", false)
                }
            }
        }
    }

    protected fun updateLanguageFilesIfNeeded() {
        if (autoLanguageUpdate) {
            availableLang?.let {
                it.forEach { lang ->
                    val langFile = File(langDir, "$lang.yml")
                    val langResource = "lang/$lang.yml"

                    getResource(langResource)?.use { resourceStream ->
                        val resourceBytes = resourceStream.readBytes()

                        val jarLangVersion = readLangVersion(resourceBytes.inputStream())
                        val installedLangVersion = if (langFile.exists()) {
                            Files.newInputStream(langFile.toPath()).use { inputStream ->
                                readLangVersion(inputStream)
                            }
                        } else {
                            "0"
                        }

                        if (isVersionNewer(jarLangVersion, installedLangVersion)) {
                            log.info("Replacing old $lang language file (version: $installedLangVersion) with newer version: $jarLangVersion")
                            resourceBytes.inputStream().use { byteArrayStream ->
                                Files.copy(
                                    byteArrayStream,
                                    langFile.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING
                                )
                            }
                        }
                    } ?: log.warn("Resource file '$langResource' not found in the Jar.")
                }
            }
        }
    }

    protected fun loadLanguageFiles() {
        availableLang?.let {
            it.forEach { lang ->
                val langFile = langDir.resolve("$lang.yml")
                if (Files.exists(langFile.toPath())) {
                    Files.newBufferedReader(langFile.toPath(), StandardCharsets.UTF_8).use { reader ->
                        val data: Map<String, Any> = yaml.load(reader)
                        val messageMap: MutableMap<MessageKey, String> = mutableMapOf()
                        LanguageManager.processYamlAndMapMessageKeys(data, messageMap)
                        LanguageManager.messages[lang] = messageMap
                    }
                } else {
                    log.warn("Language file for '$lang' not found.")
                }
            }
        }
    }

    protected fun readLangVersion(stream: InputStream): String {
        return InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
            val langData: Map<String, Any> = yaml.load(reader)
            langData["langVersion"]?.toString() ?: "0"
        }
    }

    protected fun checkUpdate() {
        try {
            val connection = createConnection()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = executor.submit { connection.inputStream.bufferedReader().readText() }.get()
                val (latestVersion, versionCount, newerVersionCount) = extractVersionInfo(response)

                onAllVersionsRetrieved(versionCount)
                if (isVersionNewer(latestVersion, description.version)) {
                    onNewVersionFound(latestVersion, newerVersionCount)
                } else {
                    onNoNewVersionFound()
                }
            } else {
                onUpdateCheckFailed(connection.responseCode)
            }
        } catch (e: Exception) {
            onUpdateCheckError(e)
        }
    }

    protected fun createConnection(): HttpURLConnection {
        val url = URI(MODRINTH_API_URL).toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        return connection
    }

    protected fun extractVersionInfo(response: String): Triple<String, Int, Int> {
        val jsonArray = JSONArray(response)
        var latestVersion = ""
        var latestDate = ""
        val versionCount = jsonArray.length()
        var newerVersionCount = 0

        for (i in 0 until jsonArray.length()) {
            val versionObject = jsonArray.getJSONObject(i)
            val versionNumber = versionObject.getString("version_number")
            val releaseDate = versionObject.getString("date_published")

            if (isVersionNewer(versionNumber, description.version)) {
                newerVersionCount++
            }

            if (releaseDate > latestDate) {
                latestDate = releaseDate
                latestVersion = versionNumber
            }
        }

        return Triple(latestVersion, versionCount, newerVersionCount)
    }

    protected fun isVersionNewer(version1: String, version2: String): Boolean {
        val v1Parts = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = version2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(v1Parts.size, v2Parts.size)
        val v1Padded = v1Parts + List(maxLength - v1Parts.size) { 0 }
        val v2Padded = v2Parts + List(maxLength - v2Parts.size) { 0 }

        for (i in 0 until maxLength) {
            if (v1Padded[i] > v2Padded[i]) return true
            if (v1Padded[i] < v2Padded[i]) return false
        }

        return false
    }

    // You can override these methods to handle the update check results
    open fun onLoadPre() {}
    open fun onLoadPost() {}
    open fun onEnablePre() {}
    open fun onEnablePost() {}
    open fun onDisablePre() {}
    open fun onDisablePost() {}

    open fun onCheckUpdate() {}
    open fun onAllVersionsRetrieved(versionCount: Int) {}
    open fun onNewVersionFound(latestVersion: String, newerVersionCount: Int) {}
    open fun onNoNewVersionFound() {}
    open fun onUpdateCheckFailed(responseCode: Int) {}
    open fun onUpdateCheckError(e: Exception) {}
}