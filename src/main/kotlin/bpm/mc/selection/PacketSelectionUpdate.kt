package bpm.mc.selection

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import net.minecraft.core.BlockPos
import java.util.*

class PacketSelectionUpdate(
    var uuid: UUID = UUID.randomUUID(),
    var blockPos: BlockPos? = null,
    var entityId: Int? = null,
    var start: BlockPos? = null,
    var end: BlockPos? = null
) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(uuid)

        // Write flags for which fields are present
        val hasBlockPos = blockPos != null
        val hasEntityId = entityId != null
        val hasStartEnd = start != null && end != null

        buffer.writeBoolean(hasBlockPos)
        buffer.writeBoolean(hasEntityId)
        buffer.writeBoolean(hasStartEnd)

        // Write fields based on flags
        if (hasBlockPos) {
            buffer.writeBlockPos(blockPos!!)
        }

        if (hasEntityId) {
            buffer.writeInt(entityId!!)
        }

        if (hasStartEnd) {
            buffer.writeBlockPos(start!!)
            buffer.writeBlockPos(end!!)
        }
    }

    override fun deserialize(buffer: Buffer) {
        uuid = buffer.readUUID()

        // Read flags
        val hasBlockPos = buffer.readBoolean()
        val hasEntityId = buffer.readBoolean()
        val hasStartEnd = buffer.readBoolean()

        // Read fields based on flags
        blockPos = if (hasBlockPos) buffer.readBlockPos() else null
        entityId = if (hasEntityId) buffer.readInt() else null

        if (hasStartEnd) {
            start = buffer.readBlockPos()
            end = buffer.readBlockPos()
        }


    }
}