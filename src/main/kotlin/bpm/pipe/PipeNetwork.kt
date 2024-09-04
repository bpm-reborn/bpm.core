package bpm.pipe
import bpm.mc.block.BasePipeBlock
import bpm.mc.block.EnderControllerBlock
import bpm.mc.block.EnderControllerTileEntity
import java.util.concurrent.ConcurrentHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import bpm.common.logging.KotlinLogging



class PipeNetwork {
    data class LevelPipe(val level: Level, val pipe: BasePipeBlock, val pos: BlockPos)

    val pipes = ConcurrentHashMap<BlockPos, LevelPipe>()
    private val logger = KotlinLogging.logger {}

    fun addPipe(pipe: BasePipeBlock, level: Level, pos: BlockPos) {
        pipes[pos] = LevelPipe(level, pipe, pos)
        logger.debug { "Added pipe at $pos to network" }
        if(pipe is EnderControllerBlock) {
            val tile = level.getBlockEntity(pos)
            if(tile is EnderControllerTileEntity) {
//                tile.network = this
                logger.debug { "Added controller at $pos to network" }
            }
        }
    }

    fun removePipe(level: Level, pos: BlockPos) {
        pipes.remove(pos)
        logger.debug { "Removed pipe at $pos from network" }
    }

    fun contains(level: Level, pos: BlockPos): Boolean = pipes.containsKey(pos)

    fun isEmpty(): Boolean = pipes.isEmpty()

    fun split(level: Level, removedPos: BlockPos): List<PipeNetwork> {
        val newNetworks = mutableListOf<PipeNetwork>()
        val remainingPipes = pipes.toMap()
        val processed = mutableSetOf<BlockPos>()

        for ((pos, levelPipe) in remainingPipes) {
            if (pos in processed) continue
            val connectedPipes = findConnectedPipes(pos, remainingPipes)
            if (connectedPipes.isNotEmpty()) {
                val newNetwork = PipeNetwork()
                connectedPipes.forEach { (connectedPos, connectedLevelPipe) ->
                    newNetwork.addPipe(connectedLevelPipe.pipe, level, connectedPos)
                    processed.add(connectedPos)
                }
                newNetworks.add(newNetwork)
            }
        }

        logger.info { "Network split into ${newNetworks.size} networks" }
        return newNetworks
    }

    private fun findConnectedPipes(
        start: BlockPos,
        availablePipes: Map<BlockPos, LevelPipe>
    ): Map<BlockPos, LevelPipe> {
        val connectedPipes = mutableMapOf<BlockPos, LevelPipe>()
        val queue = ArrayDeque<BlockPos>()
        queue.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val currentPipe = availablePipes[current] ?: continue
            connectedPipes[current] = currentPipe

            for (direction in Direction.entries) {
                val neighborPos = current.relative(direction)
                if (neighborPos in availablePipes && neighborPos !in connectedPipes) {
                    queue.add(neighborPos)
                }
            }
        }

        return connectedPipes
    }

    fun cleanupGhostPipes() {
        val ghostPipes = pipes.entries.filter { (pos, pipe) ->
            val blockState = pipe.level.getBlockState(pos)
            blockState.block !is BasePipeBlock
        }
        ghostPipes.forEach { (pos, pipe) ->
            removePipe(pipe.level, pos)
            logger.warn { "Removed ghost pipe at $pos" }
        }
    }


}