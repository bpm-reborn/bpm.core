package bpm.client.render.world

import bpm.mc.registries.ModComponents
import bpm.mc.registries.ModItems
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Vector3f
import kotlin.math.abs

class QuantumRenderer {
    companion object {
        private const val SWING_EFFECT_STRENGTH = 0.8f
        private const val WAVE_PROPAGATION_SPEED = 15f
    }

    private val stickStack = ItemStack(ModItems.ENDER_DEBUG_STICK)
    private var swingIntensity = 0f
    private var swingWaveOffset = 0f
    private var currentSwingOffset = Vector3f(0f, 0f, 0f)
    private var targetSwingOffset = Vector3f(0f, 0f, 0f)
    private var lastSwingProgress = 0f

    private fun updateSwingEffect(type: ItemDisplayContext) {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return

        val swingProgress = player.attackAnim
        val swingVelocity = (swingProgress - lastSwingProgress).coerceIn(-1f, 1f)
        lastSwingProgress = swingProgress

        // Update swing intensity with decay
        swingIntensity = (swingIntensity + abs(swingVelocity) * SWING_EFFECT_STRENGTH)
            .coerceIn(0f, 1f) * 0.95f

        // Update wave propagation
        swingWaveOffset += swingIntensity * WAVE_PROPAGATION_SPEED
    }

    fun render(
        stack: ItemStack,
        type: ItemDisplayContext,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        if (stack.has(ModComponents.CONTROLLER_UUID)) {
            poseStack.pushPose()
            setupTransforms(type, poseStack)
            SharedQuantumRenderer.renderQuantumEffect(poseStack, buffer)
            poseStack.popPose()
        }

        poseStack.pushPose()
        setStickTransforms(type, poseStack)
        renderBase(stickStack, type, poseStack, buffer, light, overlay)
        poseStack.popPose()
    }

    private fun setStickTransforms(type: ItemDisplayContext, poseStack: PoseStack) {
        when (type) {
            ItemDisplayContext.GUI -> {
                poseStack.translate(0.4, 0.4, 0.0)
                poseStack.scale(0.725f, 0.725f, 0.725f)
            }

            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.45, 0.6, 0.45)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-10f))
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-10f))
                poseStack.scale(0.6f, 0.6f, 0.6f)
            }

            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.45, 0.5, 0.45)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90f))
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(45f))
            }

            ItemDisplayContext.THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.65, 0.5, 0.4)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-45f))
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(15f))
                poseStack.scale(0.5f, 0.5f, 0.5f)
            }

            ItemDisplayContext.FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(0.55, 0.6, 0.45)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(10f))
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(10f))
                poseStack.scale(0.6f, 0.6f, 0.6f)
            }

            ItemDisplayContext.GROUND -> {
                poseStack.translate(0.5, 0.5, 0.5)
                poseStack.scale(0.5f, 0.5f, 0.5f)
            }

            ItemDisplayContext.FIXED -> {
                poseStack.translate(0.5, 0.5, 0.5)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(45f))
                poseStack.scale(0.6f, 0.6f, 0.6f)
            }

            else -> {
                poseStack.translate(0.5, 0.5, 0.5)
                poseStack.scale(0.5f, 0.5f, 0.5f)
            }
        }
    }

    private fun setupTransforms(type: ItemDisplayContext, poseStack: PoseStack) {
        when (type) {
            ItemDisplayContext.GUI -> {
                poseStack.translate(0.82f, 0.82f, 0.0f)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0f))
            }

            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.55, 0.9, 0.55)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90f))
                poseStack.scale(0.5f, 0.5f, 0.5f)
            }

            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.5, 0.9, 0.575)
                poseStack.scale(0.35f, 0.35f, 0.35f)
            }

            ItemDisplayContext.THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.55, 0.85, 0.55)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-45f))
                poseStack.scale(0.35f, 0.35f, 0.35f)
            }

            ItemDisplayContext.FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(0.48, 0.95, 0.6)
                poseStack.scale(0.4f, 0.4f, 0.4f)
            }

            ItemDisplayContext.GROUND -> {
                poseStack.translate(0.6, 0.7, 0.5)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(45f))
                poseStack.scale(0.3f, 0.3f, 0.3f)
            }

            ItemDisplayContext.FIXED -> {
                poseStack.translate(0.5, 0.5, 0.5)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(45f))
                poseStack.scale(0.4f, 0.4f, 0.4f)
            }

            else -> {
                poseStack.translate(0.5, 0.5, 0.5)
                poseStack.scale(0.4f, 0.4f, 0.4f)
            }
        }
    }

    private fun renderBase(
        stack: ItemStack,
        type: ItemDisplayContext,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        Minecraft.getInstance().itemRenderer.renderStatic(
            stack,
            type,
            light,
            overlay,
            poseStack,
            buffer,
            null,
            0
        )
    }
}