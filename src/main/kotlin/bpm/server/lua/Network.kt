package bpm.server.lua

import bpm.pipe.PipeNetManager
import net.minecraft.core.BlockPos
import java.util.*

object Network : LuaBuiltin {


    @JvmStatic
    fun getControllerPosition(uuid: String): BlockPos? {
        val uuid = UUID.fromString(uuid)
        val result = PipeNetManager.getControllerPosition(uuid)
        return result
    }


    override val name: String = "Network"

}