package bpm.pipe.proxy

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import net.minecraft.core.BlockPos

//Sent whenever a proxy block at a given position was updated
class PacketProxyUpdate(
    //The world position of the proxy block
    var proxyOrigin: BlockPos = BlockPos.ZERO,
    //The new proxied state
    var proxiedState: ProxiedState = ProxiedState()
) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeBlockPos(proxyOrigin)
        Serial.write(buffer, proxiedState)
    }

    override fun deserialize(buffer: Buffer) {
        proxyOrigin = buffer.readBlockPos()
        proxiedState = Serial.read(buffer) ?: error("Failed to read ProxiedState from buffer!")
    }
}