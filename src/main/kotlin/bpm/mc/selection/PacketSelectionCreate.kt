package bpm.mc.selection

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import bpm.common.serial.Serial

class PacketSelectionCreate(
    var data: SelectionManager.SelectionData = SelectionManager.SelectionData(
        uuid = java.util.UUID.randomUUID(),
        selectionType = SelectionManager.SelectionType.BLOCK,
        blockPos = null,
        entityId = null,
        start = null,
        end = null
    )
) : Packet {

    override fun serialize(buffer: Buffer) {
        Serial.write(buffer, data)
    }

    override fun deserialize(buffer: Buffer) {
        data = Serial.read(buffer) ?: error("Failed to deserialize SelectionData")
    }
}