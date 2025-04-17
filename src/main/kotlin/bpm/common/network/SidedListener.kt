package bpm.common.network

import bpm.common.logging.KotlinLogging
import bpm.common.network.Endpoint.Side
import bpm.common.packets.Packet
import java.util.UUID

/**
 * An interface for listening to network events.
 */
abstract class SidedListener(protected val side: Side) : Listener {

    override fun onPacket(packet: Packet, from: UUID) {
        when (side) {
            Side.CLIENT -> onClientPacket(packet, from)
            Side.SERVER -> onServerPacket(packet, from)
        }
    }

    protected inline fun isClient(inner: SidedListener.() -> Unit) {
        if (side == Side.CLIENT) {
            inner()
        }
    }

    protected inline fun isServer(inner: SidedListener.() -> Unit) {
        if (side == Side.SERVER) {
            inner()
        }
    }

    open fun onClientPacket(packet: Packet, from: UUID) = Unit
    open fun onServerPacket(packet: Packet, from: UUID) = Unit


    // On server config load
    open fun onServerLoad() = Unit

    // Called on world unload
    open fun onServerUnload() = Unit

    // On client config load
    open fun onClientLoad() = Unit


}

