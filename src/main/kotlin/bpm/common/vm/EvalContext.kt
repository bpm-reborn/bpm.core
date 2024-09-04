package bpm.common.vm

import bpm.common.logging.KotlinLogging
import bpm.common.workspace.Workspace
import party.iroiro.luajava.*
import party.iroiro.luajava.luajit.LuaJit
import party.iroiro.luajava.value.LuaValue
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object EvalContext {

    private val logger = KotlinLogging.logger {}
    private lateinit var lua: Lua
    //    private val functionGroups: ConcurrentHashMap<String, MutableMap<String, LuaValue>> = ConcurrentHashMap()
    private val workspaceFunctionGroups: ConcurrentHashMap<UUID, ConcurrentHashMap<String, MutableMap<String, LuaValue>>> = ConcurrentHashMap()

    init {
        initializeLuaJit()
    }

    private fun initializeLuaJit() {
        try {
            lua = LuaJit()
            initializeLuaState()
        } catch (e: Exception) {
            println("Error loading LuaJit: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun initializeLuaState() {
        lua.openLibraries()
        lua.setExternalLoader(ClassPathLoader())
    }

    fun eval(workspace: Workspace): Result = synchronized(lua){
        val functionGroups = workspaceFunctionGroups.computeIfAbsent(workspace.uid) { ConcurrentHashMap() }
        functionGroups.clear()
        try {
            lua.gc()
            val compiledSource = ComplexLuaTranspiler.generateLuaScript(workspace)
            logger.debug { "Compiled Lua script: $compiledSource" }
            val result = lua.eval(compiledSource)[0]
            if (result.type() == Lua.LuaType.TABLE) {
                for (groupKey in result.keys) {
                    val group = result.get(groupKey)
                    if (group?.type() == Lua.LuaType.TABLE) {
                        val groupName = groupKey.toString()
                        functionGroups[groupName] = mutableMapOf()
                        for (functionKey in group.keys) {
                            val function = group.get(functionKey)
                            if (function?.type() == Lua.LuaType.FUNCTION) {
                                val functionName = functionKey.toString()
                                functionGroups[groupName]!![functionName] = function
                                logger.debug { "Added function `$functionName` to group `$groupName`" }
                            }
                        }
                    }
                }
            }
        } catch (e: LuaException) {
            return RuntimeError(e.message ?: "Unknown error", e.stackTrace)
        }
        return Success("Workspace evaluated successfully")
    }


    data class Success(val value: Any?, private val functionName: String? = null) : Result() {

        override val message: String
            get() = if (functionName != null) "Success in function `$functionName`: $result" else "Success: $result"
    }

    abstract class Failure(val error: String, private val functionName: String? = null) : Result() {

        override val message: String get() = if (functionName != null) "Error in function `$functionName`: $error" else "Error: $error"
    }

    data class RuntimeError(
        val errorMessage: String, val stackTrace: Array<StackTraceElement>, private val func: String? = null
    ) : Failure(errorMessage, func) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RuntimeError) return false

            if (errorMessage != other.errorMessage) return false
            if (!stackTrace.contentEquals(other.stackTrace)) return false
            if (func != other.func) return false

            return true
        }

        override fun hashCode(): Int {
            var result = errorMessage.hashCode()
            result = 31 * result + stackTrace.contentHashCode()
            result = 31 * result + (func?.hashCode() ?: 0)
            return result
        }
    }

    data class InvalidWorkspace(val workspaceId: UUID, private val func: String? = null) :
        Failure("Invalid workspace with id `$workspaceId`!", func)

    data class GroupNotFound(val groupName: String, private val func: String? = null) :
        Failure("Group `$groupName` not found!", func)

    data class GroupResult(val groupName: String, val results: List<Result>) : Result() {

        override val message: String
            get() = "Group `$groupName` results: ${results.joinToString { "${it.message}\n" }}"
    }

    sealed class Result {

        abstract val message: String
        val isSuccess: Boolean
            get() = this is Success

        val isFailure: Boolean
            get() = this is Failure

        val isRealFailure: Boolean
            get() = this is Failure && !isNotFound && !isWorkspaceInvalid

        val hasFailure: Boolean
            get() = when (this) {
                is Failure -> true
                is GroupResult -> results.any { it.hasFailure }
                else -> false
            }

        val result: Any?
            get() = if (this is Success) this.value else null

        val successes: List<Success>
            get() = when (this) {
                is Success -> listOf(this)
                is GroupResult -> results.filterIsInstance<Success>()
                else -> emptyList()
            }

        val failures: List<Failure>
            get() = when (this) {
                is Failure -> listOf(this)
                is GroupResult -> results.filterIsInstance<Failure>()
                else -> emptyList()
            }

        //True when all failures are GroupNotFound
        val isNotFound get() = failures.all { it is GroupNotFound }

        //True when all failures are InvalidWorkspace
        val isWorkspaceInvalid get() = failures.all { it is InvalidWorkspace }


        override fun toString(): String = message
    }


    fun callFunction(workspace: Workspace, groupName: String, vararg args: Any?): Result {
        val functionGroups = workspaceFunctionGroups[workspace.uid] ?: return InvalidWorkspace(
            workspace.uid
        )
        val group = functionGroups[groupName] ?: return GroupNotFound(groupName)
        val results = mutableSetOf<Result>()
        for ((functionName, function) in group) {
            try {
                val result = function.call(*args)
                results.add(Success(result, functionName))
            } catch (e: LuaException) {
                results.add(RuntimeError(e.message ?: "Unknown error", e.stackTrace, functionName))
            }
        }
        return GroupResult(groupName, results.toList())
    }

    fun callAllFunctions(workspace: Workspace, vararg args: Any?): Result {
        val functionGroups = workspaceFunctionGroups[workspace.uid] ?: return InvalidWorkspace(
            workspace.uid
        )
        val results = mutableSetOf<Result>()
        for ((groupName, group) in functionGroups) {
            results.add(callFunction(workspace, groupName, *args))
        }
        return GroupResult("All", results.toList())
    }

    fun close() {
        lua.close()
    }
}