package bpm.common.network

import bpm.network.*
import net.minecraft.server.MinecraftServer
import net.neoforged.neoforge.server.ServerLifecycleHooks
import bpm.common.logging.KotlinLogging
import bpm.common.network.Network.new
import bpm.common.packets.*
import bpm.common.packets.internal.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object Server : Endpoint<Server>() {

    // Create our logger
    private val logger = KotlinLogging.logger {}

    // Stores the connections to the server
    private val clients: ConcurrentHashMap<UUID, Connection> = ConcurrentHashMap()
    private val cachedClientPlayers: ConcurrentMap<UUID, PlayerTarget> = ConcurrentHashMap()
    override val worker = Worker(this)
    private val server: MinecraftServer by lazy {
        ServerLifecycleHooks.getCurrentServer() ?: error("Server not available")
    }


    /**
     * This should start the server or connect to the server depending on the implementation.
     */
    override fun initiate() {
        try {
            worker.start()
        } catch (e: Exception) {
            logger.error(e) { "Failed to start server" }
            return
        }
        //TODO: relay to listeners
    }

    /**
     * This should stop the server or disconnect from the server depending on the implementation.
     */
    override fun terminate() {
        runningRef.set(false)
        clients.clear()
//        socket?.close()
        //TODO: relay to listeners
        logger.info { "Server terminated" }
    }


    /**
     * Called when a client has connected
     */
    override fun connected(id: Connection) {
//        logger.info { "Client ${id.uuid} connected" }
        send(new<ConnectResponsePacket> {
            this.valid = true
        }, id)
        id.uuid = id.uuid
        listeners.forEach {
            it.onConnect(id.uuid)
        }
        clients[id.uuid] = id
    }


    /**
     * Sends the given packet to a specified ID. If no ID is specified, the packet will be sent to all connected endpoints.
     * If the packet is not sent, this method will return 0. Otherwise, it will return the number of endpoints that the packet was sent to.
     *
     * @param packet The packet to send. This packet will be serialized and sent to the specified ID.
     * @param id The ID of the recipient (optional). If no ID is specified, the packet will be sent to all connected endpoints.
     */
    override fun send(packet: Packet, id: Connection?) {
        if (id == null)
            MinecraftNetworkAdapter.sendPacket(packet, AllPlayersTarget)
        else {
            val playerUUID = id.uuid
            MinecraftNetworkAdapter.sendPacket(packet, targetOf(playerUUID) ?: AllPlayersTarget)
        }
    }
    /**
     * Sends the given packet to all connected endpoints.
     */
    override fun sendToAll(packet: Packet, vararg exclude: UUID) {
        MinecraftNetworkAdapter.sendPacket(packet, AllPlayersTarget)
    }

    private fun targetOf(uuid: UUID): PacketTarget? {
        return cachedClientPlayers.computeIfAbsent(uuid) {
            val player = server.playerList.getPlayer(uuid)
            if (player == null) {
                logger.warn { "Player $uuid not found" }
                return@computeIfAbsent null
            }
            PlayerTarget(player)
        }
    }

    /**
     * Retrieves the Connection associated with the specified connection ID.
     *
     * @param connectionID The ID of the connection to retrieve.
     * @return The Connection object associated with the given connection ID, or null if no such connection exists.
     */
    override fun get(connectionID: UUID): Connection? {
        return clients[connectionID]
    }
    /**
     * Disconnects the specified user by their ID.
     *
     * @param id the ID of the user to be disconnected
     */
    override fun disconnected(id: Connection) {
        listeners.forEach {
            it.onDisconnect(id.uuid)
        }
        clients.remove(id.uuid)
        logger.warn { "Client ${id.uuid} disconnected" }
        //TODO: relay to listeners
    }


}