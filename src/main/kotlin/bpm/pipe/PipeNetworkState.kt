package bpm.pipe

import bpm.common.logging.KotlinLogging
import bpm.common.memory.Buffer
import bpm.common.serial.Serialize
import bpm.mc.block.BasePipeBlock
import bpm.pipe.proxy.ProxyState
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import java.util.*
import java.util.concurrent.ConcurrentHashMap


// A serializable representation of a pipe
data class TrackedPipe(
    val level: ResourceKey<Level> = Level.OVERWORLD,
    val worldPos: BlockPos = BlockPos.ZERO,
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
        buffer.writeBlockPos(value.worldPos)
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

data class PipeNetManagerState(
    val networks: MutableMap<UUID, PipeNet> = ConcurrentHashMap(),
    val blockPosToNetwork: MutableMap<BlockPos, UUID> = ConcurrentHashMap(),
    val controllerToNetwork: MutableMap<UUID, UUID> = ConcurrentHashMap(),
    val proxies: MutableMap<BlockPos, ProxyState> = ConcurrentHashMap()
)

object PipeNetManagerStateSerializer : Serialize<PipeNetManagerState>(PipeNetManagerState::class) {

    override fun deserialize(buffer: Buffer): PipeNetManagerState {
        val networksSize = buffer.readInt()
        val networks = (0 until networksSize).associate {
            UUID.fromString(buffer.readString()) to PipeNetSerializer.deserialize(buffer)
        }

        val blockPosToNetworkSize = buffer.readInt()
        val blockPosToNetwork = (0 until blockPosToNetworkSize).associate {
            buffer.readBlockPos() to UUID.fromString(buffer.readString())
        }

        val controllerToNetworkSize = buffer.readInt()
        val controllerToNetwork = (0 until controllerToNetworkSize).associate {
            UUID.fromString(buffer.readString()) to UUID.fromString(buffer.readString())
        }

        val proxiesSize = buffer.readInt()
        val proxies = (0 until proxiesSize).associate {
            buffer.readBlockPos() to ProxyState.ProxyStateSerializer.deserialize(buffer)
        }
        return PipeNetManagerState(
            networks.toMutableMap(),
            blockPosToNetwork.toMutableMap(),
            controllerToNetwork.toMutableMap(),
            proxies.toMutableMap()
        )
    }

    override fun serialize(buffer: Buffer, value: PipeNetManagerState) {
        buffer.writeInt(value.networks.size)
        value.networks.forEach { (uuid, pipeNet) ->
            buffer.writeString(uuid.toString())
            PipeNetSerializer.serialize(buffer, pipeNet)
        }

        buffer.writeInt(value.blockPosToNetwork.size)
        value.blockPosToNetwork.forEach { (blockPos, uuid) ->
            buffer.writeBlockPos(blockPos)
            buffer.writeString(uuid.toString())
        }

        buffer.writeInt(value.controllerToNetwork.size)
        value.controllerToNetwork.forEach { (controllerUuid, networkUuid) ->
            buffer.writeString(controllerUuid.toString())
            buffer.writeString(networkUuid.toString())
        }

        buffer.writeInt(value.proxies.size)
        value.proxies.forEach { (pos, proxyState) ->
            buffer.writeBlockPos(pos)
            ProxyState.ProxyStateSerializer.serialize(buffer, proxyState)
        }
    }
}