package bpm.common.memory

import bpm.common.serial.Serial
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import org.joml.*
import java.util.*
import kotlin.reflect.KClass

inline fun <reified T : Any> Buffer.writeList(list: List<T>) {
    writeInt(list.size)
    list.forEach {
        if (!Serial.has<T>()) writeAny(it)
        else Serial.write(this, it) ?: error("Failed to write list element")
    }
}

inline fun <reified T : Any> Buffer.readList(): List<T> = readList(T::class)
fun <T : Any> Buffer.readList(clazz: KClass<T>): List<T> {
    val list = mutableListOf<T>()
    repeat(readInt()) {
        if (!Serial.has(clazz.java)) list.add(readAny(clazz))
        else list.add(Serial.read(clazz, this) ?: error("Failed to read list element"))
    }
    return list
}

inline fun <reified T : Any> Buffer.writeSet(set: Set<T>) {
    writeInt(set.size)
    set.forEach {
        if (!Serial.has<T>()) writeAny(it)
        else Serial.write(this, it) ?: error("Failed to write set element")
    }
}

inline fun <reified T : Any> Buffer.readSet(): Set<T> = readSet(T::class)
fun <T : Any> Buffer.readSet(clazz: KClass<T>): Set<T> {
    val set = mutableSetOf<T>()
    repeat(readInt()) {
        if (!Serial.has(clazz.java)) set.add(readAny(clazz))
        else set.add(Serial.read(clazz, this) ?: error("Failed to read set element"))
    }
    return set
}


fun <T : Any> Buffer.writeAny(value: T) {
    when (value) {
        is Int -> writeInt(value)
        is Float -> writeFloat(value)
        is Double -> writeDouble(value)
        is String -> writeString(value)
        is Boolean -> writeBoolean(value)
        is Byte -> writeByte(value)
        is BlockPos -> writeBlockPos(value)
        is ByteArray -> writeBytes(value)
        is Char -> writeChar(value)
        is Class<*> -> writeClass(value)
        is Enum<*> -> writeEnum(value)
        is Long -> writeLong(value)
        is ResourceKey<*> -> writeResourceKey(value)
        is ResourceLocation -> writeResourceLocation(value)
        is Short -> writeShort(value)
        is UUID -> writeUUID(value)
        is Vector2f -> writeVector2f(value)
        is Vector2i -> writeVector2i(value)
        is Vector3d -> writeVector3d(value)
        is Vector3f -> writeVector3f(value)
        is Vector3i -> writeVector3i(value)
        is Vector4i -> writeVector4i(value)
        is Vector4f -> writeVector4f(value)
        is Vector4d -> writeVector4d(value)
        is List<*> -> writeList(value as List<Any>)
        is Set<*> -> writeSet(value as Set<Any>)
        else -> error("Unsupported buffer argument type ${value::class.qualifiedName}")
    }
}

inline fun <reified T : Any> Buffer.readAny(): T = readAny(T::class)
fun <T : Any> Buffer.readAny(clazz: KClass<T>): T {
    val value = when (clazz) {
        Int::class -> readInt()
        Float::class -> readFloat()
        Double::class -> readDouble()
        String::class -> readString()
        Boolean::class -> readBoolean()
        Byte::class -> readByte()
        BlockPos::class -> readBlockPos()
        ByteArray::class -> readBytes()
        Char::class -> readChar()
        Class::class -> readClass()
        Enum::class -> readEnum(clazz.java as Class<out Enum<*>>)
        Long::class -> readLong()
        ResourceKey::class -> readResourceKey<T>()
        ResourceLocation::class -> readResourceLocation()
        Short::class -> readShort()
        UUID::class -> readUUID()
        Vector2f::class -> readVector2f()
        Vector2i::class -> readVector2i()
        Vector3d::class -> readVector3d()
        Vector3f::class -> readVector3f()
        Vector3i::class -> readVector3i()
        Vector4i::class -> readVector4i()
        Vector4f::class -> readVector4f()
        Vector4d::class -> readVector4d()
        List::class -> readList(clazz)
        Set::class -> readSet(clazz)
        else -> error("Unsupported buffer argument type ${clazz.qualifiedName}")
    }
    return value as T
}
