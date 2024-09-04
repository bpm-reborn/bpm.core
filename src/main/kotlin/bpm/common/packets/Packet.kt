package bpm.common.packets

import bpm.common.memory.Buffer

interface Packet {

    /**
     * Represents the ID of an object.
     *
     * The ID is generated based on the class name using the `hashCode()` function of the `Class` object.
     * It uniquely identifies an instance of a class.
     *
     * @property id The generated ID.
     */
    val id: Int
        get() = this::class.java.name.hashCode()

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    fun serialize(buffer: Buffer)

    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    fun deserialize(buffer: Buffer)
}