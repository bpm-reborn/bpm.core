package bpm.mc.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import net.minecraft.core.BlockPos

data class RequestPipeNetworkPacket(var blockPos: BlockPos = BlockPos.ZERO) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeInt(blockPos.x)
        buffer.writeInt(blockPos.y)
        buffer.writeInt(blockPos.z)
    }

    override fun deserialize(buffer: Buffer) {
        blockPos = BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt())
    }
}