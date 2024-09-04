package bpm.client.runtime.windows

import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import imgui.ImVec2
import bpm.client.font.Fonts
import bpm.common.workspace.packets.NotifyMessage
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max

class NotificationManager {
    private val notificationQueue = ConcurrentLinkedQueue<NotifyMessage>()
    private var lastNotificationEndTime = 0f

    private val headerFont = Fonts.getFamily("Inter")["Bold"][16]
    private val bodyFont = Fonts.getFamily("Inter")["Regular"][14]
    private val iconFont = Fonts.getFamily("Fa")["Regular"][26]

    // Minimum size constants
    private val MIN_WIDTH = 300f
    private val MIN_HEIGHT = 100f

    fun addNotification(message: NotifyMessage) {
        val existingNotification = notificationQueue.find { it.type == message.type && it.message == message.message }
        if (existingNotification != null) {
            existingNotification.count++
            existingNotification.time = ImGui.getTime().toFloat() // Reset the time for the existing notification
        } else {
            message.count = 1
            message.time = ImGui.getTime().toFloat()
            notificationQueue.add(message)
        }
    }

    fun renderNotifications(drawList: ImDrawList, displaySize: ImVec2) {
        var yOffset = displaySize.y - 20f
        val currentTime = ImGui.getTime().toFloat()
        val visibleNotifications = mutableListOf<NotifyMessage>()

        val iterator = notificationQueue.iterator()
        while (iterator.hasNext() && visibleNotifications.size < 4) {
            val notification = iterator.next()
            val visibleTime = currentTime - max(notification.time, lastNotificationEndTime)
            val alpha = calculateAlpha(visibleTime, notification.lifetime)

            if (alpha <= 0) {
                if (visibleTime > notification.lifetime) {
                    iterator.remove()
                    lastNotificationEndTime = currentTime
                }
                continue
            }

            visibleNotifications.add(notification)
        }

        for (notification in visibleNotifications.asReversed()) {
            renderNotification(notification, drawList, displaySize, yOffset)
            yOffset -= MIN_HEIGHT + 10f  // Use minimum height for consistent spacing
        }
    }

    private fun renderNotification(notification: NotifyMessage, drawList: ImDrawList, displaySize: ImVec2, yOffset: Float) {
        val visibleTime = ImGui.getTime().toFloat() - max(notification.time, lastNotificationEndTime)
        val alpha = calculateAlpha(visibleTime, notification.lifetime)

        val padding = 16f
        val iconSize = 26f
        val headerHeight = 32f
        val margin = 15f

        val headerText = if (notification.count > 1) "${notification.header} (${notification.count})" else notification.header

        val headerSize = ImGui.calcTextSize(headerText)
        val messageSize = ImGui.calcTextSize(notification.message)

        val contentWidth = maxOf(headerSize.x, messageSize.x) + padding * 3 + iconSize
        val contentHeight = headerHeight + messageSize.y + padding * 3

        val totalWidth = max(contentWidth, MIN_WIDTH)
        val totalHeight = max(contentHeight, MIN_HEIGHT)

        val xPos = displaySize.x - totalWidth - margin
        val yPos = yOffset - totalHeight - margin

        // Background with gradient
        val bgColor = getNotificationColor(notification.type, 1f) // Full opacity for background
        drawList.addRectFilledMultiColor(
            xPos, yPos,
            xPos + totalWidth, yPos + totalHeight,
            blendColors(bgColor, ImColor.intToColor(0, 0, 0, 50), 0.7f).toLong(),
            blendColors(bgColor, ImColor.intToColor(0, 0, 0, 30), 0.7f).toLong(),
            blendColors(bgColor, ImColor.intToColor(0, 0, 0, 10), 0.7f).toLong(),
            bgColor.toLong()
        )

        // Header bar
        drawList.addRectFilled(
            xPos, yPos,
            xPos + totalWidth, yPos + headerHeight,
            getNotificationColor(notification.type, 1f), // Full opacity for header
            4f
        )

        // Icon
        drawList.addText(
            iconFont,
            iconSize,
            xPos + padding,
            yPos + (headerHeight - iconSize) / 2,
            ImColor.floatToColor(1f, 1f, 1f, 1f), // Full opacity for icon
            notification.icon.toChar().toString()
        )

        // Header text
        drawList.addText(
            headerFont,
            16f,
            xPos + padding * 2 + iconSize,
            yPos + (headerHeight - headerSize.y) / 2,
            ImColor.floatToColor(1f, 1f, 1f, 1f), // Full opacity for header text
            headerText
        )

        // Message text
        drawList.addText(
            bodyFont,
            14f,
            xPos + padding,
            yPos + headerHeight + padding,
            ImColor.floatToColor(0.9f, 0.9f, 0.9f, 1f), // Full opacity for message text
            notification.message
        )

        // Subtle border
        drawList.addRect(
            xPos, yPos,
            xPos + totalWidth, yPos + totalHeight,
            ImColor.floatToColor(1f, 1f, 1f, 0.3f),
            4f
        )
    }

    private fun calculateAlpha(visibleTime: Float, lifetime: Float): Float {
        val fadeInTime = 0.3f
        val fadeOutTime = 0.5f

        return when {
            visibleTime < 0f -> 0f
            visibleTime < fadeInTime -> visibleTime / fadeInTime
            visibleTime > lifetime - fadeOutTime -> 1f - (visibleTime - (lifetime - fadeOutTime)) / fadeOutTime
            visibleTime > lifetime -> 0f
            else -> 1f
        }.coerceIn(0f, 1f)
    }

    private fun getNotificationColor(type: NotifyMessage.NotificationType, alpha: Float): Int {
        return when (type) {
            NotifyMessage.NotificationType.INFO -> ImColor.floatToColor(0.2f, 0.6f, 0.9f, alpha)
            NotifyMessage.NotificationType.SUCCESS -> ImColor.floatToColor(0.2f, 0.8f, 0.2f, alpha)
            NotifyMessage.NotificationType.WARNING -> ImColor.floatToColor(1f, 0.7f, 0.2f, alpha)
            NotifyMessage.NotificationType.ERROR -> ImColor.floatToColor(0.9f, 0.2f, 0.2f, alpha)
        }
    }

    private fun blendColors(color1: Int, color2: Int, factor: Float): Int {
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF
        val a1 = (color1 shr 24) and 0xFF

        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF
        val a2 = (color2 shr 24) and 0xFF

        val r = (r1 * (1 - factor) + r2 * factor).toInt()
        val g = (g1 * (1 - factor) + g2 * factor).toInt()
        val b = (b1 * (1 - factor) + b2 * factor).toInt()
        val a = (a1 * (1 - factor) + a2 * factor).toInt()

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}