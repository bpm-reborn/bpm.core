package bpm.pipe.proxy

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import net.minecraft.core.BlockPos

//Asks the server to send the proxied state of a proxy block
class PacketRemoveProxy(
    //The world position of the proxy block
    var proxyOrigin: BlockPos = BlockPos.ZERO,
) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeBlockPos(proxyOrigin)
    }

    override fun deserialize(buffer: Buffer) {
        proxyOrigin = buffer.readBlockPos()
    }
}