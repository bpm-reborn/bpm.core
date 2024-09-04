package bpm.common.property

import bpm.common.logging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger { }

interface PropertyMap : Property<MutableMap<String, Property<*>>>, Iterable<Pair<String, Property<*>>> {

    /**
     * Returns whether the result of [get] is an empty string.
     *
     * @return `true` if the result of [get] is an empty string, `false` otherwise.
     */
    val isEmpty: Boolean
        get() = get().isEmpty()

    /**
     * Represents the size of something.
     */
    val length: Int
        get() = get().size

    /**
     * Returns a mutable map of property names to their corresponding property objects.
     *
     * @return a mutable map containing property names as keys and property objects as values.
     */
    override fun get(): ConcurrentHashMap<String, Property<*>>

    /**
     * Checks if the given name is contained in the map.
     *
     * @param name The name to be checked.
     * @return `true` if the name is contained in the map, `false` otherwise.
     */
    operator fun contains(name: String): Boolean = get().containsKey(name)

    /**
     * Sets the value of the property using the specified map of properties.
     *
     * @param value The map of properties to set.
     * @return The updated property object.
     */
    override fun set(value: MutableMap<String, Property<*>>) {
        get().clear()
        get().putAll(value)
    }

    /**
     * Sets a property for the current [String].
     *
     * @param property The property to be set.
     * @return The updated [String] with the specified property.
     */
    infix fun String.to(property: Property<*>) = set(this, property)

    /**
     * Sets the specified property of this string to the given value.
     *
     * @param property the property to set
     * @param value the value to set the property to
     */
    infix fun String.to(value: Any) = set(this, Property.of(value))


    /**
     * Returns an iterator over a collection of pairs representing the key-value mappings of the properties.
     *
     * @return an iterator of type [Iterator<Pair<String, IProperty<*>>>] over the collection of key-value mappings
     */
    override fun iterator(): Iterator<Pair<String, Property<*>>> = get().map { Pair(it.key, it.value) }.iterator()

    /**
     * Sets the value of a property with the given name.
     *
     * @param name The name of the property.
     * @param value The value to be set for the property.
     * @return The Property object representing the updated property.
     * @param <T> The type of the property value.
     */
    operator fun <T : Property<*>> set(name: String, value: T) {
        get()[name] = value
    }
    /**
     * Clears the properties.
     * This will remove all properties from the map.
     */
    fun clear() {
        get().clear()
    }

    /**
     * Returns the property with the given name.
     *
     * @param name The name of the property.
     * @return The property object with the given name.
     * @param <T> The type of the property value.
     */
    operator fun get(name: String): Property<*> {
        return get()[name] ?: Property.Null
    }

    /**
     * Finds a property by it's qualified name.
     *
     * This can include numbers for list indices, e.g. `list.0.test`.
     */
    fun find(name: String): Property<*>?
    fun putAll(properties: PropertyMap) {
        get().putAll(properties.get())
    }

}

/**
 * Finds a property with the specified name and type.
 *
 * @param name The name of the property to find.
 * @param type The type of the property to find.
 * @return The property with the specified name and type, or null if not found.
 */
inline fun <reified T : Property<*>> PropertyMap.findTyped(name: String): T? = find(name) as? T


/**
 * Retrieves the property with the given name and casts it to the specified type.
 *
 * @param name the name of the property to retrieve
 * @return the property with the given name, casted to the specified type
 * @throws IllegalArgumentException if the property with the given name is not of type Property<T>
 */
fun <T : Property<*>> PropertyMap.getTyped(name: String): T =
    get(name) as? T ?: throw IllegalArgumentException("Property $name is not of type Property<T>")
