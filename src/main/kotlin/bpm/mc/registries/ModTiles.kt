package bpm.mc.registries

import bpm.Bpm
import bpm.booostrap.ModRegistry
import bpm.mc.block.EnderControllerTileEntity
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister


object ModTiles : ModRegistry<BlockEntityType<*>> {

    override val registry: DeferredRegister<BlockEntityType<*>> = DeferredRegister.create(
        Registries.BLOCK_ENTITY_TYPE,
        Bpm.ID
    )

    val ENDER_CONTROLLER_TILE_ENTITY: BlockEntityType<EnderControllerTileEntity>
            by register {
                registry.register("ender_pipe_controller") { _ ->
                    BlockEntityType.Builder.of(::EnderControllerTileEntity, ModBlocks.ENDER_CONTROLLER).build(null)
                }
            }
}