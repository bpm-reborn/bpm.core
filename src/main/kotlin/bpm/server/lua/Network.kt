package bpm.server.lua

import bpm.pipe.PipeNetworkManager
import net.minecraft.core.BlockPos
import java.util.*

object Network : LuaBuiltin {


    @JvmStatic
    fun getControllerPosition(uuid: String): BlockPos? {
        val result = PipeNetworkManager.getControllerPosition(UUID.fromString(uuid))
        return result
    }

    @JvmStatic
    fun getControllerPositions(uuid: String): List<BlockPos> {
        val result = PipeNetworkManager.getControllerPositions(UUID.fromString(uuid))
        return result
    }


    override val name: String = "Network"

}