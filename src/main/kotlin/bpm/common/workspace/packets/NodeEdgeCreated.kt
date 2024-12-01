package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import bpm.common.workspace.graph.Edge
import java.util.UUID

data class NodeEdgeCreated(var uid: UUID = NetUtils.DefaultUUID, var edge: Edge = Edge()) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(uid)
        Serial.write(buffer, edge)
    }

    override fun deserialize(buffer: Buffer) {
        uid = buffer.readUUID()
        edge = Serial.read(buffer) ?: error("Failed to deserialize edge")
    }
}