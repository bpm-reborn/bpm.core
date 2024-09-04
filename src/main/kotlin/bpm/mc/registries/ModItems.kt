package bpm.mc.registries

import bpm.Bpm
import bpm.booostrap.ModRegistry
import bpm.mc.item.EnderControllerItem
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

object ModItems : ModRegistry<Item> {

    override val registry = DeferredRegister.createItems(Bpm.ID)

    val ENDER_CONTROLLER by register {
        registry.registerItem("ender_pipe_controller") {
            EnderControllerItem()
        }
    }

    val ENDER_PROXY by register {
        registry.registerItem("ender_pipe_proxy") {
            BlockItem(ModBlocks.ENDER_PROXY, Item.Properties())
        }
    }

    val ENDER_PIPE by register {
        registry.registerItem("ender_pipe") {
            BlockItem(ModBlocks.ENDER_PIPE, Item.Properties())
        }
    }
}