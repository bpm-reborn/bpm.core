package bpm.server.lua

import bpm.common.network.Network.new
import bpm.common.network.Server
import bpm.common.workspace.packets.NotifyMessage

object Notify : LuaBuiltin {

    @JvmStatic
    fun info(msg: String, time: Float) {
        Server.sendToAll(new<NotifyMessage> {
            icon = 0x0021
            message = msg
            color = "#4287f5"
            lifetime = time
            type = NotifyMessage.NotificationType.INFO
        })
    }

    override val name: String = "Notify"
}
