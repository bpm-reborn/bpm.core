package bpm.common.property

import bpm.common.network.NetUtils
import org.joml.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType


/**
 * Casts the value type of this property to the specified type [T].
 *
 * @return A new property with the value type casted to [T].
 *
 * @throws ClassCastException If the value type cannot be casted to [T].
 *
 * @param T The desired type to cast to.
 */
inline fun <reified T : Property<*>> Property<*>.cast(): T = this as T


inline infix fun <reified T : Property<*>> Property<*>.castOr(crossinline block: () -> T): T = this as? T ?: block()


/**
 * Retrieves the property with the specified name.
 *
 * @return The property object with the specified name.
 */
interface Property<T : Any> {

    /**
     * Retrieves the value of type T.
     *
     * @return the value of type T
     */
    fun get(): T

    /**
     * This variable represents the number of bytes.
     *
     * @property bytes The number of bytes.
     */
    val bytes: kotlin.Int

    /**
     * Sets the value of the property.
     *
     * @param value the new value for the property
     * @return the updated Property object
     */
    fun set(value: T)

    /**
     * Executes the `get` function and returns its result.
     *
     * @return The result of executing the `get` function.
     */
    operator fun invoke() = get()

    fun copy(): Property<T>

    /**
     * Represents a null property which holds no value.
     */
    object Null : Property<Unit> {


        /**
         * Retrieves the data.
         */
        override fun get() = Unit
        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 0
        override fun copy(): Property<Unit> = this

        /**
         * Sets the value of the property.
         *
         * @param value the new value to be set
         * @return the updated Property instance
         */
        override fun set(value: Unit) = Unit

        override fun toString(): kotlin.String = "NullProperty"

    }
    /**
     * Represents a property that holds an integer value.
     *
     * This class inherits from the `PrimitiveProperty` class to provide common functionality for
     * working with primitive properties.
     *
     * @param defaultValue The default value of the property (default is 0).
     */
    class Int(defaultValue: kotlin.Int = 0) : PropertyLiteral<kotlin.Int>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 4
        override fun copy(): Property<kotlin.Int> = Int(this())
    }

    /**
     * Represents a property that holds a value of type Double.
     *
     * This class is a subclass of [PropertyLiteral] and provides additional methods and functionality specific to Double values.
     *
     * @param defaultValue The default value of the property.
     */
    class Double(defaultValue: kotlin.Double = 0.0) : PropertyLiteral<kotlin.Double>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 8
        override fun copy(): Property<kotlin.Double> = Double(this())
    }

    /**
     * Represents a property that holds a string value.
     *
     * This class extends the `PrimitiveProperty` class to provide additional functionality for string values.
     *
     * @param defaultValue The default value for the property. Default value is an empty string `""`.
     *
     * @see PropertyLiteral
     *
     */
    class String(defaultValue: kotlin.String = "") : PropertyLiteral<kotlin.String>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 4 + get().length
        override fun copy(): Property<kotlin.String> = String(this())
    }

    /**
     * Represents a UUID property literal.
     *
     * @property defaultValue The default value for the UUID property.
     */
    class UUID(defaultValue: java.util.UUID = NetUtils.DefaultUUID) :
        PropertyLiteral<java.util.UUID>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 16
        override fun copy(): Property<java.util.UUID> = UUID(this())
    }

    /**
     * Represents a Boolean property.
     *
     * This class extends the `PrimitiveProperty` class, which provides the basic functionality of a property.
     * The `BooleanProperty` class specifically handles properties with boolean values.
     *
     * @param defaultValue The default value of the property. Defaults to `false` if not specified.
     *
     * @constructor Creates a new BooleanProperty object with the specified default value.
     */
    class Boolean(defaultValue: kotlin.Boolean = false) : PropertyLiteral<kotlin.Boolean>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 1
        override fun copy(): Property<kotlin.Boolean> = Boolean(this())
    }

    /**
     * Represents a property with a long value.
     *
     * @param defaultValue The default value of the property.
     */
    class Long(defaultValue: kotlin.Long = 0L) : PropertyLiteral<kotlin.Long>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 8
        override fun copy(): Property<kotlin.Long> = Long(this())
    }

    /**
     * A property that stores a float value.
     *
     * @param defaultValue The default value of the property. Defaults to 0.0f.
     */
    class Float(defaultValue: kotlin.Float = 0.0f) : PropertyLiteral<kotlin.Float>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 4
        override fun copy(): Property<kotlin.Float> = Float(this())
    }

    /**
     * Represents a property that holds a value of type Short.
     *
     * @property defaultValue The default value of the property.
     *
     * @constructor Creates a ShortProperty with the specified default value.
     *
     * @param defaultValue The default value of the property.
     */
    class Short(defaultValue: kotlin.Short = 0) : PropertyLiteral<kotlin.Short>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 2
        override fun copy(): Property<kotlin.Short> = Short(this())
    }

    /**
     * Represents a property that holds a value of type Byte.
     *
     * This class extends the `PrimitiveProperty` class and encapsulates a Byte value.
     * It provides methods to get and set the value, as well as methods to observe changes to the value.
     *
     * @param defaultValue The default value of the property. Defaults to 0 if not specified.
     */
    class Byte(defaultValue: kotlin.Byte = 0) : PropertyLiteral<kotlin.Byte>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 1
        override fun copy(): Property<kotlin.Byte> = Byte(this())
    }

    /**
     * Represents a character property that can hold a single character value.
     *
     * @param defaultValue The default character value for the property.
     */
    class Char(defaultValue: kotlin.Char = ' ') : PropertyLiteral<kotlin.Char>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 1
        override fun copy(): Property<kotlin.Char> = Char(this())
    }
    /**
     * Represents a color property.
     * @property defaultValue The default value of the color property.
     * @constructor Creates a color property with the given default value.
     * @param defaultValue The default value of the color property. Defaults to Vector4i(40, 20, 69, 255).
     */
    class Vec2f(defaultValue: Vector2f) : PropertyLiteral<Vector2f>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 8
        override fun copy(): Property<Vector2f> = Vec2f(this())
    }
    /**
     * A class representing a 3-dimensional vector of floats.
     *
     * @param defaultValue The default value of the vector.
     */
    class Vec3f(defaultValue: Vector3f) : PropertyLiteral<Vector3f>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 12
        override fun copy(): Property<Vector3f> = Vec3f(this())
    }
    /**
     * Represents a 4-component vector in 3D space.
     *
     * This class extends the [PropertyLiteral] class and provides specialized methods and properties for working with 4D vectors.
     *
     * @param defaultValue The default value for the vector.
     */
    class Vec4f(defaultValue: Vector4f) : PropertyLiteral<Vector4f>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 16
        override fun copy(): Property<Vector4f> = Vec4f(this())
    }

    /**
     * Represents a 2D vector of integers.
     *
     * @property defaultValue The default value of the vector.
     * @constructor Creates a Vec2i object with the specified default value.
     */
    class Vec2i(defaultValue: Vector2i) : PropertyLiteral<Vector2i>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 8
        override fun copy(): Property<Vector2i> = Vec2i(this())
    }
    /**
     * Represents a 3D vector of integers.
     *
     * This class extends the `PropertyLiteral` class and provides additional functionality for 3D integer vectors.
     * It holds three integer values representing the x, y, and z components of the vector.
     *
     * @constructor Creates a `Vec3i` instance with the specified default value.
     * @param defaultValue The default value for the vector.
     */
    class Vec3i(defaultValue: Vector3i) : PropertyLiteral<Vector3i>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 12
        override fun copy(): Property<Vector3i> = Vec3i(this())
    }
    /**
     * Represents a 4-dimensional integer vector.
     *
     * @param defaultValue The default value of the vector.
     */
    class Vec4i(defaultValue: Vector4i) : PropertyLiteral<Vector4i>(defaultValue) {

        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 16
        override fun copy(): Property<Vector4i> = Vec4i(this())

    }


    class Class(val value: java.lang.Class<*>) : Property<java.lang.Class<*>> {

        override fun get(): java.lang.Class<*> = value
        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 4 + get().javaClass.name.length
        override fun copy(): Property<java.lang.Class<*>> = Class(this())

        override fun set(value: java.lang.Class<*>) {
            throw UnsupportedOperationException("Cannot set value of ClassProperty")
        }

        override fun toString(): kotlin.String {
            return "ClassProperty(${get().simpleName})"
        }
    }
    /**
     * Represents a class that stores a collection of properties.
     *
     * @property properties A mutable map that holds the properties.
     */
    open class Object(private val properties: ConcurrentHashMap<kotlin.String, Property<*>> = ConcurrentHashMap()) :
        PropertyMap {

        override fun get() = properties
        /**
         * Finds a property by it's qualified name.
         *
         * This can include numbers for list indices, e.g. `list.0.test`.
         */
        override fun find(name: kotlin.String): Property<*>? {
            if (!name.contains('.')) return get()[name]
            val parts = name.split('.')
            var current: Property<*>? = get()[parts[0]]
            for (i in 1 until parts.size) {
                if (current is List) {
                    current = current.getTyped<PropertyList>(parts[i].toIntOrNull() ?: 0)
                    continue
                }
                if (current !is Object) return null
                current = current.getTyped<Object>(parts[i])
            }
            return current
        }
        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 4 + get().values.sumOf { it.bytes } + get().keys.sumOf { it.length + 4 }
        override fun copy() = Object(ConcurrentHashMap(this().mapValues { it.value.copy() }))

        private fun takeFirstPart(name: kotlin.String): kotlin.String {
            val index = name.indexOf('.')
            return if (index == -1) name else name.substring(0, index)
        }


        override fun equals(other: Any?): kotlin.Boolean {
            if (this === other) return true
            if (other !is Object) return false

            if (properties != other.properties) return false

            return true
        }

        override fun hashCode(): kotlin.Int {
            return properties.hashCode()
        }


        companion object {

            operator fun invoke(apply: PropertyMap.() -> Unit): Object {
                val obj = Object()
                obj.apply()
                return obj
            }
        }
    }

    /**
     * Represents a list property that holds a mutable list of properties.
     * Implements the PropertyList interface.
     *
     * @property properties The mutable list of properties.
     */
    class List(private val properties: MutableList<Property<*>> = mutableListOf()) : PropertyList {

        override fun get(): kotlin.collections.List<Property<*>> = properties
        /**
         * This variable represents the number of bytes.
         *
         * @property bytes The number of bytes.
         */
        override val bytes: kotlin.Int = 4 + get().sumOf { it.bytes }
        override fun copy() = List(this().map { it.copy() }.toMutableList())

        override fun toString(): kotlin.String = "ListProperty(${get()})"
        override fun equals(other: Any?): kotlin.Boolean {
            if (this === other) return true
            if (other !is List) return false

            if (properties != other.properties) return false

            return true
        }

        override fun hashCode(): kotlin.Int {
            return properties.hashCode()
        }

        companion object {

            operator fun invoke(apply: PropertyList.() -> Unit): List {
                val list = List()
                list.apply()
                return list
            }
        }

    }

    companion object {

        /**
         * Creates and returns a Property object based on the given value.
         *
         * @param value the value to create the Property object from.
         * @return a Property object representing the given value.
         * @throws IllegalArgumentException if the value's type cannot be converted to a Property object.
         */
        fun of(value: Any): Property<*> {
            return when (value) {
                is kotlin.Int -> Int(value)
                is kotlin.Double -> Double(value)
                is kotlin.String -> String(value)
                is kotlin.Boolean -> Boolean(value)
                is kotlin.Long -> Long(value)
                is kotlin.Float -> Float(value)
                is kotlin.Short -> Short(value)
                is kotlin.Byte -> Byte(value)
                is kotlin.Char -> Char(value)
                is java.util.UUID -> UUID(value)
                is java.lang.Class<*> -> Class(value)
                is Map<*, *> -> {
                    val map = value as Map<kotlin.String, *>
                    Object {
                        map.forEach { (key, value) ->
                            this[key] = of(value!!)
                        }
                    }
                }

                is MutableList<*> -> List(value as MutableList<Property<*>>)
                is kotlin.collections.List<*> -> List(value.toMutableList() as MutableList<Property<*>>)
                else -> ofVector(value)
            }
        }

        private fun ofVector(value: Any): Property<*> = when (value) {
            is Vector2f -> Vec2f(value)
            is Vector3f -> Vec3f(value)
            is Vector4f -> Vec4f(value)
            is Vector2i -> Vec2i(value)
            is Vector3i -> Vec3i(value)
            is Vector4i -> Vec4i(value)
            else -> throw IllegalArgumentException("Cannot create property of type ${value::class.simpleName}")
        }
    }
}

/**
 * An interface representing an object holder that holds a set of properties.
 */
interface PropertySupplier {

    val properties: PropertyMap
}

inline fun <reified T : PropertySupplier> configured(crossinline apply: PropertyMap.() -> Unit): T {
    val type = T::class
    //Find a constructor that only takes a PropertyMap
    val propertiesConstructor = type.constructors.find {
        it.parameters.size == 1 && (it.parameters[0].type == PropertyMap::class || it.parameters[0].type.isSubtypeOf(
            PropertyMap::class.starProjectedType
        ))
    } ?: throw IllegalArgumentException("Cannot find constructor for type $type that takes a PropertyMap")
    val obj = propertiesConstructor.call(Property.Object {
        apply()
    })
    return obj
}