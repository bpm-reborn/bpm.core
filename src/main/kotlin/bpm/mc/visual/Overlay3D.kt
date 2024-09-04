package bpm.mc.visual

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.culling.Frustum
import bpm.common.network.Listener
import bpm.common.packets.Packet
import org.joml.Matrix4f
import java.util.*

object Overlay3D : Listener {

    fun render(
        renderer: LevelRenderer,
        stack: PoseStack,
        model: Matrix4f,
        projection: Matrix4f,
        camera: Camera,
        frustum: Frustum
    ) {

    }

    override fun onPacket(packet: Packet, from: UUID) {

    }
}