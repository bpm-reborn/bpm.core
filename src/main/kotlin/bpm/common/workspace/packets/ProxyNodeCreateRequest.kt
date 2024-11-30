package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import org.joml.Vector2f

class ProxyNodeCreateRequest(
    var position: Vector2f = Vector2f(),
    var blockPos: BlockPos = BlockPos.ZERO,
    var level: ResourceKey<Level> = Level.OVERWORLD
) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeFloat(position.x)
        buffer.writeFloat(position.y)
        buffer.writeBlockPos(blockPos)
        buffer.writeResourceKey(level)
    }

    override fun deserialize(buffer: Buffer) {
        position.x = buffer.readFloat()
        position.y = buffer.readFloat()
        blockPos = buffer.readBlockPos()
        level = buffer.readResourceKey()
    }
}