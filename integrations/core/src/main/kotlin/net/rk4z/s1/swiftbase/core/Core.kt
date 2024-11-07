package net.rk4z.s1.swiftbase.core

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
import java.util.*
import kotlin.io.path.notExists

@Suppress("MemberVisibilityCanBePrivate")
class Core(
    internal val packageName: String,
    internal val dataFolder: File,
    internal val useConfigFile: Boolean = true,
    internal val useLanguageSystem: Boolean = true,
    internal val isDebug: Boolean = false,
    internal val availableLang: List<String>? = null,
    internal val version: String,
    modrinthID: String?
) {
    companion object {
        @JvmStatic
        lateinit var helper: ResourceHelper
            private set

        @JvmStatic
        lateinit var logger: Logger
            private set

        @JvmStatic
        lateinit var executor: S0Executor

        private var instance: Core? = null

        @JvmStatic
        fun get(): Core {
            return instance ?: throw IllegalStateException("Core is not initialized.")
        }
    }

    @Suppress("PropertyName")
    val MODRINTH_API_URL = "https://api.modrinth.com/v2/project/${modrinthID}/version"
    @Suppress("PropertyName")
    val MODRINTH_DOWNLOAD_URL = "https://modrinth.com/plugin/${modrinthID}/versions/"

    val yaml = Yaml()
    val configFile = dataFolder.resolve("config.yml")
    private val langDir = dataFolder.resolve("lang")

    init {
        instance = this
        helper = ResourceHelper(dataFolder)
        logger = LoggerFactory.getLogger("SwiftBase-Core")
    }

    var onCheckUpdate: () -> Unit = {}
    var onAllVersionsRetrieved: (versionCount: Int) -> Unit = {}
    var onNewVersionFound: (latestVersion: String, newerVersionCount: Int) -> Unit = { _, _ -> }
    var onNoNewVersionFound: () -> Unit = {}
    var onUpdateCheckFailed: (responseCode: Int) -> Unit = {}
    var onUpdateCheckError: (e: Exception) -> Unit = {}

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

    fun initializeDirectories() {
        if (dataFolder.toPath().notExists()) {
            dataFolder.mkdirs()
        }
        if (useConfigFile) {
            val lang = Locale.getDefault().language
            val configFileName = "$lang.yml"
            var configInputStream: InputStream? = helper.getResource("config/$configFileName")

            if (configInputStream == null) {
                // We fall back to english if the language file is not found.
                // So, if you want to use a config file, you must provide english config file.
                configInputStream = helper.getResource("config/en.yml")
            }

            if (configInputStream != null) {
                val targetConfigFile = File(dataFolder, "config.yml")

                if (!targetConfigFile.exists()) {
                    Files.copy(configInputStream, targetConfigFile.toPath())
                }

                configInputStream.close()
            } else {
                throw IllegalStateException("'UseConfigFile' setting is enabled but no config file (including default english config) is found.")
            }
        }
        if (useLanguageSystem) {
            if (!langDir.exists()) {
                langDir.mkdirs()
            }
            availableLang?.let {
                it.forEach { s ->
                    val langFile = langDir.resolve("$s.yml")
                    if (langFile.toPath().notExists()) {
                        helper.saveResource("lang/$s.yml", false)
                    }
                }
            }
        }
    }

    fun updateLanguageFilesIfNeeded() {
        availableLang?.let {
            it.forEach { lang ->
                val langFile = File(langDir, "$lang.yml")
                val langResource = "lang/$lang.yml"

                helper.getResource(langResource)?.use { resourceStream ->
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

    fun loadLanguageFiles() {
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

    fun readLangVersion(stream: InputStream): String {
        return InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
            val langData: Map<String, Any> = yaml.load(reader)
            langData["langVersion"]?.toString() ?: "0"
        }
    }

    fun checkUpdate() {
        try {
            val connection = createConnection()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = executor.submit { connection.inputStream.bufferedReader().readText() }.get()
                val (latestVersion, versionCount, newerVersionCount) = extractVersionInfo(response)

                onAllVersionsRetrieved(versionCount)
                if (isVersionNewer(latestVersion, version)) {
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

    fun createConnection(): HttpURLConnection {
        val url = URI(MODRINTH_API_URL).toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        return connection
    }

    fun extractVersionInfo(response: String): Triple<String, Int, Int> {
        val jsonArray = JSONArray(response)
        var latestVersion = ""
        var latestDate = ""
        val versionCount = jsonArray.length()
        var newerVersionCount = 0

        for (i in 0 until jsonArray.length()) {
            val versionObject = jsonArray.getJSONObject(i)
            val versionNumber = versionObject.getString("version_number")
            val releaseDate = versionObject.getString("date_published")

            if (isVersionNewer(versionNumber, version)) {
                newerVersionCount++
            }

            if (releaseDate > latestDate) {
                latestDate = releaseDate
                latestVersion = versionNumber
            }
        }

        return Triple(latestVersion, versionCount, newerVersionCount)
    }

    fun isVersionNewer(version1: String, version2: String): Boolean {
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
}