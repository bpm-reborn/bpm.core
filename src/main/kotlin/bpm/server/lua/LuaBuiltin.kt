package bpm.server.lua

interface LuaBuiltin {

    val name: String get() = this::class.simpleName ?: error("No name for builtin")
}