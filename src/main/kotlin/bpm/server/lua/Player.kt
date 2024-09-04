package bpm.server.lua

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.server.ServerLifecycleHooks
import java.util.UUID

object Player : LuaBuiltin {
    override val name: String = "Player"

    private val server: MinecraftServer by lazy {
        ServerLifecycleHooks.getCurrentServer() ?: throw IllegalStateException("Server not available")
    }


    @JvmStatic
    fun getPlayers(): List<ServerPlayer> {
        return server.playerList.players
    }

    @JvmStatic
    fun getPlayer(uuid: UUID): ServerPlayer? {
        return server.playerList.getPlayer(uuid)
    }


}