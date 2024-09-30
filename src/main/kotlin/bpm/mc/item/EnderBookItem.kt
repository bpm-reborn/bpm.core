package bpm.mc.item

import bpm.client.runtime.ClientRuntime
import bpm.mc.visual.DocsGuiScreen
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level

class EnderBookItem : Item(Properties().rarity(Rarity.EPIC)) {

    override fun appendHoverText(
        stack: ItemStack, context: TooltipContext, components: MutableList<Component>, flag: TooltipFlag
    ) {
        components.add(
            Component.literal("Ender Book").withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.GOLD)
        )
        components.add(
            Component.literal("Right click to open the documentation editor").withStyle(ChatFormatting.GRAY)
        )
    }

    override fun use(p_41432_: Level, p_41433_: Player, p_41434_: InteractionHand): InteractionResultHolder<ItemStack> {
        //If we are on the client side, open the docs editor
        if (p_41432_.isClientSide) {
            Minecraft.getInstance().setScreen(DocsGuiScreen())
        }
        return InteractionResultHolder.success(p_41433_.getItemInHand(p_41434_))
    }

    override fun isFoil(stack: ItemStack): Boolean {
        return true
    }

    override fun isEnchantable(stack: ItemStack): Boolean {
        return false
    }
}