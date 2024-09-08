package bpm.pipe.proxy

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import net.minecraft.core.BlockPos

//Sent as a response to a PacketProxyRequest
class PacketProxyResponse(
    //The world position of the proxy block
    var proxyOrigin: BlockPos = BlockPos.ZERO,
    //The proxy state
    var proxyState: ProxyState = ProxyState()
) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeBlockPos(proxyOrigin)
        Serial.write(buffer, proxyState)
    }

    override fun deserialize(buffer: Buffer) {
        proxyOrigin = buffer.readBlockPos()
        proxyState = Serial.read(buffer) ?: error("Failed to read ProxiedState from buffer!")
    }
}