package bpm.client.render.markdown

import imgui.ImDrawList
import imgui.ImGui
import imgui.ImVec2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryUtil
import java.net.URL
import java.nio.ByteBuffer
import java.nio.IntBuffer

object MarkdownGif {

    val gifData: MutableMap<String, GifData> = mutableMapOf()

    data class GifData(
        val frames: List<GifFrame>,
        val width: Int,
        val height: Int,
        var currentFrame: Int = 0,
        var lastUpdateTime: Long = System.currentTimeMillis()
    )

    data class GifFrame(
        val textureId: Int,
        val duration: Int // in milliseconds
    )

    fun loadGif(url: String): GifData? {
        if (url in gifData) return gifData[url]

        try {
            val connection = URL(url).openConnection()
            connection.connect()
            val inputStream = connection.getInputStream()
            val bytes = inputStream.readBytes()
            val buffer = BufferUtils.createByteBuffer(bytes.size)
            buffer.put(bytes)
            buffer.flip()

            val width = BufferUtils.createIntBuffer(1)
            val height = BufferUtils.createIntBuffer(1)
            val frames = BufferUtils.createIntBuffer(1)
            val channels = BufferUtils.createIntBuffer(1)
            val delaysBuffer: PointerBuffer = MemoryUtil.memAllocPointer(1)

            val pixelData = STBImage.stbi_load_gif_from_memory(buffer, delaysBuffer, width, height, frames, channels, 4)
            if (pixelData != null) {
                val frameCount = frames.get(0)
                val gifFrames = mutableListOf<GifFrame>()
                val frameSize = width.get(0) * height.get(0) * 4

                val delaysPtr = delaysBuffer.get(0)
                val delays = IntArray(frameCount) { i ->
                    MemoryUtil.memGetInt(delaysPtr + i * 4)
                }

                for (i in 0 until frameCount) {
                    val frameBuffer = BufferUtils.createByteBuffer(frameSize)
                    pixelData.limit(frameSize * (i + 1))
                    pixelData.position(frameSize * i)
                    frameBuffer.put(pixelData)
                    frameBuffer.flip()

                    val textureId = createTexture(frameBuffer, width.get(0), height.get(0))
                    val duration = delays[i] // Use the actual delay for each frame
                    gifFrames.add(GifFrame(textureId, duration))
                }

                gifData[url] = GifData(gifFrames, width.get(0), height.get(0))
                STBImage.stbi_image_free(pixelData)
            }
        } catch (e: Exception) {
            println("Failed to load GIF: $url")
            return null
        }

        return gifData[url]
    }

    private fun createTexture(buffer: ByteBuffer, width: Int, height: Int): Int {
        val textureId = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA,
            width,
            height,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            buffer
        )
        return textureId
    }

    fun renderGif(url: String, drawList: ImDrawList, startX: Float, startY: Float, scale: Float) {
        val gif = gifData[url] ?: return

        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - gif.lastUpdateTime
        val currentFrame = gif.frames[gif.currentFrame]

        if (elapsedTime >= currentFrame.duration) {
            gif.currentFrame = (gif.currentFrame + 1) % gif.frames.size
            gif.lastUpdateTime = currentTime
        }

        val frame = gif.frames[gif.currentFrame]
        val imageSize = ImVec2(gif.width * scale, gif.height * scale)

        drawList.addImage(
            frame.textureId,
            startX,
            startY,
            startX + imageSize.x,
            startY + imageSize.y,
            0f,
            0f,
            1f,
            1f
        )
    }

    fun isGif(url: String): Boolean {
        return url.toLowerCase().endsWith(".gif")
    }

    fun cleanup() {
        gifData.values.forEach { gif ->
            gif.frames.forEach { frame ->
                GL11.glDeleteTextures(frame.textureId)
            }
        }
        gifData.clear()
    }
}