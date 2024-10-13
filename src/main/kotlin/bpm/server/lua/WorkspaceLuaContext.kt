package bpm.server.lua

import bpm.common.bootstrap.BpmIO
import bpm.common.logging.KotlinLogging
import bpm.common.vm.LuaTranspiler
import bpm.common.vm.EvalContext
import bpm.common.workspace.Workspace
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.RefLuaValue
import java.util.concurrent.ConcurrentHashMap

class WorkspaceLuaContext(val workspace: Workspace, private val luaThread: Lua) {

    private val logger = KotlinLogging.logger {}
    private val functionGroups = ConcurrentHashMap<String, MutableList<RefLuaValue>>()

    private fun cleanup() {
        //Iterate over all the function groups and remove all the functions
        val refs = functionGroups.values.flatten().map { it.reference }
        refs.forEach(luaThread::unref)
        functionGroups.clear()
    }

    fun compile() {
        cleanup()
        BpmIO.saveWorkspace(workspace)
        workspace.needsRecompile = false
        val compiledSource = LuaTranspiler.generateLuaScript(workspace)

        try {
            val result = luaThread.eval(compiledSource)[0]
            if (result.type() == Lua.LuaType.TABLE) {
                for (groupKey in result.keys) {
                    val group = result.get(groupKey)
                    if (group?.type() == Lua.LuaType.TABLE) {
                        val groupName = groupKey.toString()
                        functionGroups[groupName] = mutableListOf()
                        for (functionKey in group.keys) {
                            val function = group.get(functionKey)
                            if (function?.type() == Lua.LuaType.FUNCTION) {
                                functionGroups[groupName]?.add(function as RefLuaValue)
                                logger.debug { "Added function `${functionKey}` to group `$groupName` for workspace ${workspace.uid}" }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error compiling workspace ${workspace.uid}" }
            workspace.needsRecompile = true
            throw RuntimeException("Failed to compile workspace ${workspace.uid}", e)
        }
    }

    fun execute(groupName: String, vararg args: Any?): List<EvalContext.Result> {
        val group = functionGroups[groupName] ?: return listOf(EvalContext.GroupNotFound(groupName))
        return group.map { function ->
            try {
                val result = function.call(args)
                EvalContext.Success(result)
            } catch (e: Exception) {
                EvalContext.RuntimeError(e.message ?: "Unknown error", e.stackTrace)
            }
        }
    }

    fun close() {
        // We don't close the luaThread here because it's managed by the main Lua state
        functionGroups.clear()
    }
}