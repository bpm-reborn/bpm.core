package bpm.pipe.proxy

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import java.util.UUID

//Asks the server to send the proxied state of a proxy block
class PacketProxyRemoveWorkspace(
    //The world position of the proxy block
    var workspace: UUID = NetUtils.DefaultUUID
) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(workspace)
    }

    override fun deserialize(buffer: Buffer) {
        workspace = buffer.readUUID()
    }
}