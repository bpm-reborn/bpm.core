package bpm.mc.block

import bpm.mc.registries.ModAttachments.UUID_ATTACHMENT
import bpm.mc.registries.ModTiles
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.HolderOwner
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.attachment.AttachmentHolder
import net.neoforged.neoforge.attachment.IAttachmentHolder
import net.neoforged.neoforge.attachment.IAttachmentSerializer
import bpm.client.runtime.ClientRuntime
import bpm.common.workspace.Workspace
import bpm.mc.registries.ModBlocks
import bpm.pipe.PipeNetworkManager
import bpm.server.ServerRuntime
import java.util.*

class EnderControllerTileEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModTiles.ENDER_CONTROLLER_TILE_ENTITY, pos, state), IAttachmentHolder,
    HolderOwner<EnderControllerTileEntity> {

    val attachmentHolder = AttachmentHolder.AsField(this)

    override fun onLoad() {
        super.onLoad()
        PipeNetworkManager.onPipeAdded(ModBlocks.ENDER_CONTROLLER, level!!, worldPosition)
    }

    override fun saveAdditional(tag: CompoundTag, provider: HolderLookup.Provider) {
        super.saveAdditional(tag, provider)
        // Save the UUID directly to the main tag
        // Save other attachments
        tag.putUUID("_Uid", getUUID())
        attachmentHolder.serializeAttachments(provider)?.let {
            tag.put(AttachmentHolder.ATTACHMENTS_NBT_KEY, it)
        }
    }


    override fun loadAdditional(tag: CompoundTag, provider: HolderLookup.Provider) {
        super.loadAdditional(tag, provider)
        // Load the UUID directly from the main tag
        if (tag.contains("_Uid")) {
            setUUID(tag.getUUID("_Uid"))
        }
        if (tag.contains(AttachmentHolder.ATTACHMENTS_NBT_KEY)) {
            attachmentHolder.deserializeInternal(provider, tag.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY))
        }
    }

    override fun getUpdateTag(provider: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(provider)
        // Include UUID in the update tag
        attachmentHolder.serializeAttachments(provider)?.let {
            tag.put(AttachmentHolder.ATTACHMENTS_NBT_KEY, it)
        }
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket? {
        return ClientboundBlockEntityDataPacket.create(this)
    }


    fun getUUID(): UUID = getData(UUID_ATTACHMENT)
    //The variable to store the workspace
    private var cachedWorkspace: Workspace? = null
    //Gets the workspace from the runtime
    val workspace: Workspace?
        get() {
            if (cachedWorkspace != null) return cachedWorkspace
            val uuid = getUUID()
            cachedWorkspace = if (level?.isClientSide == true) ClientRuntime[uuid]
            else ServerRuntime[uuid]
            return cachedWorkspace
        }


    fun setUUID(newUUID: UUID) {
        setData(UUID_ATTACHMENT, newUUID)
        setChanged()
    }
}

object UUIDSerializer : IAttachmentSerializer<CompoundTag, UUID> {

    override fun write(data: UUID, provider: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        tag.putUUID("value", data)
        return tag
    }

    override fun read(holder: IAttachmentHolder, nbt: CompoundTag, provider: HolderLookup.Provider): UUID {
        return nbt.getUUID("value")
    }
}