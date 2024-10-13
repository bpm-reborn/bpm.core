package bpm.client.render.markdown

import bpm.mc.visual.ProxyScreen.width
import com.mojang.blaze3d.platform.GlConst.GL_CLAMP_TO_EDGE
import io.netty.buffer.ByteBuf
import kotlinx.coroutines.*
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import java.net.URL
import java.nio.ByteBuffer
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack

object MarkdownImages {

    private val imageData: MutableMap<String, ImageData> = mutableMapOf()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    data class ImageData(
        val width: Int,
        val height: Int,
        val channels: Int,
        val buffer: ByteBuffer,
        var textureId: Int = -1
    )

    fun getImage(url: String): Any? {
        if (MarkdownGif.isGif(url)) {
            return MarkdownGif.loadGif(url)
        }

        val imgData = imageData[url]
        if (imgData == null) {
            loadImage(url)
        }
        if (imgData != null && imgData.textureId == -1) {
            imgData.textureId = loadOpenGLTexture(imgData)
        }
        return imgData
    }

    fun loadImage(url: String) {
        if (url in imageData) return

        try {
            val imageBytes = downloadImage(url)
            val imgData = prepareForImGui(imageBytes)
            imageData[url] = imgData
        } catch (e: Exception) {
            println("Failed to load image: $url")
            e.printStackTrace()
        }
    }

     fun loadOpenGLTexture(imageData: ImageData): Int {
        val texture = GL11.glGenTextures()
        glBindTexture(GL_TEXTURE_2D, texture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)


        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            imageData.width,
            imageData.height,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            imageData.buffer
        )
        glBindTexture(GL_TEXTURE_2D, 0)
        return texture
    }

    private fun downloadImage(url: String): ByteArray {
        val connection = URL(url).openConnection()
        connection.connect()
        val inputStream = connection.getInputStream()
        return inputStream.readBytes()
    }

    private fun prepareForImGui(imageBytes: ByteArray): ImageData {
        val buffer = BufferUtils.createByteBuffer(imageBytes.size)
        buffer.put(imageBytes)
        buffer.flip()

        MemoryStack.stackPush().use { stack ->
            val width = stack.mallocInt(1)
            val height = stack.mallocInt(1)
            val channels = stack.mallocInt(1)
            val pixelData = STBImage.stbi_load_from_memory(buffer, width, height, channels, 4)
            if (pixelData == null) {
                throw Exception("Failed to load image")
            }
            return ImageData(width.get(0), height.get(0), channels.get(0), pixelData)
        }
    }

    fun cleanup() {
        coroutineScope.cancel()
        imageData.values.forEach { STBImage.stbi_image_free(it.buffer) }
        imageData.clear()
        MarkdownGif.cleanup()
    }
}