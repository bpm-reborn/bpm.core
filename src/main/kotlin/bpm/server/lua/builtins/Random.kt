package bpm.server.lua.builtins

import bpm.server.lua.LuaBuiltin

object Random : LuaBuiltin {
    override val name: String = "Random"

    @JvmStatic
    fun nextInt(): Int {
        return kotlin.random.Random.nextInt()
    }

    @JvmStatic
    fun nextInt(bound: Int): Int {
        return kotlin.random.Random.nextInt(bound)
    }

    @JvmStatic
    fun nextInt(origin: Int, bound: Int): Int {
        return kotlin.random.Random.nextInt(origin, bound)
    }

    @JvmStatic
    fun nextLong(): Long {
        return kotlin.random.Random.nextLong()
    }

    @JvmStatic
    fun nextLong(bound: Long): Long {
        return kotlin.random.Random.nextLong(bound)
    }

    @JvmStatic
    fun nextLong(origin: Long, bound: Long): Long {
        return kotlin.random.Random.nextLong(origin, bound)
    }

    @JvmStatic
    fun nextDouble(): Double {
        return kotlin.random.Random.nextDouble()
    }

    @JvmStatic
    fun nextDouble(bound: Double): Double {
        return kotlin.random.Random.nextDouble(bound)
    }

    @JvmStatic
    fun nextDouble(origin: Float, bound: Float): Float {
        return kotlin.random.Random.nextFloat() * (bound - origin) + origin
    }

    @JvmStatic
    fun nextBoolean(): Boolean {
        return kotlin.random.Random.nextBoolean()
    }

}