package bpm.server.lua

import bpm.common.network.Listener
import bpm.server.ServerRuntime

object LuaEventExecutor : Listener {


    override fun onTick(delta: Float, tick: Int) = with(ServerRuntime) {
        for (workspace in workspaces.values) {
            if (workspace.needsRecompile) continue
            //Executes the function Tick, every tick. Waits for recompilation if the workspace needs it (there was an error)
            execute(workspace, "Tick")
            //Only if there wasn't an error in the tick function, we call the Redstone function
            execute(workspace, "Redstone")
        }
    }

}