package bpm.common.network

import java.util.*

object NetUtils {

    /**
     * DefaultUUID is a constant variable representing a default UUID value.
     *
     * @property DefaultUUID The default UUID value represented by this constant.
     */
    val DefaultUUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")


    /**
     * Checks if the given UUID is the default UUID.
     *
     * @param uuid The UUID to be checked.
     * @return true if the given UUID is the default UUID, false otherwise.
     */
    fun isDefaultUUID(uuid: UUID): Boolean {
        return uuid == DefaultUUID
    }
}