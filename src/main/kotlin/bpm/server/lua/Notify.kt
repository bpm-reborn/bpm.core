package bpm.server.lua

import bpm.client.render.panel.ConsolePanel
import bpm.common.network.Network.new
import bpm.common.network.Server
import bpm.common.workspace.packets.NotifyMessage
import org.joml.Vector4f

object Notify : LuaBuiltin {

    @JvmStatic
    fun message(msg: String, time: Float, coler: String, headerIn: String, typeIn: String) {
        //If the color is 7 characters, append FF to the end
//        Server.sendToAll(new<NotifyMessage> {
//            icon = 0x0021
//            message = msg
//            color = if (coler.length == 7) "${coler}FF" else coler
//            lifetime = time
//            header = headerIn
//            type = NotifyMessage.NotificationType.valueOf(typeIn)
//        })
        val logLevel = when (typeIn) {
            "INFO" -> ConsolePanel.LogLevel.INFO
            "SUCCESS" -> ConsolePanel.LogLevel.DEBUG
            "WARNING" -> ConsolePanel.LogLevel.WARNING
            "ERROR" -> ConsolePanel.LogLevel.ERROR
            else -> ConsolePanel.LogLevel.INFO
        }
        ConsolePanel.log(msg, logLevel)
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
        ConsolePanel.log(msg, ConsolePanel.LogLevel.INFO)
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
        ConsolePanel.log(msg, ConsolePanel.LogLevel.WARNING)
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
        ConsolePanel.log(msg, ConsolePanel.LogLevel.ERROR)
    }


    override val name: String = "Notify"
}
