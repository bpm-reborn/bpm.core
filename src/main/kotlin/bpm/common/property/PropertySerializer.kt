package bpm.common.property

import bpm.common.property.*

import bpm.common.logging.KotlinLogging
import bpm.common.memory.*
import bpm.common.serial.*
import org.joml.*
import java.lang.RuntimeException
import java.util.*
import kotlin.reflect.KClass

abstract class PropertySerializer<T : Property<*>>(type: KClass<T>) : Serialize<T>(type) {


    /**
     * The `Companion` object provides utility methods for reading and writing property objects from buffers,
     * as well as for registering property serializers.
     */
    companion object {

        /**
         * Logger instance for logging purposes.
         *
         * This variable is a private logging instance created using KotlinLogging library.
         * It can be used for logging various messages at different levels: DEBUG, INFO, WARN, ERROR.
         *
         * Usage:
         * ```
         * log.debug("This is a debug message.")
         * log.info("This is an info message.")
         * log.warn("This is a warning message.")
         * log.error("This is an error message.")
         * ```
         *
         * @see KotlinLogging.logger
         */
        private val log = KotlinLogging.logger { }

        /**
         * Reads a property from the given buffer.
         *
         * @param buffer the buffer to read from
         * @return the deserialized property
         * @throws RuntimeException if no serializer is found for the type of the property
         */
        fun read(buffer: Buffer): Property<*> {
            val type = buffer.readClass()
//            log.debug { "Reading type ${type.name}" }
            val serializer = Serial[type]
            if (serializer !is PropertySerializer<*>) throw RuntimeException("No serializer found for type $type")
            return serializer.deserialize(buffer)
        }

        /**
         * Read and parse the typed property from the given buffer.
         *
         * @param buffer The buffer to read from.
         * @return The parsed typed property.
         * @throws ClassCastException if the property cannot be cast to the specified type.
         */
        inline fun <reified T : Property<*>> readTyped(buffer: Buffer): T {
            val property = read(buffer)
            return property.cast()
        }

        /**
         * Writes the given value to the specified buffer using the appropriate serializer.
         *
         * @param buffer the buffer to write the value to
         * @param value the value to be written
         * @throws RuntimeException if no serializer is found for the type of the value
         */
        fun <T : Property<*>> write(buffer: Buffer, value: T) {
            val type = value::class.java
            buffer.writeClass(type)
//            log.debug { "Writing type ${type.name}" }
            val serializer = Serial[type]
            if (serializer !is PropertySerializer<*>) throw RuntimeException("No serializer found for type $type")
            @Suppress("UNCHECKED_CAST") (serializer as PropertySerializer<T>).serialize(buffer, value)
        }

        fun register() {

            //Register all property serializers
            Serial.register(IntPropertySerializer)
            Serial.register(FloatPropertySerializer)
            Serial.register(DoublePropertySerializer)
            Serial.register(CharPropertySerializer)
            Serial.register(StringPropertySerializer)
            Serial.register(UUIDPropertySerializer)
            Serial.register(BooleanPropertySerializer)
            Serial.register(LongPropertySerializer)
            Serial.register(ClassPropertySerializer)
            Serial.register(Vec2fPropertySerialize)
            Serial.register(Vec3fPropertySerialize)
            Serial.register(Vec4fPropertySerialize)
            Serial.register(Vec2iPropertySerialize)
            Serial.register(Vec3iPropertySerialize)
            Serial.register(Vec4iPropertySerialize)
            Serial.register(ListPropertySerializer)
            Serial.register(MapPropertySerializer)
            Serial.register(NullPropertySerializer)

        }

    }

    /**
     * A serializer for serializing and deserializing IntProperty objects.
     *
     * This class extends the PropertySerializer class and provides implementation
     * for serializing and deserializing IntProperty objects.
     *
     * @param <T> the type of the IntProperty object
     */
    data object IntPropertySerializer : PropertySerializer<Property.Int>(Property.Int::class) {

        override fun deserialize(buffer: Buffer): Property.Int = Property.Int(buffer.readInt())

        override fun serialize(buffer: Buffer, value: Property.Int) = buffer.writeInt(value.get())

    }

    /**
     * Serializer for FloatProperty.
     *
     * This class extends PropertySerializer and provides serialization and deserialization
     * functionality for Property.Float objects.
     *
     * @see PropertySerializer
     * @see Property.Float
     */
    data object FloatPropertySerializer : PropertySerializer<Property.Float>(Property.Float::class) {

        override fun deserialize(buffer: Buffer): Property.Float = Property.Float(buffer.readFloat())

        override fun serialize(buffer: Buffer, value: Property.Float) = buffer.writeFloat(value.get())

    }

    /**
     * A serializer for DoubleProperty objects.
     */
    data object DoublePropertySerializer : PropertySerializer<Property.Double>(Property.Double::class) {

        override fun deserialize(buffer: Buffer): Property.Double = Property.Double(buffer.readDouble())


        override fun serialize(buffer: Buffer, value: Property.Double) = buffer.writeDouble(value.get())

    }

    data object CharPropertySerializer : PropertySerializer<Property.Char>(Property.Char::class) {

        override fun deserialize(buffer: Buffer): Property.Char = Property.Char(buffer.readChar())

        override fun serialize(buffer: Buffer, value: Property.Char) = buffer.writeChar(value.get())
    }

    /**
     * Serializes and deserializes String properties.
     *
     * This class provides methods to serialize and deserialize properties of type String.
     * It extends the PropertySerializer class and overrides its methods to handle String properties.
     */
    data object StringPropertySerializer : PropertySerializer<Property.String>(Property.String::class) {

        override fun deserialize(buffer: Buffer): Property.String = Property.String(buffer.readString())

        override fun serialize(buffer: Buffer, value: Property.String) = buffer.writeString(value.get())

    }

    /**
     * Serializes and deserializes a property of type UUID.
     *
     * This class is a concrete implementation of the `PropertySerializer` class for handling UUID properties.
     */
    data object UUIDPropertySerializer : PropertySerializer<Property.UUID>(Property.UUID::class) {

        override fun deserialize(buffer: Buffer): Property.UUID = Property.UUID(UUID.fromString(buffer.readString()))

        override fun serialize(buffer: Buffer, value: Property.UUID) = buffer.writeString(value.get().toString())

    }

    /**
     * Serializer for BooleanProperty.
     *
     * This class provides methods to serialize and deserialize BooleanProperty objects.
     * It extends the PropertySerializer class and specifies the type parameter as Property.Boolean.
     *
     * @see PropertySerializer
     */
    data object BooleanPropertySerializer : PropertySerializer<Property.Boolean>(Property.Boolean::class) {

        override fun deserialize(buffer: Buffer): Property.Boolean = Property.Boolean(buffer.readBoolean())

        override fun serialize(buffer: Buffer, value: Property.Boolean) = buffer.writeBoolean(value.get())

    }

    /**
     * This class is responsible for serializing and deserializing [Property.Long] objects.
     * It extends the [PropertySerializer] class.
     *
     * @param T The type of property this serializer handles.
     */
    data object LongPropertySerializer : PropertySerializer<Property.Long>(Property.Long::class) {

        override fun deserialize(buffer: Buffer): Property.Long = Property.Long(buffer.readLong())

        override fun serialize(buffer: Buffer, value: Property.Long) = buffer.writeLong(value.get())

    }

    /**
     * ClassPropertySerializer extends PropertySerializer and is used for serializing and deserializing
     * Property.Class objects.
     *
     * @see PropertySerializer
     * @*/
    data object ClassPropertySerializer : PropertySerializer<Property.Class>(Property.Class::class) {

        override fun deserialize(buffer: Buffer): Property.Class = Property.Class(buffer.readClass())

        override fun serialize(buffer: Buffer, value: Property.Class) = buffer.writeClass(value.get())

    }

    //Vec2f serializer
    data object Vec2fPropertySerialize : PropertySerializer<Property.Vec2f>(Property.Vec2f::class) {

        /**
         * Deserializes the contents of the Buffer and returns an instance of type T.
         *
         * @return The deserialized object of type T.
         */
        override fun deserialize(buffer: Buffer): Property.Vec2f {
            val x = buffer.readFloat()
            val y = buffer.readFloat()
            return Property.Vec2f(Vector2f(x, y))
        }
        /**
         * Serializes the provided value into the buffer.
         *
         * @param value The value to be serialized.
         */
        override fun serialize(buffer: Buffer, value: Property.Vec2f) {
            buffer.writeFloat(value.get().x)
            buffer.writeFloat(value.get().y)
        }

    }

    //Vec3f serializer
    data object Vec3fPropertySerialize : PropertySerializer<Property.Vec3f>(Property.Vec3f::class) {

        /**
         * Deserializes the contents of the Buffer and returns an instance of type T.
         *
         * @return The deserialized object of type T.
         */
        override fun deserialize(buffer: Buffer): Property.Vec3f {
            val x = buffer.readFloat()
            val y = buffer.readFloat()
            val z = buffer.readFloat()
            return Property.Vec3f(Vector3f(x, y, z))
        }
        /**
         * Serializes the provided value into the buffer.
         *
         * @param value The value to be serialized.
         */
        override fun serialize(buffer: Buffer, value: Property.Vec3f) {
            buffer.writeFloat(value.get().x)
            buffer.writeFloat(value.get().y)
            buffer.writeFloat(value.get().z)
        }

    }

    //Vec4f serializer
    data object Vec4fPropertySerialize : PropertySerializer<Property.Vec4f>(Property.Vec4f::class) {

        /**
         * Deserializes the contents of the Buffer and returns an instance of type T.
         *
         * @return The deserialized object of type T.
         */
        override fun deserialize(buffer: Buffer): Property.Vec4f {
            val x = buffer.readFloat()
            val y = buffer.readFloat()
            val z = buffer.readFloat()
            val w = buffer.readFloat()
            return Property.Vec4f(Vector4f(x, y, z, w))
        }
        /**
         * Serializes the provided value into the buffer.
         *
         */
        override fun serialize(buffer: Buffer, value: Property.Vec4f) {
            buffer.writeFloat(value.get().x)
            buffer.writeFloat(value.get().y)
            buffer.writeFloat(value.get().z)
            buffer.writeFloat(value.get().w)
        }

    }

    //Vec2i serializer
    data object Vec2iPropertySerialize : PropertySerializer<Property.Vec2i>(Property.Vec2i::class) {

        /**
         * Deserializes the contents of the Buffer and returns an instance of type T.
         *
         * @return The deserialized object of type T.
         */
        override fun deserialize(buffer: Buffer): Property.Vec2i {
            val x = buffer.readInt()
            val y = buffer.readInt()
            return Property.Vec2i(Vector2i(x, y))
        }
        /**
         * Serializes the provided value into the buffer.
         *
         * @param value The value to be serialized.
         */
        override fun serialize(buffer: Buffer, value: Property.Vec2i) {
            buffer.writeInt(value.get().x)
            buffer.writeInt(value.get().y)
        }

    }

    //Vec3i serializer
    data object Vec3iPropertySerialize : PropertySerializer<Property.Vec3i>(Property.Vec3i::class) {

        /**
         * Deserializes the contents of the Buffer and returns an instance of type T.
         *
         * @return The deserialized object of type T.
         */
        override fun deserialize(buffer: Buffer): Property.Vec3i {
            val x = buffer.readInt()
            val y = buffer.readInt()
            val z = buffer.readInt()
            return Property.Vec3i(Vector3i(x, y, z))
        }
        /**
         * Serializes the provided value into the buffer.
         *
         * @param value The value to be serialized.
         */
        override fun serialize(buffer: Buffer, value: Property.Vec3i) {
            buffer.writeInt(value.get().x)
            buffer.writeInt(value.get().y)
            buffer.writeInt(value.get().z)
        }

    }

    //Vec4i serializer
    data object Vec4iPropertySerialize : PropertySerializer<Property.Vec4i>(Property.Vec4i::class) {

        /**
         * Deserializes the contents of the Buffer and returns an instance of type T.
         *
         * @return The deserialized object of type T.
         */
        override fun deserialize(buffer: Buffer): Property.Vec4i {
            val x = buffer.readInt()
            val y = buffer.readInt()
            val z = buffer.readInt()
            val w = buffer.readInt()
            return Property.Vec4i(Vector4i(x, y, z, w))
        }
        /**
         * Serializes the provided value into the buffer.
         *
         * @param value The value to be serialized.
         * @throws SerializationException if an error occurs during serialization.
         */
        override fun serialize(buffer: Buffer, value: Property.Vec4i) {
            buffer.writeInt(value.get().x)
            buffer.writeInt(value.get().y)
            buffer.writeInt(value.get().z)
            buffer.writeInt(value.get().w)
        }

    }


    /**
     * Serializer for the Null property type.
     */
    data object NullPropertySerializer : PropertySerializer<Property.Null>(Property.Null::class) {

        override fun deserialize(buffer: Buffer): Property.Null = Property.Null

        override fun serialize(buffer: Buffer, value: Property.Null) = Unit
    }

    /**
     * Serializes and deserializes a List of Property objects.
     *
     * This class implements the PropertySerializer interface for Property.List objects.
     *
     * @see PropertySerializer
     * @see Property.List
     *
     */
    data object ListPropertySerializer : PropertySerializer<Property.List>(Property.List::class) {

        /**
         * Deserializes a buffer into a list of Property objects.
         *
         * @param buffer The buffer to deserialize.
         * @return The deserialized list of Property objects.
         */
        override fun deserialize(buffer: Buffer): Property.List {
            val size = buffer.readInt()
            val list = Property.List()
            for (i in 0 until size) {
                list.add(read(buffer))
            }
            return list
        }

        /**
         * Serializes a List of Property objects into a Buffer.
         *
         * @param buffer the Buffer to serialize the Property List into
         * @param value the Property List to be serialized
         */
        override fun serialize(buffer: Buffer, value: Property.List) {
            buffer.writeInt(value().size)
            value().forEach { write(buffer, it) }
        }
    }

    /**
     * The MapPropertySerializer class is responsible for serializing and deserializing Property.Objects that are
     * represented as maps.
     *
     * @param T The type of the property object being serialized/deserialized.
     * @constructor Creates a MapPropertySerializer instance.
     */
    data object MapPropertySerializer : PropertySerializer<Property.Object>(Property.Object::class) {

        /**
         * Deserialize the provided buffer and return a Property.Object.
         *
         * @param buffer The buffer to deserialize.
         * @return The deserialized Property.Object.
         */
        override fun deserialize(buffer: Buffer): Property.Object {
            val size = buffer.readInt()
            val map = Property.Object()
            for (i in 0 until size) {
                val key = buffer.readString()
                val value = read(buffer)
                map[key] = value
            }
            return map
        }

        /**
         * Serializes a Property.Object into a specified buffer.
         *
         * @param buffer the buffer to write the serialized data into.
         * @param value the Property.Object to be serialized.
         */
        override fun serialize(buffer: Buffer, value: Property.Object) {
            buffer.writeInt(value().size)
            value().forEach { (key, value) ->
                buffer.writeString(key)
                write(buffer, value)
            }
        }
    }


}

