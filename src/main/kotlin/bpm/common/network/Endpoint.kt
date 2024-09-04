package bpm.common.network

import bpm.common.logging.KotlinLogging
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import bpm.common.utils.className
import bpm.common.utils.instantiate
import bpm.common.utils.instantiateWith
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import kotlin.reflect.KClass

typealias ListenerProvider = Supplier<Listener>

/**
 * The Endpoint interface represents a connection endpoint in a client-server communication system.
 * It provides methods to handle connection events such as client connection and disconnection.
 */
abstract class Endpoint<T : Endpoint<T>> {


    /**
     * Get the value with the specified type.
     *
     * @return The value with the specified type.
     */
    @Suppress("UNCHECKED_CAST")
    val typed: T get() = this as T

    /**
     * Represents the internal worker object that is responsible for managing the network state.
     */
    abstract val worker: Worker
    /**
     * This should start the server or connect to the server depending on the implementation.
     */
    internal abstract fun initiate()

    /**
     * This should stop the server or disconnect from the server depending on the implementation.
     */
    internal abstract fun terminate()

    /**
     * Called when a client has connected
     */
    open fun connected(id: Connection) = Unit

    /**
     * Flag indicating whether a process is currently running or not.
     *
     * @property runningRef The reference to the AtomicBoolean instance used to track the running state.
     */
    protected val runningRef = AtomicBoolean(false)

    /**
     * Checks if the process is currently running.
     *
     * @return true if the process is running, false otherwise.
     */
    fun isRunning(): Boolean = runningRef.get()

    private var isSerializerRegistered = false

    /**
     * A list of network listeners.
     *
     * This variable represents a collection of NetListener objects that can be attached
     * to various network components to listen for network events and receive notifications.
     *
     * Usage:
     * ```
     * val listeners: List<NetListener> = listOf(listener1, listener2, listener3)
     * ```
     *
     * Note:
     * - The order in which the listeners are added to the list determines the order in which
     *   they will be invoked when network events occur.
     * - The list can be empty if no listeners are attached.
     *
     * @see Listener
     */
    val listeners: MutableList<Listener> = mutableListOf()

    /**
     * Mutable map of typed listeners.
     * The key of the map represents the class of the listener,
     * and the value represents an instance of the listener.
     *
     * @property typedListeners The map of typed listeners.
     */
    open val typedListeners: MutableMap<KClass<out Listener>, Listener> = mutableMapOf()

    /**
     * Protected variable `delayedListeners` is a mutable list that stores objects of type `ListenerProvider`.
     * These `ListenerProvider` objects represent classes that provide listeners.
     * The variable is accessible within the given scope.
     *
     * @see ListenerProvider
     */
    private val delayedListeners: Queue<ListenerProvider> = LinkedList()
    /**
     * Internal variable that represents the shutdown thread.
     *
     * This thread is responsible for invoking the `stop()` method
     * when the application is shutting down.
     *
     * @since 1.0.0
     */
    internal val shutdown = Thread { stop() }

    /**
     * Registers the serial stuff.
     */
    fun registerSerializers() {
//        Serial.registerSerializers()
//        Network.registerPackets()
        isSerializerRegistered = true
    }

    /**
     * Registers the current endpoint.
     *
     * @return The registered endpoint.
     */
    fun start(): T {
        Runtime.getRuntime().addShutdownHook(shutdown)
        runningRef.set(true)
        logger.info { "Endpoint has be registered" }
        if (!isSerializerRegistered) registerSerializers()
        installListeners()
        logger.info { "Serializers have been regsitered" }
        initiate()
        startupListeners()
        return typed
    }

    /**
     * Stops the endpoint by setting the running reference to false, waiting for one second for graceful shutdown,
     * terminating the process, and logging the endpoint status.
     */
    fun stop() {
        if (!runningRef.get()) return
        runningRef.set(false)
        logger.info { "Endpoint has been unregistered, waiting one second for graceful shutdown..." }
        terminate()
        teardownListeners()
        logger.info { "Endpoint has been stopped" }
    }

    /**
     * Called when the listener is installed and the endpoint is started.
     */
    private fun startupListeners() = tellListeners { onInstall() }

    /**
     * Called when the listener is uninstalled and the endpoint is stopped.
     */
    private fun teardownListeners() = tellListeners { onUninstall() }

    /**
     * Installs the listeners and handles their initialization by draining the delayed listeners queue.
     * This method is called by the `start` method after the network and serialization initialization.
     */
    private fun installListeners() {
        // install the delayed listeners by emptying the queue one by one, putting them in the listeners list
        // and then invoking the listener
        while (delayedListeners.isNotEmpty()) {
            val listener = delayedListeners.poll().get()
            listen(listener)
            logger.debug { "Installed listener ${listener.className}" }
        }
    }

    /**
     * Installs a new endpoint with a provided listener type.
     *
     * @return The installed endpoint.
     * @param L The type of the listener.
     */
    inline fun <reified L : Listener> configuredInstall(crossinline apply: L.() -> Unit = {}): Endpoint<T> =
        install { -> L::class.instantiate(apply) }

    /**
     * Installs a new endpoint with a provided listener type.
     *
     * @return The installed endpoint.
     * @param L The type of the listener.
     */
    inline fun <reified L : Listener> install(vararg args: Any): Endpoint<T> =
        install { -> L::class.instantiateWith(*args) }

    /**
     * Retrieves the installed listener of the given type.
     *
     * @return The installed listener of type [L], or null if no listener of that type is installed.
     * @throws ClassCastException if the installed listener cannot be cast to type [L].
     *
     * @param L The type of the listener to retrieve.
     */
    inline fun <reified L : Listener> installed(): L = typedListeners[L::class] as L

    /**
     * Installs a listener of the given type and applies the provided listener function.
     *
     * @param listener The listener to be installed.
     * @return The endpoint with the installed listener.
     */
    inline fun <reified L : Listener> installed(crossinline listener: L.() -> Unit) = installed<L>().apply(listener)

    inline fun <reified L : Listener, R> installed(crossinline listener: L.() -> R) = installed<L>().listener()

    inline operator fun <reified L : Listener, R> get(crossinline listener: L.() -> R) = installed<L>().listener()

    /**
     * Listens for network events using the provided NetListener and returns the current Endpoint.
     *
     * @param listener The NetListener used to listen for network events.
     * @return The current Endpoint after listening for network events.
     */
    fun install(listener: () -> Listener): Endpoint<T> {
        delayedListeners.add(ListenerProvider(listener))
        return this
    }

    /**
     * Installs a listener to the endpoint.
     *
     * @param listener The listener to be installed.
     * @return The endpoint with the installed listener.
     */
    fun install(listener: Listener): Endpoint<T> {
        delayedListeners.add(ListenerProvider { listener })
        return this
    }

    /**
     * Sets the network listener for this Endpoint.
     *
     * @param packetListener the network listener that will handle incoming packets
     * @return the updated Endpoint instance
     */
    fun install(packetListener: (Packet, UUID) -> Unit): Endpoint<T> {
        delayedListeners.add(ListenerProvider {
            NetReceiver { packet, from ->
                packetListener(packet, from)
            }
        })
        return this
    }

    /**
     * Listens for network events using the provided NetListener and returns the current Endpoint.
     *
     * @param netListener The NetListener used to listen for network events.
     * @return The current Endpoint after listening for network events.
     */
    fun install(netListener: (Packet) -> Unit): Endpoint<T> {
        delayedListeners.add(ListenerProvider {
            NetReceiver { packet, _ ->
                netListener(packet)
            }
        })
        return this
    }

    /**
     * Executes the provided [tellListener] function for each element in [listeners].
     *
     * @param tellListener the function to be executed for each element in [listeners].
     *
     * @see Listener
     */
    fun tellListeners(tellListener: Listener.() -> Unit) = listeners.forEach(tellListener)

    /**
     * Sends the given packet to a specified ID. If no ID is specified, the packet will be sent to all connected endpoints.
     * If the packet is not sent, this method will return 0. Otherwise, it will return the number of endpoints that the packet was sent to.
     *
     * @param packet The packet to send. This packet will be serialized and sent to the specified ID.
     * @param id The ID of the recipient (optional). If no ID is specified, the packet will be sent to all connected endpoints.
     */
    abstract fun send(packet: Packet, id: Connection? = null)

    /**
     * Sends the given packet to the specified UUID.
     *
     * @param packet The packet to send.
     * @param uuid The UUID of the recipient.
     */
    fun send(packet: Packet, uuid: UUID) = send(packet, get(uuid))

    /**
     * Sends the given packet to all connected endpoints.
     */
    abstract fun sendToAll(packet: Packet, vararg exclude: UUID)
    /**
     * Listens for network events by registering a network listener.
     *
     * @param netListener The network listener to be registered.
     */
    protected fun listen(netListener: Listener): Unit = synchronized(listeners) {
        listeners.add(netListener)
        typedListeners[netListener::class] = netListener
    }

    /**
     * Registers a listener for incoming packets.
     *
     * @param listening The listener function to be invoked when a packet is received.
     */
    protected fun listen(listening: (Packet, UUID) -> Unit) = listen(NetReceiver { packet, from ->
        listening(packet, from)
    })

    /**
     * Receives a packet from the specified connection.
     *
     * @param id The connection from which to receive the packet. Default value is null.
     * @return The received packet, or null if no packet is received.
     */
    open fun receive(id: Connection? = null): Packet? = null

    /**
     * Retrieves the Connection associated with the specified connection ID.
     *
     * @param connectionID The ID of the connection to retrieve.
     * @return The Connection object associated with the given connection ID, or null if no such connection exists.
     */
    abstract operator fun get(connectionID: UUID): Connection?


    /**
     * Disconnects the specified user by their ID.
     *
     * @param id the ID of the user to be disconnected
     */
    open fun disconnected(id: Connection) = Unit

    /**
     * Starts listening for ticks and invokes the specified listener on every tick.
     *
     * @param listener the listener to be invoked on every tick
     *                 The listener is a lambda function that takes a single parameter
     *                 representing the time elapsed since the last tick.
     *                 The parameter is a floating-point number (in seconds).
     */
    fun tick(listener: (delta: Float, tick: Int) -> Unit) = listen(object : Listener {
        /**
         * Called when a packet is received.
         *
         * @param packet the packet that was received
         */
        override fun onPacket(packet: Packet, from: UUID) = Unit

        /**
         * Executes the onTick event.
         *
         * @param delta The time interval since the last tick, in seconds.
         */
        override fun onTick(delta: Float, tick: Int) {
            listener(delta, tick)
        }
    })

    /**
     * Disconnects a client with the given UUID.
     *
     * @param uuid The UUID of the client to disconnect.
     */
    fun disconnect(uuid: UUID) {
        disconnected(get(uuid) ?: return)
    }

    companion object {


        private val logger = KotlinLogging.logger { }

        val client: Client get() = Client

        val server: Server get() = Server

        /**
         * Invokes the `invoke` method on the instance.
         *
         * @return the `Endpoint` returned by the `get` method of the instance.
         */

        fun get(side: Side): Endpoint<*> = when (side) {
            Side.SERVER -> server
            Side.CLIENT -> client
        }

        /**
         * Checks if a listener of type [L] is installed.
         *
         * @return `true` if a listener of type [L] is installed, otherwise `false`.
         * @throws NoSuchElementException if [L] does not implement any listener interface.
         */
        inline fun <reified L : Listener> installed(side: Side = Side.CLIENT) = get(side).installed<L>()


    }

    /**
     * Represents an ID that consists of a UUID and a Type.
     *
     * @property uuid The UUID associated with the ID.
     * @property side The Type associated with the ID.
     */
    data class Connection(var uuid: UUID, val side: Side) {


        override fun toString(): String {
            return "Connection(${uuid.toString()}, ${side.name})"
        }
    }

    /**
     * The connection type, either server or client.
     *
     * If we're a server, we'll have a client connection types in the id that is received in the [connected] and [disconnected] methods.
     */
    enum class Side {

        // The per client worker that will be internally used to track clients on the server.
        SERVER,
        // The client worker that will be internally used to track on a client connection from the server.
        CLIENT;

        /**
         * Retrieves the opposite type of the current type.
         *
         * @return The opposite type, either SERVER or CLIENT.
         */
        val adversary: Side get() = if (this == SERVER) CLIENT else SERVER
    }
}

