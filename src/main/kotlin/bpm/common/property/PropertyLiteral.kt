package bpm.common.property

abstract class PropertyLiteral<T : Any>(defaultValue: T) : Property<T> {
    private var value: T = defaultValue
    override fun get(): T = value

    override fun set(value: T) {
        this.value = value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PropertyLiteral<*>) return false
        if (get() != other.get()) return false

        return true
    }

    override fun hashCode(): Int {
        return get().hashCode()
    }

    override fun toString(): String = "${this.javaClass.simpleName}(${get()})"
}

