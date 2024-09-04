package bpm.mc.registries

import bpm.Bpm
import bpm.booostrap.ModRegistry
import bpm.mc.block.UUIDSerializer
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.*
import java.util.function.Supplier

object ModAttachments : ModRegistry<AttachmentType<*>> {

    override val registry: DeferredRegister<AttachmentType<*>> = DeferredRegister.create(
        NeoForgeRegistries.ATTACHMENT_TYPES,
        Bpm.ID
    )

    val UUID_ATTACHMENT: AttachmentType<UUID> by register {
        registry.register("uuid",
            Supplier {
                AttachmentType.builder(Supplier { UUID.randomUUID() })
                    .serialize(UUIDSerializer)
                    .build()
            })
    }

}