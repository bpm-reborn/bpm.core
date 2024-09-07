package bpm.server.lua

import bpm.common.network.Network.new
import bpm.common.network.Server
import bpm.common.workspace.packets.NotifyMessage
import org.joml.Vector4f

object Notify : LuaBuiltin {

    @JvmStatic
    fun message(msg: String, time: Float, coler: String, headerIn: String, typeIn: String) {
        Server.sendToAll(new<NotifyMessage> {
            icon = 0x0021
            message = msg
            color = coler
            lifetime = time
            header = headerIn
            type = NotifyMessage.NotificationType.valueOf(typeIn)
        })
    }

    @JvmStatic
    fun success(msg: String, time: Float, coler: String) {
        Server.sendToAll(new<NotifyMessage> {
            icon = 0x0021
            message = msg
            color = coler
            lifetime = time
            type = NotifyMessage.NotificationType.SUCCESS
        })
    }

    @JvmStatic
    fun warning(msg: String, time: Float, coler: String) {
        Server.sendToAll(new<NotifyMessage> {
            icon = 0x0021
            message = msg
            color = coler
            lifetime = time
            type = NotifyMessage.NotificationType.WARNING
        })
    }

    @JvmStatic
    fun error(msg: String, time: Float, coler: String) {
        Server.sendToAll(new<NotifyMessage> {
            icon = 0x0021
            message = msg
            color = coler
            lifetime = time
            type = NotifyMessage.NotificationType.ERROR
        })
    }



    override val name: String = "Notify"
}
