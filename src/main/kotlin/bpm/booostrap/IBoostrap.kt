package bpm.booostrap

import net.neoforged.bus.api.IEventBus
import bpm.common.packets.Packet
import bpm.common.serial.Serialize
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import kotlin.reflect.KClass

interface IBoostrap {

    val registries: List<ModRegistry<*>>
    val packets: List<KClass<out Packet>>
    val serializable: List<KClass<out Serialize<*>>>

    fun collect(): IBoostrap

    fun register(bus: IEventBus = MOD_BUS): IBoostrap

}

inline fun <reified T : ModRegistry<*>> IBoostrap.registry(): T {
    return registries.first { it::class == T::class } as T
}

inline fun <reified T : Packet> IBoostrap.packet(): KClass<out Packet> {
    return packets.first { it == T::class }
}

inline fun <reified T : Serialize<*>> IBoostrap.serializable(): KClass<out Serialize<*>> {
    return serializable.first { it == T::class }
}