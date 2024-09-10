package bpm.pipe.proxy

import bpm.common.memory.Buffer
import bpm.common.serial.Serialize
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import thedarkcolour.kotlinforforge.neoforge.kotlin.enumMapOf
import java.util.EnumMap

data class ProxiedState(
    //A position relative to the origin of the proxy block
    var relativePos: BlockPos = BlockPos.ZERO,


    val proxiedFaces: MutableMap<Direction, ProxiedType> = enumMapOf(
        Direction.NORTH to ProxiedType.NONE,
        Direction.EAST to ProxiedType.NONE,
        Direction.SOUTH to ProxiedType.NONE,
        Direction.WEST to ProxiedType.NONE,
        Direction.UP to ProxiedType.NONE,
        Direction.DOWN to ProxiedType.NONE
    )
) {

    fun getProxiedType(direction: Direction): ProxiedType {
        return proxiedFaces[direction] ?: ProxiedType.NONE
    }

    fun setProxiedType(direction: Direction, type: ProxiedType) {
        proxiedFaces[direction] = type
    }

    //Serializer for ProxiedState
    object ProxiedStateSerializer : Serialize<ProxiedState>(ProxiedState::class) {

        override fun deserialize(buffer: Buffer): ProxiedState {
            val relativePos = buffer.readBlockPos()
            val proxiedFaces: MutableMap<Direction, ProxiedType> = EnumMap(Direction::class.java)

            for (direction in Direction.entries) {
                val type = ProxiedType.entries[buffer.readInt()]
                proxiedFaces[direction] = type
            }
            return ProxiedState(relativePos, proxiedFaces)
        }

        override fun serialize(buffer: Buffer, value: ProxiedState) {
            buffer.writeBlockPos(value.relativePos)
            for (direction in Direction.entries) {
                buffer.writeInt(value.proxiedFaces[direction]!!.ordinal)
            }
        }
    }
}