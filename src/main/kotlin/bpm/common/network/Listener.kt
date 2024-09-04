package bpm.common.network

import bpm.common.logging.KotlinLogging
import bpm.common.network.Endpoint.Side
import bpm.common.packets.Packet
import java.util.UUID

/**
 * An interface for listening to network events.
 */
interface Listener {

    val server: Server get() = Server
    val client: Client get() = Client

    /**
     * Called when the listener is installed and the endpoint is started.
     */
    fun onInstall() = Unit

    /**
     * Called when the listener is uninstalled and the endpoint is stopped.
     */
    fun onUninstall() = Unit

    /**
     * Called when a packet is received.
     *
     * @param packet the packet that was received
     */
    fun onPacket(packet: Packet, from: UUID) = Unit

    /**
     * Executes the logic for each tick of the game loop.
     *
     * @param delta The elapsed time since the last tick, in milliseconds.
     * @return Nothing is explicitly returned.
     */
    fun onTick(delta: Float, tick: Int): Unit = Unit

    /**
     * This method is called when a connection with a unique identifier (UUID) is established.
     *
     * @param uuid The unique identifier (UUID) of the connected device.
     */
    fun onConnect(uuid: UUID) = Unit
    /**
     * Called when a user with the given UUID disconnects from the system.
     *
     * @param uuid The UUID of the user who disconnected.
     */
    fun onDisconnect(uuid: UUID) = Unit

    companion object {

        private val logger = KotlinLogging.logger {}
    }
}

inline fun <reified T : Listener> listener(side: Side): T = Endpoint.get(side).installed<T>()
