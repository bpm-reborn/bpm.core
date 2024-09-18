package bpm.server.lua

import bpm.common.logging.KotlinLogging
import bpm.common.vm.EvalContext
import bpm.common.workspace.Workspace
import party.iroiro.luajava.ClassPathLoader
import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJit
import party.iroiro.luajava.value.LuaValue
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object LuaManager {

    private val logger = KotlinLogging.logger {}
    private val mainLuaState: Lua = LuaJit()
    private val workspaceContexts = ConcurrentHashMap<UUID, WorkspaceLuaContext>()

    init {
        initializeMainLuaState()
    }

    private fun initializeMainLuaState() {
        mainLuaState.openLibraries()
        mainLuaState.setExternalLoader(ClassPathLoader())
    }

    fun getOrCreateContext(workspace: Workspace): WorkspaceLuaContext {
        return workspaceContexts.computeIfAbsent(workspace.uid) {
            WorkspaceLuaContext(workspace, mainLuaState)
        }
    }

    fun removeContext(workspaceUid: UUID) {
        workspaceContexts.remove(workspaceUid)?.close()
    }

    fun execute(workspace: Workspace, groupName: String, vararg args: Any?): List<EvalContext.Result> {
        return getOrCreateContext(workspace).execute(groupName, *args)
    }

    fun compileWorkspace(workspace: Workspace) {
        getOrCreateContext(workspace).compile()
    }

    fun close() {
        workspaceContexts.values.forEach { it.close() }
        workspaceContexts.clear()
        mainLuaState.close()
    }
}
