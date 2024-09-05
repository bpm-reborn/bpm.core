package bpm.mc.registries

import bpm.Bpm
import bpm.booostrap.ModRegistry
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModCreativeTabs : ModRegistry<CreativeModeTab> {

    override val registry: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Bpm.ID)

    val MAIN by register {
        registry.register(
            "main",
            Supplier {
                CreativeModeTab.builder()
                    .displayItems { _, output ->
                        ModItems.registry.entries.forEach {
                            output.accept(it.get())
                        }
                    }
                    .icon { ItemStack(ModItems.ENDER_PIPE) }
                    .title(Component.translatable("tab.bpm.title"))
                    .build()
            }
        )
    }


}