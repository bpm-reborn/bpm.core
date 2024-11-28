package bpm.mc.item

import bpm.mc.registries.ModItemRenderers
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.item.ItemProperties
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.client.event.ModelEvent
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions
import net.neoforged.neoforge.client.model.BakedModelWrapper
import java.util.function.Consumer

class QuantumEntanglementItem : Item(Properties().stacksTo(1)) {

    //    override fun initializeClient(consumer: Consumer<IClientItemExtensions>) {
//        consumer.accept(ModItemRenderers.QuantumSphereRenderer)
//    }

}
