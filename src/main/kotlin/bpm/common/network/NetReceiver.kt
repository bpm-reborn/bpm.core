package bpm.common.network

import bpm.common.packets.Packet
import java.util.*

/**
 * Represents a functional listener that implements the [Listener] interface. It provides a simplified way to handle network events by implementing the [receive] function.
 *
 * @see Listener
 */
fun interface NetReceiver : Listener {

    /**
     * Receives a packet from a specified UUID.
     *
     * @param packet The packet to be received.
     * @param from The UUID of the sender.
     */
    fun Listener.receive(packet: Packet, from: UUID)

    /**
     * This method is called when a packet is received.
     *
     * @param packet The received packet.
     * @param from The UUID of the sender.
     */
    override fun onPacket(packet: Packet, from: UUID) {
        receive(packet, from)
    }
}