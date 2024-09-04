package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import bpm.common.property.PropertyMap
import bpm.common.serial.Serial
import bpm.common.workspace.graph.Link

data class LinkCreateRequest(var link: Link = Link()) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        Serial.write(buffer, link.properties)
    }


    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        link.properties.clear()
        val properties: PropertyMap = Serial.read(buffer) ?: error("Failed to read properties from buffer!")
        link.properties.putAll(properties)
    }
}