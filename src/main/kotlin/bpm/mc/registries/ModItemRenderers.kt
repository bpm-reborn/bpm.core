package bpm.mc.registries

import bpm.client.render.world.QuantumRenderer
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions

object ModItemRenderers {

    val QuantumSphereRenderer = object : IClientItemExtensions {
        private val renderer = QuantumRenderer()

        override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
            return object : BlockEntityWithoutLevelRenderer(
                Minecraft.getInstance().blockEntityRenderDispatcher,
                Minecraft.getInstance().entityModels
            ) {
                override fun renderByItem(
                    stack: ItemStack,
                    type: ItemDisplayContext,
                    poseStack: PoseStack,
                    buffer: MultiBufferSource,
                    packedLight: Int,
                    packedOverlay: Int
                ) {
                    renderer.render(stack, type, poseStack, buffer, packedLight, packedOverlay)
                }
            }
        }
    }
}