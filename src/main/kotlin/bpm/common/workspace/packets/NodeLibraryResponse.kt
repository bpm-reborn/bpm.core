package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import bpm.common.property.PropertyList
import bpm.common.serial.Serial
import bpm.common.type.NodeLibrary

data class NodeLibraryResponse(var nodeSchemas: PropertyList = NodeLibrary().collectToPropertyList()) : Packet {

    override fun serialize(buffer: Buffer) {
        Serial.write(buffer, nodeSchemas)
    }

    override fun deserialize(buffer: Buffer) {
        nodeSchemas = Serial.read(buffer) ?: error("Failed to deserialize node library")
    }
}