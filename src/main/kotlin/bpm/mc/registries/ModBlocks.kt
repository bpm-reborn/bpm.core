package bpm.mc.registries

import bpm.Bpm
import bpm.booostrap.ModRegistry
import bpm.mc.block.EnderControllerBlock
import bpm.mc.block.EnderPipeBlock
import bpm.mc.block.EnderProxyBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

// THIS LINE IS REQUIRED FOR USING PROPERTY DELEGATES

object ModBlocks : ModRegistry<Block> {

    override val registry = DeferredRegister.createBlocks(Bpm.ID)

    val ENDER_CONTROLLER by register {
        registry.registerBlock(
            "ender_pipe_controller",
            {
                EnderControllerBlock(
                    BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.AMETHYST_CLUSTER)
                        .requiresCorrectToolForDrops()
                )
            },
            BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.AMETHYST_CLUSTER)
                .requiresCorrectToolForDrops()
        )
    }

    val ENDER_PROXY by register {
        registry.registerBlock(
            "ender_pipe_proxy",
            {
                EnderProxyBlock(BlockBehaviour.Properties.of())
            },
            BlockBehaviour.Properties.of().strength(3.0f).sound(SoundType.AMETHYST_CLUSTER)
                .requiresCorrectToolForDrops()
        )
    }

    val ENDER_PIPE by register {
        registry.registerBlock(
            "ender_pipe",
            {
                EnderPipeBlock(BlockBehaviour.Properties.of())
            },
            BlockBehaviour.Properties.of().strength(3.0f).sound(SoundType.AMETHYST_CLUSTER)
                .requiresCorrectToolForDrops()
        )
    }


}