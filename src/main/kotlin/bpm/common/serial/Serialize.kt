package bpm.common.serial

import bpm.common.logging.KotlinLogging
import bpm.common.memory.*
import kotlin.reflect.KClass

/**
 * A serializer interface for deserializing and serializing objects.
 *
 * @param T The type of objects to be deserialized and serialized.
 */
abstract class Serialize<T : Any>(private val type: KClass<T>) {

    protected val self: Serialize<T> get() = this

    internal fun register() = Serial.register(type.java, self)

    /**
     * Deserializes the contents of the Buffer and returns an instance of type T.
     *
     * @return The deserialized object of type T.
     */
    abstract fun deserialize(buffer: Buffer): T

    /**
     * Serializes the provided value into the buffer.
     *
     * @param value The value to be serialized.
     */
    abstract fun serialize(buffer: Buffer, value: T)

    companion object {

        private val logger = KotlinLogging.logger { }
    }
}