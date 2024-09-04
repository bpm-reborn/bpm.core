package bpm.mc.item

import bpm.mc.registries.ModBlocks
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.*
import bpm.client.runtime.ClientRuntime

class EnderControllerItem : BlockItem(ModBlocks.ENDER_CONTROLLER, Item.Properties().rarity(Rarity.EPIC)) {

    override fun appendHoverText(
        stack: ItemStack, context: TooltipContext, components: MutableList<Component>, flag: TooltipFlag
    ) {
//        components.add(
//            Component.literal("Ender Controller").withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.GOLD)
//        )
        if (Screen.hasShiftDown()) {
            val data = stack.get(DataComponents.BLOCK_ENTITY_DATA) ?: return
            val tag = data.copyTag()
            val attachments = tag.getCompound("neoforge:attachments")
            val uuid = attachments.getCompound("bpm:uuid").getUUID("value")
            //This has the side effect of initializing the workspace if it doesn't exist client side by requesting it from the server

            val workspace = ClientRuntime[uuid]
            //New line
            components.add(
                Component.literal("========================================")
                    .withStyle(ChatFormatting.STRIKETHROUGH).withStyle(ChatFormatting.GRAY)
            )
            //New line
            components.add(
                Component.literal("")
            )
            val nodeCount = Component.literal(workspace?.graph?.nodes?.size?.toString() ?: "none!")
                .withStyle(ChatFormatting.DARK_GRAY)
                .withStyle(ChatFormatting.ITALIC)
            val connectionCount = Component.literal(workspace?.graph?.getLinks()?.size?.toString() ?: "none!")
                .withStyle(ChatFormatting.DARK_GRAY)
                .withStyle(ChatFormatting.ITALIC)
            components.add(
                Component.literal("Nodes").withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.ITALIC)
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(
                        nodeCount
                    )
            )
            //New line
            components.add(
                Component.literal("")
            )
            components.add(
                Component.literal("Connections").withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.ITALIC)
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(
                        connectionCount
                    )
            )

            //New line
            components.add(
                Component.literal("")
            )
            components.add(
                Component.literal("Uuid").withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.ITALIC)
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC))
                    .append(Component.literal(uuid.toString()).withStyle(ChatFormatting.DARK_GRAY))
            )

            //New line
            components.add(
                Component.literal("")
            )

            components.add(
                Component.literal("========================================")
                    .withStyle(ChatFormatting.STRIKETHROUGH).withStyle(ChatFormatting.GRAY)
            )

        } else {
            components.add(
                Component.literal("Hold ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("Shift").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" for more info").withStyle(ChatFormatting.GRAY))
            )
        }

//        components.
        super.appendHoverText(stack, context, components, flag)
    }

    override fun isFoil(stack: ItemStack): Boolean {
        return true
    }

    override fun isEnchantable(stack: ItemStack): Boolean {
        return false
    }
}