package bpm.client.render.world

import bpm.mc.projectile.EnderLinkProjectile
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth

class EnderLinkProjectileRenderer(
    context: EntityRendererProvider.Context
) : EntityRenderer<EnderLinkProjectile>(context) {

    private val ENDER_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/end_portal.png")

    override fun render(
        entity: EnderLinkProjectile,
        entityYaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        poseStack.pushPose()

        // Add floating effect
        val age = entity.tickCount + partialTick
        val floatOffset = Mth.sin(age * 0.1f) * 0.1f
        poseStack.translate(0.0, floatOffset + 0.15, 0.0)

        // Scale the projectile
        poseStack.scale(0.4f, 0.4f, 0.4f)

        // Add rotation to quantum effect
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(age * 2f))

        SharedQuantumRenderer.renderQuantumEffect(poseStack, buffer)

        poseStack.popPose()
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight)
    }

    override fun getTextureLocation(entity: EnderLinkProjectile): ResourceLocation = ENDER_TEXTURE
}