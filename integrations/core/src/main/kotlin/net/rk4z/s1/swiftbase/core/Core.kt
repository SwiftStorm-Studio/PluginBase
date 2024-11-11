package net.rk4z.s1.swiftbase.core

import org.json.JSONArray
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

@Suppress("unused", "MemberVisibilityCanBePrivate", "CanBeParameter", "PropertyName")
class Core private constructor(
    val packageName: String,
    val dataFolder: File,
    val version: String,
    val modrinthID: String? = null,
    val useConfigFile: Boolean = true,
    val useLanguageSystem: Boolean = true,
    val isDebug: Boolean = false,
    val availableLang: List<String>? = null,
    val executor: S0Executor,
    /**
     * The root directory of the config files in the JAR.
     * This directory should contain the default config file for the plugin.
     * The default config file should be named as the default language code (e.g. `en.yml`).
     */
    val configFileRoot: String,
    /**
     * The root directory of the language files in the JAR.
     * This directory should contain the default language files for the plugin.
     * The default language files should be named as the language code (e.g. `en.yml`).
     */
    val langDirRoot: String,
    val pl: Logger
) {
    companion object {
        private lateinit var instance: Core

        internal val logger: Logger = LoggerFactory.getLogger("SwiftBase")

        var platformLogger: Logger = LoggerFactory.getLogger("")

        internal fun initialize(
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
            logger: Logger
        ): Core {
            if (::instance.isInitialized) {
                throw IllegalStateException("Core instance is already initialized.")
            }

            instance = Core(
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
                logger
            )
            return instance
        }

        internal fun get(): Core {
            if (!::instance.isInitialized) {
                throw IllegalStateException("Core instance is not initialized.")
            }

            return instance
        }
    }

    val MODRINTH_API_URL = "https://api.modrinth.com/v2/project/${modrinthID}/version"
    val MODRINTH_DOWNLOAD_URL = "https://modrinth.com/plugin/${modrinthID}/versions/"

    val helper = ResourceHelper(dataFolder)
    val yaml = Yaml()
    val configFile = dataFolder.resolve("config.yml")
    val langDir = dataFolder.resolve("lang")

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
        val config: Map<String, Any> = Files.newInputStream(configFile.toPath()).use { yaml.load(it) }
        val value = config[key]
        return parseValue(value)
    }

    inline fun <reified T> parseValue(value: Any?): T? {
        return when (T::class) {
            String::class -> value as? T
            Int::class -> value?.toString()?.toIntOrNull() as? T
            Boolean::class -> value?.toString()?.toBooleanOrNull() as? T
            Double::class -> value?.toString()?.toDoubleOrNull() as? T
            Short::class -> value?.toString()?.toShortOrNull() as? T
            Long::class -> value?.toString()?.toLongOrNull() as? T
            Float::class -> value?.toString()?.toFloatOrNull() as? T
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
        if (dataFolder.toPath().notExists()) dataFolder.mkdirs()
        if (useConfigFile) createConfigIfNotExists()
        if (useLanguageSystem) initializeLanguageFiles()
    }

    fun updateLanguageFilesIfNeeded() {
        availableLang?.let {
            it.forEach { lang ->
                val langFile = File(langDir, "$lang.yml")
                val langResource = "$langDirRoot/$lang.yml"

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
                        logger.info("Replacing old $lang language file (version: $installedLangVersion) with newer version: $jarLangVersion")
                        resourceBytes.inputStream().use { byteArrayStream ->
                            Files.copy(
                                byteArrayStream,
                                langFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        }
                    }
                } ?: logger.warn("Resource file '$langResource' not found in the Jar.")
            }
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

    private fun createConfigIfNotExists() {
        val defaultConfigLang = Locale.getDefault().language
        val configResource = "$configFileRoot/${defaultConfigLang}.yml"
        helper.getResource(configResource)?.use { inputStream ->
            val targetConfigFile = File(dataFolder, "config.yml")
            if (!targetConfigFile.exists()) Files.copy(inputStream, targetConfigFile.toPath())
        }
    }

    private fun initializeLanguageFiles() {
        if (!langDir.exists()) langDir.mkdirs()
        availableLang?.forEach { lang ->
            langDir.resolve("$lang.yml").apply {
                if (this.toPath().notExists()) helper.saveResource("$langDirRoot/$lang.yml", false, langDir.absolutePath)
            }
        }
    }

    private fun readLangVersion(stream: InputStream): String {
        return InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
            val langData: Map<String, Any> = yaml.load(reader)
            langData["langVersion"]?.toString() ?: "0"
        }
    }

    private fun createConnection(): HttpURLConnection {
        return URI(MODRINTH_API_URL).toURL().openConnection() as HttpURLConnection
    }

    private fun extractVersionInfo(response: String): Triple<String, Int, Int> {
        val jsonArray = JSONArray(response)
        var latestVersion = ""
        var latestDate = ""
        val versionCount = jsonArray.length()
        var newerVersionCount = 0

        for (i in 0 until jsonArray.length()) {
            val versionObject = jsonArray.getJSONObject(i)
            val versionNumber = versionObject.getString("version_number")
            val releaseDate = versionObject.getString("date_published")

            if (isVersionNewer(versionNumber, version)) newerVersionCount++

            if (releaseDate > latestDate) {
                latestDate = releaseDate
                latestVersion = versionNumber
            }
        }
        return Triple(latestVersion, versionCount, newerVersionCount)
    }

    private fun isVersionNewer(version1: String, version2: String): Boolean {
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
