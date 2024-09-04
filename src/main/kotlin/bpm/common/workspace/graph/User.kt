package bpm.common.workspace.graph

import bpm.common.logging.KotlinLogging
import bpm.common.memory.Buffer
import bpm.common.network.NetUtils
import bpm.common.serial.Serialize
import org.joml.Vector2f
import org.joml.Vector4f
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * @param uid The universally unique identifier (UUID) for a user.
 * @param name The name of the user.
 * @param workspaceUid The opened workspace for the user.
 */
data class User(
    /**
     * The universally unique identifier (UUID) for a user.
     */
    var uid: UUID = NetUtils.DefaultUUID,
    /**
     * The name of the user.
     */
    var name: String? = null,
    /**
     * The opened workspace for the user.
     */
    var workspaceUid: UUID = NetUtils.DefaultUUID
) {

    val username: String = name ?: pullUsername()

    val avatar: ByteArray = getAvatarBytes()

    private fun pullUsername(): String {
//        //send Http request to https://api.mojang.com/user/profile/$uuid
//        val connection = URL(
//            "https://api.mojang.com/user/profile/${
//                uid.toString().replace("-", "")
//            }"
//        ).openConnection() as HttpURLConnection
//        connection.requestMethod = "GET"
//
//        // handle the response
//        return if (connection.responseCode == 200) {
//            val response = connection.inputStream.bufferedReader().use { it.readText() }
//            val jsonObject = JSONTokener(response).nextValue() as JSONObject
//            val name = jsonObject.getString("name")
//            logger.debug { "Successfully pulled username for $uid, [$name]" }
//            name
//        } else {
////            throw IllegalStateException("Unexpected response code: ${connection.responseCode}")
//            logger.error { "Failed to pull username for $uid, using UUID instead" }
//            uid.toString()
//        }
        return uid.toString()
    }


    private fun getAvatarBytes(): ByteArray {
        val connection = URL(
            "https://mc-heads.net/avatar/${
                uid.toString().replace("-", "")
            }/128"
        ).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        // handle the response
        return if (connection.responseCode == 200) {
            val result = connection.inputStream.readAllBytes()
            logger.debug { "Successfully pulled avatar for $username" }
            result
        } else {
            throw IllegalStateException("Unexpected response code: ${connection.responseCode}")
        }
    }

    /**
     * Represents a 2D cursor position in a Cartesian coordinate system.
     * The cursor is defined by its X and Y coordinates.
     *
     * @property x The X coordinate of the cursor.
     * @property y The Y coordinate of the cursor.
     *
     * @constructor Creates a new instance of the [cursor] class with the default coordinates (0, 0).
     */
    val cursor = Vector2f()

    /**
     * A 4-dimensional vector representing a view frame in 3D space. This is the bounding box of the view for the user.
     *
     * As a user zooms in and out of the workspace, the view frame changes accordingly. This is a view space relative to the
     * graph space.
     *
     * @constructor Creates a new instance of the Vector4f class. Default values are (0, 0, 0, 0).
     */
    val frame: Vector4f = Vector4f()


    /** Get automagically deserialized from [Serial.read] in [Workspace.Serializer.deserialize]**/
    object Serializer : Serialize<User>(User::class) {

        /**
         * Deserializes the contents of the Buffer and returns an instance of type T.
         *
         * @return The deserialized object of type T.
         */
        override fun deserialize(buffer: Buffer): User {
            val uid = buffer.readUUID()
            val name = buffer.readString()
            val workspaceUid = buffer.readUUID()
            val user = User(uid, name, workspaceUid)
            user.cursor.x = buffer.readFloat()
            user.cursor.y = buffer.readFloat()
            user.frame.x = buffer.readFloat()
            user.frame.y = buffer.readFloat()
            user.frame.z = buffer.readFloat()
            user.frame.w = buffer.readFloat()
            return user
        }
        /**
         * Serializes the provided value into the buffer.
         *
         * @param value The value to be serialized.
         */
        override fun serialize(buffer: Buffer, value: User) {
            buffer.writeUUID(value.uid)
            buffer.writeString(value.username)
            buffer.writeUUID(value.workspaceUid)
            buffer.writeFloat(value.cursor.x)
            buffer.writeFloat(value.cursor.y)
            buffer.writeFloat(value.frame.x)
            buffer.writeFloat(value.frame.y)
            buffer.writeFloat(value.frame.z)
            buffer.writeFloat(value.frame.w)
        }

    }

    companion object {

        private val logger = KotlinLogging.logger {}
    }
}