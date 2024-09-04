package bpm.common.network


import bpm.network.MinecraftNetworkAdapter
import bpm.network.ServerTarget
import net.minecraft.client.Minecraft
import bpm.common.logging.KotlinLogging
import bpm.common.network.Network.new
import bpm.common.packets.*
import bpm.common.packets.internal.*
import java.util.*

object Client : Endpoint<Client>() {

    // Gets the current player's UUID
    private val uuid by lazy { Minecraft.getInstance().player?.uuid ?: error("Player UUID not available") }


    private val logger = KotlinLogging.logger {}

    /**
     * Represents a worker object.
     *
     * @property worker The instance of the Worker class.
     */
    override val worker: Worker = Worker(this)


    /**
     * Indicates whether a connection is established or not.
     *
     * @property connected True if a connection is established, false otherwise.
     */
    var connected = false
        private set

    override fun connected(id: Connection) {
        send(new<ConnectRequest> {
            this.uuid = this@Client.uuid
        })
    }

    fun connect(): Client {
        if (connected) return this
        send(new<ConnectRequest> {
            this.uuid = this@Client.uuid
        })
        connected = true
        return this
    }


    /**
     * Initiates the connection to the server.
     */
    override fun initiate() {
        try {
            worker.start()
        } catch (e: Exception) {
            logger.error {
                "Failed to start client thread"
            }
            runningRef.set(false)
        }
    }


    internal fun disconnect() {
        if(!connected) return
        send(new<DisconnectPacket> {
            this.uuid = this@Client.uuid
        })
        logger.info { "Sent client disconnection packet" }
        synchronized(listeners) {
            listeners.forEach {
                it.onDisconnect(NetUtils.DefaultUUID)
            }
        }
        connected = false
    }


    /**
     * Terminates the client connection.
     */
    override fun terminate() {
        runningRef.set(false)
    }


    /**
     * Sends a packet to the server.
     */
    override fun send(packet: Packet, id: Connection?) {
        MinecraftNetworkAdapter.sendPacket(packet, ServerTarget)
        logger.info { "Sent packet of type ${packet::class.simpleName} with id ${packet.id}" }
    }
    /**
     * Sends the given packet to all connected endpoints.
     */
    override fun sendToAll(packet: Packet, vararg exclude: UUID) = send(packet)

    inline operator fun invoke(block: (Client) -> Unit): Client {
        block(this)
        return this
    }

    /**
     * Retrieves the current connection.
     */
    override fun get(connectionID: UUID): Connection? {
        return null
    }

}

