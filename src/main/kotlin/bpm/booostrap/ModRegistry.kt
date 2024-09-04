package bpm.booostrap

import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

interface ModRegistry<T : Any> {

    val registry: DeferredRegister<T>

    fun register(modBus: IEventBus) = registry.register(modBus)

    fun <V : T> register(registrationFunction: () -> DeferredHolder<T, V>): RegistryValueAccessor<T, V> {
        return RegistryValueAccessor<T, V>().also { it.setValue(null, it::holder, registrationFunction()) }
    }
}