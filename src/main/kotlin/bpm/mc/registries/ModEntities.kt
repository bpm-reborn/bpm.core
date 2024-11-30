package bpm.mc.registries

import bpm.Bpm
import bpm.common.bootstrap.ModRegistry
import bpm.mc.projectile.EnderLinkProjectile
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.neoforged.neoforge.registries.DeferredRegister

object ModEntities : ModRegistry<EntityType<*>> {

    override val registry: DeferredRegister<EntityType<*>> = DeferredRegister.create(Registries.ENTITY_TYPE, Bpm.ID)

    val ENDER_LINK_PROJECTILE by register {
        registry.register("ender_link_projectile") { _ ->
            EntityType.Builder.of(::EnderLinkProjectile, MobCategory.MISC)
                .sized(0.25f, 0.25f)
                .fireImmune()
                .clientTrackingRange(4)
                .updateInterval(10)
                .build("ender_link_projectile")
        }
    }
}