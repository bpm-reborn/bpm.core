package bpm.common.workspace.packets

import bpm.common.memory.Buffer
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import bpm.common.workspace.graph.User

/**
 * This is used to tell a client that a user has connected to a workspace.
 *
 * It is also sent to the client when they connect to a workspace, so they can know who is in the workspace.
 */
class UserConnectedToWorkspace(val users: MutableList<User> = mutableListOf()) : Packet {

    /**
     * Serializes the provided Buffer.
     *
     * @param buffer The Buffer to be serialized.
     */
    override fun serialize(buffer: Buffer) {
        val userCount = users.size
        buffer.writeInt(userCount)
        repeat(userCount) {
            val user = users[it]
            Serial.write(buffer, user)
        }
    }
    /**
     * Deserializes the given buffer.
     *
     * @param buffer The buffer to deserialize.
     */
    override fun deserialize(buffer: Buffer) {
        val userCount = buffer.readInt()
        repeat(userCount) {
            val user = Serial.read<User>(buffer) ?: error("Failed to deserialize user")
            users.add(user)
        }
    }


}