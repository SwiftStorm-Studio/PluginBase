package net.rk4z.s1.swiftbase.fabric

import net.minecraft.server.network.ServerPlayerEntity
import net.rk4z.s1.swiftbase.core.IPlayer

class FabricPlayerAdapter(private val player: ServerPlayerEntity) : IPlayer {
    override fun getLanguage(): String {
        // This field returns a string like "en_US"
        val lang = player.clientOptions.comp_1951
        // We only want the first part of the string (Language code)
        return lang.substringBefore("_")
    }
}