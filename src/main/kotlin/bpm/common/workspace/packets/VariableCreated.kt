package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import bpm.common.property.Property
import bpm.common.property.PropertyMap
import bpm.common.serial.Serial

data class VariableCreated(
    var name: String = "",
    var property: PropertyMap = Property.Object()
) : Packet {

    override fun serialize(buffer: Buffer) {
        buffer.writeString(name)
        Serial.write(buffer, property)
    }

    override fun deserialize(buffer: Buffer) {
        name = buffer.readString()
        property = Serial.read(buffer) ?: Property.Object()
    }

}