@file:Suppress("DEPRECATION", "unused")

package net.rk4z.s1.swiftbase.fabric

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.rk4z.s1.swiftbase.core.IPlayer
import net.rk4z.s1.swiftbase.core.LanguageManager
import net.rk4z.s1.swiftbase.core.MessageKey
import kotlin.collections.get
import kotlin.reflect.full.isSubclassOf

@Suppress("DEPRECATION")
class FabricPlayer(internal val player: ServerPlayerEntity) : IPlayer<Text> {
    private val languageManager: LanguageManager<FabricPlayer, Text>
        get() {
            //この関数が呼び出される時点でLanguageManagerが初期化されていない場合はエラーを出す
            if (!LanguageManager.isInitialized()) {
                throw IllegalStateException("LanguageManager is not initialized but you are trying to use it.")
            }
            val languageManager = LanguageManager.get<FabricPlayer, Text>()
            return languageManager
        }

    override fun getLanguage(): String {
        // en_US -> en
        return player.clientOptions.comp_1951.split("_")[0]
    }

    override fun getMessage(key: MessageKey<*, *>, vararg args: Any): Text {
        val messages = languageManager.messages
        val expectedMKType = languageManager.expectedMKType
        val textComponentFactory = languageManager.textComponentFactory

        require(key::class.isSubclassOf(expectedMKType)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
        val lang = this.getLanguage()
        val message = messages[lang]?.get(key)
        val text = message?.let { String.format(it, *args) } ?: key.rc()
        return textComponentFactory(text)
    }

    override fun getRawMessage(key: MessageKey<*, *>): String {
        val messages = languageManager.messages
        val expectedMKType = languageManager.expectedMKType

        require(key::class.isSubclassOf(expectedMKType)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
        val lang = this.getLanguage()
        return messages[lang]?.get(key) ?: key.rc()
    }

    override fun hasMessage(key: MessageKey<*, *>): Boolean {
        val messages = languageManager.messages
        val expectedMKType = languageManager.expectedMKType

        require(key::class.isSubclassOf(expectedMKType)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
        val lang = this.getLanguage()
        return messages[lang]?.containsKey(key) ?: false
    }
}

fun ServerPlayerEntity.adapt(): FabricPlayer {
    return FabricPlayer(this)
}

fun FabricPlayer.getAPlayer(): ServerPlayerEntity {
    return this.player
}