package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.packets.Packet
import bpm.common.property.Property
import bpm.common.property.PropertyMap
import bpm.common.serial.Serial
import java.util.UUID

class EdgePropertyUpdate(var edgeUid: UUID = NetUtils.DefaultUUID, var property: PropertyMap = Property.Object()) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeUUID(edgeUid)
        Serial.write(buffer, property)
    }


    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        edgeUid = buffer.readUUID()
        property = Serial.read(buffer) ?: throw IllegalStateException("Property is null")
    }
}