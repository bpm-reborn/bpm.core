package bpm.client.render.markdown

import bpm.mc.visual.ProxyScreen.width
import com.mojang.blaze3d.platform.GlConst.GL_CLAMP_TO_EDGE
import io.netty.buffer.ByteBuf
import kotlinx.coroutines.*
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL21
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
            val textureId = loadOpenGLTexture(imgData)
            if (textureId == -1) {
                imageData.remove(url) // Remove failed texture
                return null
            }
            imgData.textureId = textureId
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
        try {
            // Validate input data
            if (!validateImageData(imageData)) {
                return -1
            }

            // Generate texture ID
            val texture = GL11.glGenTextures()
            if (texture == 0) {
                println("Failed to generate texture")
                return -1
            }

            try {
                // Create a PBO for texture upload
                val pbo = GL15.glGenBuffers()
                GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, pbo)

                // Calculate buffer size and alignment
                val alignedWidth = nextPowerOfTwo(imageData.width)
                val alignedHeight = nextPowerOfTwo(imageData.height)
                val bufferSize = alignedWidth * alignedHeight * 4 // 4 bytes per pixel for RGBA

                // Allocate buffer and copy data
                GL15.glBufferData(GL21.GL_PIXEL_UNPACK_BUFFER, bufferSize.toLong(), GL15.GL_STREAM_DRAW)
                val mappedBuffer = GL15.glMapBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, GL15.GL_WRITE_ONLY)
                if (mappedBuffer != null) {
                    // Copy and pad the image data to power-of-two dimensions
                    copyAndPadImageData(imageData, mappedBuffer, alignedWidth, alignedHeight)
                    GL15.glUnmapBuffer(GL21.GL_PIXEL_UNPACK_BUFFER)
                }

                // Bind texture and set parameters
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture)

                // Set texture parameters
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL15.GL_CLAMP_TO_EDGE)
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL15.GL_CLAMP_TO_EDGE)

                // Set proper alignment
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4)
                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0)
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0)
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0)

                // Upload texture data from PBO
                GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    alignedWidth,
                    alignedHeight,
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    0L // Offset in PBO
                )

                // Cleanup
                GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0)
                GL15.glDeleteBuffers(pbo)
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)

                return if (checkGLError("Texture upload") == GL11.GL_NO_ERROR) texture else {
                    GL11.glDeleteTextures(texture)
                    -1
                }
            } catch (e: Exception) {
                GL11.glDeleteTextures(texture)
                throw e
            }
        } catch (e: Exception) {
            println("Exception while loading texture: ${e.message}")
            e.printStackTrace()
            return -1
        }
    }

    private fun validateImageData(imageData: ImageData): Boolean {
        if (imageData.width <= 0 || imageData.height <= 0) {
            println("Invalid image dimensions: ${imageData.width}x${imageData.height}")
            return false
        }
        if (imageData.buffer == null || !imageData.buffer.hasRemaining()) {
            println("Invalid buffer state")
            return false
        }
        if (imageData.buffer.remaining() < imageData.width * imageData.height * 4) {
            println("Buffer too small for image dimensions")
            return false
        }
        return true
    }

    private fun nextPowerOfTwo(n: Int): Int {
        var value = n
        value--
        value = value or (value shr 1)
        value = value or (value shr 2)
        value = value or (value shr 4)
        value = value or (value shr 8)
        value = value or (value shr 16)
        value++
        return value
    }

    private fun copyAndPadImageData(imageData: ImageData, mappedBuffer: ByteBuffer, alignedWidth: Int, alignedHeight: Int) {
        val srcBuffer = imageData.buffer
        srcBuffer.rewind()

        // Copy row by row, padding as needed
        for (y in 0 until imageData.height) {
            // Copy one row
            for (x in 0 until imageData.width) {
                val srcPos = (y * imageData.width + x) * 4
                val dstPos = (y * alignedWidth + x) * 4
                for (i in 0..3) {
                    mappedBuffer.put(dstPos + i, srcBuffer.get(srcPos + i))
                }
            }
            // Pad remaining pixels in row
            for (x in imageData.width until alignedWidth) {
                val dstPos = (y * alignedWidth + x) * 4
                mappedBuffer.put(dstPos, 0)     // R
                mappedBuffer.put(dstPos + 1, 0) // G
                mappedBuffer.put(dstPos + 2, 0) // B
                mappedBuffer.put(dstPos + 3, 0) // A
            }
        }
        // Pad remaining rows
        for (y in imageData.height until alignedHeight) {
            for (x in 0 until alignedWidth) {
                val dstPos = (y * alignedWidth + x) * 4
                mappedBuffer.put(dstPos, 0)     // R
                mappedBuffer.put(dstPos + 1, 0) // G
                mappedBuffer.put(dstPos + 2, 0) // B
                mappedBuffer.put(dstPos + 3, 0) // A
            }
        }
    }

    private fun checkGLError(operation: String): Int {
        val error = GL11.glGetError()
        if (error != GL11.GL_NO_ERROR) {
            println("OpenGL error after $operation: $error")
        }
        return error
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