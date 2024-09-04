package bpm.common.network

import bpm.common.logging.KotlinLogging
import bpm.common.packets.Packet
import bpm.common.packets.internal.Time
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class Worker(
    /**
     * The owner of the worker.
     */
    val endpoint: Endpoint<*>,
    /**
     * The amount of timers per second that the worker should tick.
     */
    private val tickPerFrame: Int = 60
) : Runnable {

    private val thread: Thread = Thread(this, "Worker")
    private var time = Time.now
    private val packets = ConcurrentLinkedQueue<QueuedPacket>()
    private var accumulator = 0f
    internal val running = AtomicBoolean(false)

    /**
     * Variable to check if the endpoint is a client end.
     *
     * @property isClient Indicates whether the endpoint is a client end.
     */
    val isClient: Boolean = endpoint is Client

    /**
     * A boolean flag indicating whether the endpoint is a server end.
     *
     * @property isServer - Flag indicating whether the endpoint is a server end.
     */
    val isServer: Boolean = endpoint is Server

    /**
     * When an object implementing interface `Runnable` is used
     * to create a thread, starting the thread causes the object's
     * `run` method to be called in that separately executing
     * thread.
     *
     *
     * The general contract of the method `run` is that it may
     * take any action whatsoever.
     *
     * @see java.lang.Thread.run
     */
    override fun run() {
        var tick = 0
        while (endpoint.isRunning()) {
            val now = Time.now
            val delta: Float = (now - time).time / 1000f // Convert to seconds
            time = now
            accumulator += delta
            while (accumulator >= 1f / tickPerFrame) {
                accumulator -= 1f / tickPerFrame
                tick = (tick + 1) % tickPerFrame
                tick(delta, tick)
            }
        }
        logger.info { "Worker stopped" }

    }

    /**
     * Enqueues a packet to be sent over a specific connection.
     *
     * @param packet The packet to enqueue.
     * @param connection The UUID of the connection to send the packet over.
     */
    fun queue(packet: Packet, connection: UUID) = synchronized(packets) {
        packets.add(QueuedPacket(packet, connection))
    }


    /**
     * Executes the tick operation for a given delta time and tick value.
     * This method invokes the "onTick" method of all the registered listeners.
     * It also processes any pending packets in a synchronized manner.
     *
     * @param delta the time elapsed since the last tick, in seconds
     * @param tick the current tick value
     */
    private fun tick(delta: Float, tick: Int) {
        while (packets.isNotEmpty()) {
            val packet = packets.poll()
            process(packet.packet, packet.connection)
        }
        //Only tick if connected and the endpoint is a client
        endpoint.tellListeners { onTick(delta, tick) }
    }

    /**
     * Processes a packet for a given connection.
     *
     * @param packet the packet to be processed
     * @param connection the UUID of the connection
     */
    private fun process(packet: Packet, connection: UUID) = endpoint.tellListeners { onPacket(packet, connection) }


    /**
     * Starts the execution of a thread.
     *
     * This method starts the execution of the thread associated with this instance.
     */
    fun start() {
        if (running.get()) {
            logger.warn { "Worker is already running" }
            return
        }
        running.set(true)
        thread.isDaemon = true
        thread.start()
    }

    /**
     * Represents a packet that is queued for processing.
     *
     * @param packet The packet to be processed.
     * @param connection The UUID of the connection associated with the packet.
     */
    data class QueuedPacket(val packet: Packet, val connection: UUID)

    /**
     * The Companion class holds a companion object that provides logging functionality.
     *
     * @constructor Creates a new instance of Companion.
     */
    companion object {

        private val logger = KotlinLogging.logger {}
    }


}