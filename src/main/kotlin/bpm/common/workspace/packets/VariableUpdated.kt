package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import bpm.common.property.Property
import bpm.common.property.PropertyMap
import bpm.common.serial.Serial


data class VariableUpdated(
    var variableName: String = "",
    var property: PropertyMap = Property.Object()
) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        buffer.writeString(variableName)
        Serial.write(buffer, property)
    }


    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        variableName = buffer.readString()
        property = Serial.read(buffer) ?: Property.Object()
    }
}