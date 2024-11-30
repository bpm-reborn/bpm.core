package bpm.mc.registries

import bpm.Bpm
import bpm.common.bootstrap.ModRegistry
import net.minecraft.core.UUIDUtil
import net.minecraft.core.component.DataComponentType
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.*

object ModComponents : ModRegistry<DataComponentType<*>> {

    override val registry: DeferredRegister<DataComponentType<*>> = DeferredRegister.createDataComponents(Bpm.ID)

    val CONTROLLER_UUID by register {
        registry.register("ender_controller_uuid") { _ ->
            DataComponentType.builder<UUID>().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC)
                .build()
        }
    }

}