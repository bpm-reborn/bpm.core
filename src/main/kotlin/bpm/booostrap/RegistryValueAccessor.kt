package bpm.booostrap

import net.neoforged.neoforge.registries.DeferredHolder
import kotlin.reflect.KProperty

class RegistryValueAccessor<T : Any, V : T> {

    lateinit var holder: DeferredHolder<T, V>

    operator fun getValue(thisRef: Any?, property: KProperty<*>): V {
        return holder.get()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: DeferredHolder<T, V>) {
        holder = value
    }
}