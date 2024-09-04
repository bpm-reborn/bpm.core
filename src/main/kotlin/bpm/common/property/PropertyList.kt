package bpm.common.property

interface PropertyList : Property<List<Property<*>>> {

    /**
     * Sets the given list of properties as the new value.
     *
     * @param value The list of properties to set.
     * @throws IllegalArgumentException If the current list of properties is not a MutableList<Property<*>>.
     */
    override fun set(value: List<Property<*>>) {
        val list = get()
        if (list is MutableList<Property<*>>) {
            list.clear()
            list.addAll(value)
        } else throw IllegalArgumentException("Property is not a MutableList<Property<*>>")
    }
    /**
     * Adds a value to the given property.
     *
     * @param value The value to be added to the property.
     * @return The property with the value added.
     */
    fun add(value: Property<*>): Property<*> {
        val list = get()
        if (list is MutableList<Property<*>>) {
            list.add(value)
            return value
        }
        return Property.Null
    }


    /**
     * Retrieves the property with the specified index.
     *
     * @param index The index of the property to retrieve.
     * @return The property object with the specified index.
     * @throws IllegalArgumentException If the property at the specified index is not of type Property
     */
    fun <T : Any> getTyped(index: Int): Property<T> = get()[index] as? Property<T>
        ?: throw IllegalArgumentException("Property at index $index is not of type Property<T>")

    /**
     * Returns the generic property at the specified index.
     *
     * @param index The index of the property to retrieve. Must be a non-negative integer.
     * @return The generic property at the specified index.
     */
    operator fun get(index: Int): Property<*> = get()[index]

    /**
     * Sets the value of a property with the given index.
     *
     * @param index The index of the property.
     * @param value The value to be set for the property.
     * @return The Property object representing the updated property.
     * @param <T> The type of the property value.
     */
    operator fun <T : Any> set(index: Int, value: T): Property<T> =
        getTyped<T>(index).apply { set(value) } as? Property<T>
            ?: throw IllegalArgumentException("Property at index $index is not of type Property<T>")

    /**
     * Checks if the given value is contained within the current property.
     *
     * @param value The property value to check.
     * @return True if the given value is contained within the current property,
     *         false otherwise.
     */
    operator fun contains(value: Property<*>): Boolean = get().contains(value)

    /**
     * Returns an iterator over the properties contained in this object.
     *
     * @return an iterator that iterates over the properties in this object.
     */
    operator fun iterator(): Iterator<Property<*>> = get().iterator()

}

