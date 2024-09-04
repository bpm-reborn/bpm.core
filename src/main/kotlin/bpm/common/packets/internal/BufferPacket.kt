package bpm.common.packets.internal

import bpm.common.logging.KotlinLogging
import bpm.common.memory.Buffer
import bpm.common.network.Network
import bpm.common.packets.Packet
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

object BufferPacket {

    /**
     * Writes a packet to the specified output stream.
     *
     * @param packet the packet to write
     * @param stream the output stream to write to
     */
    fun write(packet: Packet, stream: OutputStream) {
        val buffer = Buffer.allocate()
        packet.serialize(buffer)
        val writeSize = buffer.position
        val infoBuffer = Buffer.allocate(Int.SIZE_BYTES * 2)
        infoBuffer.writeInt(packet.id) //write the id
        infoBuffer.writeInt(writeSize) //write the size
        stream.write(infoBuffer.finish()) //write the buffer
        stream.write(buffer.finish())
        stream.flush()
        logger.debug { "Wrote $writeSize bytes for id ${packet.id} of type ${packet::class.java.simpleName}" }
    }

    /**
     * Reads a packet from the given input stream.
     *
     * TODO:
     *
     * @param stream the input stream to read from
     * @return the read packet, or null if the input stream does not have enough data to read a complete packet
     */
    fun read(stream: InputStream): Packet? {
        val infoBuffer = ByteArray(8)
        if (stream.read(infoBuffer) != 8) return null // Ensure we read 8 bytes for ID and size

        val infoWrap = ByteBuffer.wrap(infoBuffer)
        val id = infoWrap.int
        val size = infoWrap.int

        val packet = Network.new(id) ?: return null
        logger.debug { "Reading $size bytes for id $id of type ${packet::class.java.simpleName}" }

        val buffer = ByteArray(size)
        var totalRead = 0
        while (totalRead < size) {
            val read = stream.read(buffer, totalRead, size - totalRead)
            if (read == -1) break // End of stream reached
            totalRead += read
        }

        if (totalRead != size) {
            logger.error { "Expected to read $size bytes but only read $totalRead" }
            return null
        }

        val dataBuffer = Buffer.wrap(buffer)
        packet.deserialize(dataBuffer)
        return packet
    }


    val logger = KotlinLogging.logger {}
}


fun Packet.write(stream: OutputStream) = BufferPacket.write(this, stream)

fun InputStream.readPacket(): Packet? = BufferPacket.read(this)