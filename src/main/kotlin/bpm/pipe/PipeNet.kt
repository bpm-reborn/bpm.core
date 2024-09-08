package bpm.pipe

import bpm.common.logging.KotlinLogging
import bpm.common.memory.Buffer
import bpm.common.serial.Serialize
import bpm.mc.block.BasePipeBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import kotlin.reflect.KClass
import bpm.mc.block.EnderControllerBlock
import bpm.mc.block.EnderControllerTileEntity
import bpm.network.level
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID


// A serializable representation of a pipe
data class TrackedPipe(
    val level: ResourceKey<Level> = Level.OVERWORLD,
    val world: BlockPos = BlockPos.ZERO,
    val relative: BlockPos = BlockPos.ZERO,
    val type: Class<out BasePipeBlock> = BasePipeBlock::class.java
)

object TrackedPipeSerializer : Serialize<TrackedPipe>(TrackedPipe::class) {

    override fun deserialize(buffer: Buffer): TrackedPipe {
        val level = buffer.readResourceKey<Level>()
        val world = buffer.readBlockPos()
        val relative = buffer.readBlockPos()
        val type = buffer.readClass() as Class<out BasePipeBlock>
        return TrackedPipe(level, world, relative, type)
    }

    override fun serialize(buffer: Buffer, value: TrackedPipe) {
        buffer.writeResourceKey(value.level)
        buffer.writeBlockPos(value.world)
        buffer.writeBlockPos(value.relative)
        buffer.writeClass(value.type)
    }
}


data class PipeNet(var pipes: MutableMap<BlockPos, TrackedPipe> = mutableMapOf()) {

    private val logger = KotlinLogging.logger {}

    fun addPipe(pipe: Class<out BasePipeBlock>, level: Level, pos: BlockPos) {
        pipes[pos] = TrackedPipe(level.dimension(), pos, pos, pipe)
    }

    fun removePipe(level: Level, pos: BlockPos) {
        pipes.remove(pos)
    }

    fun contains(level: Level, pos: BlockPos): Boolean = pipes.containsKey(pos)

    fun isEmpty(): Boolean = pipes.isEmpty()

    fun split(level: Level, removedPos: BlockPos): List<PipeNet> {
        val newNetworks = mutableListOf<PipeNet>()
        val remainingPipes = pipes.toMap()
        val processed = mutableSetOf<BlockPos>()
        processed.add(removedPos)

        for ((pos, _) in remainingPipes) {
            if (pos in processed) continue
            val connectedPipes = findConnectedPipes(pos, remainingPipes)
            if (connectedPipes.isNotEmpty()) {
                val newNetwork = PipeNet()
                connectedPipes.forEach { (connectedPos, _) ->
                    val pipeBlock = level.getBlockState(connectedPos).block
                    if (pipeBlock is BasePipeBlock) {
                        newNetwork.addPipe(pipeBlock.javaClass, level, connectedPos)
                    }
                    processed.add(connectedPos)
                }
                newNetworks.add(newNetwork)
            }
        }

        return newNetworks
    }

    private fun findConnectedPipes(
        pos: BlockPos, remainingPipes: Map<BlockPos, TrackedPipe>
    ): Map<BlockPos, TrackedPipe> {
        return Direction.entries.mapNotNull { dir ->
            val connectedPos = pos.relative(dir)
            remainingPipes[connectedPos]?.let { connectedPipe ->
                if (connectedPipe.relative == connectedPos) {
                    connectedPos to connectedPipe
                } else {
                    null
                }
            }
        }.toMap()
    }
}

object PipeNetSerializer : Serialize<PipeNet>(PipeNet::class) {

    override fun deserialize(buffer: Buffer): PipeNet {
        val pipes = mutableMapOf<BlockPos, TrackedPipe>()
        val size = buffer.readInt()
        repeat(size) {
            val pos = buffer.readBlockPos()
            val pipe = TrackedPipeSerializer.deserialize(buffer)
            pipes[pos] = pipe
        }
        return PipeNet(pipes)
    }

    override fun serialize(buffer: Buffer, value: PipeNet) {
        buffer.writeInt(value.pipes.size)
        value.pipes.forEach { (pos, pipe) ->
            buffer.writeBlockPos(pos)
            TrackedPipeSerializer.serialize(buffer, pipe)
        }
    }
}

