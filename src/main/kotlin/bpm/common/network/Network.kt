package bpm.common.network

import bpm.common.logging.KotlinLogging
import bpm.common.packets.Packet
import bpm.common.packets.graph.GraphListRequestPacket
import bpm.common.packets.graph.GraphListResponsePacket
import bpm.common.packets.internal.ConnectRequest
import bpm.common.packets.internal.ConnectResponsePacket
import bpm.common.packets.internal.DisconnectPacket
import bpm.common.packets.internal.Heartbeat
import bpm.common.utils.*
import bpm.common.workspace.packets.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * The PacketRegistry class is responsible for holding a collection of packets with unique integer keys.
 * It provides methods to register, retrieve, and invoke packet classes.
 */
typealias PacketSupplier = () -> Packet

object Network {

    private val logger = KotlinLogging.logger {}

    /**
     * Holds a collection of packets with unique integer keys.
     * The packets are represented as lambdas that return a Packet object.
     *
     * @property packets The map to store the packets with their corresponding keys.
     */
    private val packets = ConcurrentHashMap<Int, PacketSupplier>()

    private val packetTypes = ConcurrentHashMap<KClass<out Packet>, Int>()

    /**
     * Registers a packet class and its corresponding packet supplier into the packet registry.
     *
     * @param packet                     the packet class to register.
     * @param packetSupplier             the function that supplies an instance of the packet class.
     * @return                           the updated packet registry.
     */
    fun register(packet: KClass<out Packet>, packetSupplier: PacketSupplier): Network {
        val id = packet.java.name.hashCode()
        if (packets.containsKey(id)) logger.warn { "Packet id $id is already registered, it will be overwritten" }
        packets[id] = packetSupplier
        packetTypes[packet] = id
        return this
    }

    /**
     * Registers a packet in the NetRegistry.
     *
     * @param packet The packet class to register.
     * @return The NetRegistry instance.
     */
    fun register(packet: KClass<out Packet>): Network = register(packet) { packet.instantiate }


    /**
     * Finds all packets on the class path and creates a list of them. It will register all packets dynamically.
     */
    fun registerPackets() {
        logger.info { "Registering all packets" }

        // Graph packets
        register(GraphListRequestPacket::class)
        register(GraphListResponsePacket::class)

        // Internal packets
        register(ConnectRequest::class)
        register(ConnectResponsePacket::class)
        register(DisconnectPacket::class)
        register(Heartbeat::class)

        // Workspace packets
        register(EdgePropertyUpdate::class)
        register(LinkCreated::class)
        register(LinkCreateRequest::class)
        register(LinkDeleted::class)
        register(LinkDeleteRequest::class)
        register(NodeCreated::class)
        register(NodeCreateRequest::class)
        register(NodeDeleted::class)
        register(NodeDeleteRequest::class)
        register(NodeLibraryReloadRequest::class)
        register(NodeLibraryRequest::class)
        register(NodeLibraryResponse::class)
        register(NodeMoved::class)
        register(NodeSelected::class)
        register(NotifyMessage::class)
        register(UserConnectedToWorkspace::class)
        register(VariableCreated::class)
        register(VariableCreateRequest::class)
        register(VariableDeleted::class)
        register(VariableDeleteRequest::class)
        register(VariableNodeCreateRequest::class)
        register(VariableUpdated::class)
        register(VariableUpdateRequest::class)
        register(WorkspaceCompileRequest::class)
        register(WorkspaceCreated::class)
        register(WorkspaceCreateRequestPacket::class)
        register(WorkspaceCreateResponsePacket::class)
        register(WorkspaceLibrary::class)
        register(WorkspaceLibraryRequest::class)
        register(WorkspaceLoad::class)
        register(WorkspaceSelected::class)

        logger.info { "Registered ${packets.size} packets" }
    }

    /**
     * Registers a packet type with a packet supplier function in the packet registry.
     *
     * @param packetSupplier The function that supplies the packet instance.
     *                       It takes no arguments and returns an instance of the packet.
     * @return The packet registry after registering the packet type.
     */
    inline fun <reified P : Packet> register(noinline packetSupplier: PacketSupplier): Network =
        register(P::class, packetSupplier)

    /**
     * Collects all packets from the packet registry and returns them as a list.
     *
     * @return A list of all packets in the packet registry.
     */
    val registeredTypes: List<KClass<out Packet>> get() = packetTypes.keys.toList()

    /**
     * Retrieves the packet associated with the given packet ID.
     *
     * @param packetId The ID of the packet to retrieve.
     * @return The packet corresponding to the provided packet ID.
     * @throws IllegalArgumentException if the packet ID is not found in the packets map.
     */
    fun new(packetId: Int): Packet? {
        synchronized(packets) {
            if (!packets.containsKey(packetId)) {
                logger.warn { "Packet id $packetId is not registered" }
                return null
            }
            return (packets[packetId]!!)()
        }
    }

    /**
     * Returns the packet instance with the specified packet type.
     *
     * @param packet the packet type.
     * @return the packet instance of the specified type.
     */
    fun new(packet: KClass<out Packet>): Packet? = new(packet.java.name.hashCode())

    /**
     * Creates a new packet of type [P] using the endpoint instance [E].
     *
     * @return The newly created packet of type [P], or null if the creation fails.
     */
    inline fun <reified P : Packet> new(apply: P.() -> Unit): P {
        val packet = new(P::class.java.name.hashCode()) as P?
            ?: error("Failed to create packet of type ${P::class.simpleName}")
        packet.apply()
        return packet
    }

    /**
     * Invokes the given method and returns an instance of the requested [Packet] type.
     *
     * @return An instance of [P] if available, or null if not found.
     *
     * @param P The type of [Packet] to retrieve.
     *
     * @throws IllegalStateException If the requested [Packet] type [P] does not inherit from [Packet].
     *
     * @see Packet
     */
    inline operator fun <reified P : Packet> invoke(): P? = new(P::class) as P?

    /**
     * Invokes the given method and returns an instance of the requested [Packet] type.
     *
     * @return An instance of [P] if available, or null if not found.
     *
     * @param P The type of [Packet] to retrieve.
     *
     * @throws IllegalStateException If the requested [Packet] type [P] does not inherit from [Packet].
     *
     * @see Packet
     */
    operator fun invoke(packetId: Int): Packet? = new(packetId)

}