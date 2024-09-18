package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import net.minecraft.core.BlockPos
import org.joml.Vector2f

class ProxyNodeCreateRequest(var position: Vector2f = Vector2f(), var blockPos: BlockPos = BlockPos.ZERO) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeFloat(position.x)
        buffer.writeFloat(position.y)
        buffer.writeBlockPos(blockPos)
    }

    override fun deserialize(buffer: Buffer) {
        position.x = buffer.readFloat()
        position.y = buffer.readFloat()
        blockPos = buffer.readBlockPos()
    }
}