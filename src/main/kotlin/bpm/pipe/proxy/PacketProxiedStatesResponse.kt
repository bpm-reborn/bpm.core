package bpm.pipe.proxy

import bpm.common.memory.Buffer
import bpm.common.memory.readList
import bpm.common.memory.writeList
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import java.util.UUID

/**
 * Used to request all proxies for the given workspace uuid
 */
class PacketProxiedStatesResponse(
    var workspace: UUID = NetUtils.DefaultUUID,
    val proxiedStates: MutableList<ProxyState> = mutableListOf()
) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(workspace)
        buffer.writeInt(proxiedStates.size)
        for (state in proxiedStates) {
            Serial.write(buffer, state)
        }
    }

    override fun deserialize(buffer: Buffer) {
        workspace = buffer.readUUID()
        val size = buffer.readInt()
        repeat(size) {
            proxiedStates.add(Serial.read(buffer) ?: error("Failed to read ProxyState from buffer!"))
        }
    }
}