package bpm.mc.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import bpm.pipe.PipeNetwork
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level

//Enough information to let the client reconstruct the pipe network
data class PipeNetworkResponsePacket(
    var level: ResourceKey<Level> = Level.OVERWORLD,
    var pipes: List<BlockPos>
) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeString(level.location().toString())
        buffer.writeInt(pipes.size)
        pipes.forEach {
            buffer.writeInt(it.x)
            buffer.writeInt(it.y)
            buffer.writeInt(it.z)
        }
    }

    override fun deserialize(buffer: Buffer) {
        level = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(buffer.readString())!!)
        val size = buffer.readInt()
        pipes = List(size) {
            BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt())
        }
    }
}