package bpm.pipe.proxy

import bpm.common.memory.Buffer
import bpm.common.serial.Serial
import bpm.common.serial.Serialize
import net.minecraft.core.BlockPos

data class ProxyState(
    // The real world origin of the proxy block, used to calculate the relative position of the blocks
    val origin: BlockPos = BlockPos.ZERO,
    // The blocks that are proxied by the proxy block, with their relative positions
    val proxiedBlocks: MutableMap<BlockPos, ProxiedState> = mutableMapOf()
) {

    fun getProxiedState(pos: BlockPos, relative: Boolean = true): ProxiedState {
        return if (!relative) {
            //If the position is not relative, subtract the origin to make it relative
            val newPos = pos.subtract(origin)
            proxiedBlocks[newPos] ?: ProxiedState()
        } else proxiedBlocks[pos] ?: ProxiedState()
    }

    operator fun get(pos: BlockPos): ProxiedState = getProxiedState(pos)

    //This should be a position relative to the origin of the proxy block
    fun setProxiedState(pos: BlockPos, state: ProxiedState, relative: Boolean = true) {
        if (!relative) {
            //If the position is not relative, subtract the origin to make it relative
            val newPos = pos.subtract(origin)
            proxiedBlocks[newPos] = state
        } else proxiedBlocks[pos] = state
    }

    operator fun set(pos: BlockPos, state: ProxiedState) = setProxiedState(pos, state)


    object ProxyStateSerializer : Serialize<ProxyState>(ProxyState::class) {

        override fun deserialize(buffer: Buffer): ProxyState {
            val origin = buffer.readBlockPos()
            val proxiedBlocks = mutableMapOf<BlockPos, ProxiedState>()
            val size = buffer.readInt()
            for (i in 0 until size) {
                val pos = buffer.readBlockPos()
                val state: ProxiedState = Serial.read(buffer) ?: error("Failed to read ProxiedState from buffer!")
                proxiedBlocks[pos] = state
            }
            return ProxyState(origin, proxiedBlocks)
        }

        override fun serialize(buffer: Buffer, value: ProxyState) {
            buffer.writeBlockPos(value.origin)
            buffer.writeInt(value.proxiedBlocks.size)
            value.proxiedBlocks.forEach { (pos, state) ->
                buffer.writeBlockPos(pos)
                Serial.write(buffer, state)
            }
        }

    }

}