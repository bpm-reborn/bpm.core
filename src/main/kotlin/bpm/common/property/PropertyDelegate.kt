package bpm.common.property

import kotlin.reflect.*

class PropertyDelegate<T : Any>(
    private val propertyMap: PropertyMap,
    private val defaultValue: () -> T
) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val name = property.name
        if (!propertyMap.contains(name)) propertyMap[name] = Property.of(defaultValue())
        return propertyMap.getTyped<Property<T>>(property.name)
            .get()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val name = property.name
        if (!propertyMap.contains(name)) propertyMap[name] = Property.of(value)
        else propertyMap.getTyped<Property<T>>(property.name)
            .set(value)
    }
}

infix fun <T : Any> PropertyMap.to(defaultValue: () -> T): PropertyDelegate<T> {
    return PropertyDelegate(this, defaultValue)
}


infix fun <T : Any> PropertyMap.delegate(defaultValue: () -> T): PropertyDelegate<T> {
    return PropertyDelegate(this, defaultValue)
}
