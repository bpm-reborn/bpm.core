package bpm.common.packets.internal

import bpm.common.memory.Buffer
import bpm.common.packets.Packet

/**
 * Represents a heartbeat packet used for checking the availability of a network connection.
 *
 * @property timestamp The timestamp of the heartbeat.
 * @property timeout The timeout of the heartbeat.
 */
class Heartbeat(
    var timestamp: Time = Time.now,
    var timeout: Int = 12,
) : Packet {

    /**
     * Reschedules the checkup by the specified number of seconds
     *
     * @param seconds the number of seconds to reschedule the checkup by
     */
    fun reschedule(seconds: Int = timeout) {
        timestamp = Time(Time.now.time + seconds * 1000)
    }

    /**
     * Checks whether the scheduled checkup is expired.
     *
     * @return True if the scheduled checkup is expired, false otherwise.
     */
    fun isExpired(): Boolean = timestamp.time + timeout * 1000 < Time.now.time

    /**
     * Checks if the timestamp of the current object, when added with the timeout value,
     * minus the safety margin of 5000 milliseconds, is less than the current time.
     *
     * @return true if the object is almost expired, otherwise false.
     */
    fun isAlmostExpired(expireInSeconds: Int = 1): Boolean =
        timestamp.time + timeout * 1000 - expireInSeconds * 1000 < Time.now.time

    /**
     * Represents the remaining time until expiration.
     *
     * The `expiresIn` variable calculates the remaining time until expiration based on the timestamp,
     * timeout duration, and the current time.
     *
     * @return [Time] object indicating the remaining time until expiration in milliseconds.
     */
    val expiresIn: Time
        get() = Time(timestamp.time + timeout * 1000 - Time.now.time)

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeLong(timestamp.time)
        buffer.writeInt(timeout)
    }

    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        val time = buffer.readLong()
        val timeout = buffer.readInt()
        timestamp = Time(time)
        this.timeout = timeout
    }

    companion object {

        //TODO: Maybe make this configurable and/or longer? We could also make it reset every time a packet is received.
        const val HEARTBEAT_TIMEOUT_SECONDS: Int = 12
    }
}