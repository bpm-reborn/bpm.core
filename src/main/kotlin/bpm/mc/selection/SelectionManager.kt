package bpm.mc.selection

import bpm.common.bootstrap.BpmIO
import bpm.common.logging.KotlinLogging
import bpm.common.memory.Buffer
import bpm.common.network.Client
import bpm.common.network.Endpoint
import bpm.common.network.Server
import bpm.common.network.SidedListener
import bpm.common.packets.Packet
import bpm.common.serial.Serial
import bpm.common.serial.Serialize
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import java.util.*
import kotlin.collections.HashMap

class SelectionManager(side: Endpoint.Side) : SidedListener(side) {
    private var activeSelections: SelectionState? = null
    private val logger = KotlinLogging.logger { }

    fun load(level: ServerLevel) {
        activeSelections = Serial.read(SelectionState::class, BpmIO.worldDataPathFor(level, "selections"))
        if (activeSelections == null) {
            activeSelections = SelectionState()
            activeSelections!![UUID.randomUUID()] = SelectionData(
                uuid = UUID.randomUUID(),
                selectionType = SelectionType.BLOCK,
                blockPos = BlockPos.ZERO,
                entityId = null,
                start = null,
                end = null
            )
            save(level)
        }
        Server.send(PacketSelectionSync(activeSelections!!))
        logger.debug("Loaded ${getAllSelections().size} selections")
    }

    fun save(level: ServerLevel) {
        Serial.write(BpmIO.worldDataPathFor(level, "selections"), activeSelections!!)
        logger.debug("Saved ${getAllSelections().size} selections")
        Server.send(PacketSelectionSync(activeSelections!!))
    }



    override fun onClientPacket(packet: Packet, from: UUID) {
        when (packet) {
            is PacketSelectionSync -> {
                // Sync the state
                activeSelections = packet.state
                logger.debug("Sent ${getAllSelections().size} selections to $from")
            }

            is PacketSelectionCreate -> {
                // Create a new selection and add it to active selections
                activeSelections!![packet.data.uuid] = packet.data

                // If this is server-side, broadcast to all clients
                if (side == Endpoint.Side.SERVER) {
                    Server.send(packet)
                }
            }


            is PacketSelectionUpdate -> {
                // Update an existing selection
                activeSelections!![packet.uuid]?.let { selection ->
                    when (selection.selectionType) {
                        SelectionType.BLOCK -> {
                            if (packet.blockPos != null) {
                                val updated = selection.copy(blockPos = packet.blockPos)
                                activeSelections!![packet.uuid] = updated
                            }
                        }

                        SelectionType.ENTITY -> {
                            if (packet.entityId != null) {
                                val updated = selection.copy(entityId = packet.entityId)
                                activeSelections!![packet.uuid] = updated
                            }
                        }

                        SelectionType.AREA -> {
                            if (packet.start != null && packet.end != null) {
                                val updated = selection.copy(start = packet.start, end = packet.end)
                                activeSelections!![packet.uuid] = updated
                            }
                        }
                    }

                    // If this is server-side, broadcast to all clients
                    if (side == Endpoint.Side.SERVER) {
                        Server.send(packet)
                    }
                }
            }

            is PacketSelectionDelete -> {
                // Remove a selection
                activeSelections!!.remove(packet.uuid)

                // If this is server-side, broadcast to all clients
                if (side == Endpoint.Side.SERVER) {
                    Server.send(packet)
                }
            }

            else -> Unit
        }
    }


    override fun onServerPacket(packet: Packet, from: UUID) {
        // Handle server-side packets similar to client-side
        when (packet) {
            is PacketSelectionSyncRequest -> {
                // Send the current state to the client
                activeSelections?.let { Server.send(PacketSelectionSync(it), from) }
            }

            is PacketSelectionCreate -> {
                activeSelections!![packet.data.uuid] = packet.data
            }

            is PacketSelectionUpdate -> {
                activeSelections!![packet.uuid]?.let { selection ->
                    when (selection.selectionType) {
                        SelectionType.BLOCK -> {
                            if (packet.blockPos != null) {
                                val updated = selection.copy(blockPos = packet.blockPos)
                                activeSelections!![packet.uuid] = updated
                            }
                        }

                        SelectionType.ENTITY -> {
                            if (packet.entityId != null) {
                                val updated = selection.copy(entityId = packet.entityId)
                                activeSelections!![packet.uuid] = updated
                            }
                        }

                        SelectionType.AREA -> {
                            if (packet.start != null && packet.end != null) {
                                val updated = selection.copy(start = packet.start, end = packet.end)
                                activeSelections!![packet.uuid] = updated
                            }
                        }
                    }
                }
            }

            is PacketSelectionDelete -> {
                activeSelections!!.remove(packet.uuid)
            }

            else -> Unit
        }
    }

    fun getSelection(uuid: UUID): SelectionData? {
        return activeSelections!![uuid]
    }

    fun getAllSelections(): Collection<SelectionData> {
        return activeSelections?.selections?.values ?: emptyList()
    }

    // Helper methods for creating selections
    fun createBlockSelection(blockPos: BlockPos): UUID {
        val uuid = UUID.randomUUID()
        val selection = SelectionData(
            uuid = uuid,
            selectionType = SelectionType.BLOCK,
            blockPos = blockPos,
            entityId = null,
            start = null,
            end = null
        )

        // Add to local selections
        activeSelections!![uuid] = selection

        // Send packet to sync
        val packet = PacketSelectionCreate(selection)
        if (side == Endpoint.Side.CLIENT) {
            Client.send(packet)
        } else {
            Server.send(packet)
        }

        return uuid
    }

    fun createEntitySelection(entityId: Int): UUID {
        val uuid = UUID.randomUUID()
        val selection = SelectionData(
            uuid = uuid,
            selectionType = SelectionType.ENTITY,
            blockPos = null,
            entityId = entityId,
            start = null,
            end = null
        )

        // Add to local selections
        activeSelections!![uuid] = selection

        // Send packet to sync
        val packet = PacketSelectionCreate(selection)
        if (side == Endpoint.Side.CLIENT) {
            Client.send(packet)
        } else {
            Server.send(packet)
        }

        return uuid
    }

    fun createAreaSelection(start: BlockPos, end: BlockPos): UUID {
        val uuid = UUID.randomUUID()
        val selection = SelectionData(
            uuid = uuid, selectionType = SelectionType.AREA, blockPos = null, entityId = null, start = start, end = end
        )

        // Add to local selections
        activeSelections!![uuid] = selection

        // Send packet to sync
        val packet = PacketSelectionCreate(selection)
        if (side == Endpoint.Side.CLIENT) {
            Client.send(packet)
        } else {
            Server.send(packet)
        }

        return uuid
    }

    fun updateBlockSelection(uuid: UUID, blockPos: BlockPos) {
        val selection = activeSelections!![uuid]
        if (selection?.selectionType == SelectionType.BLOCK) {
            // Update local selection
            activeSelections!![uuid] = selection.copy(blockPos = blockPos)

            // Send packet to sync
            val packet = PacketSelectionUpdate(uuid, blockPos = blockPos)
            if (side == Endpoint.Side.CLIENT) {
                Client.send(packet)
            } else {
                Server.send(packet)
            }
        }
    }

    fun updateEntitySelection(uuid: UUID, entityId: Int) {
        val selection = activeSelections!![uuid]
        if (selection?.selectionType == SelectionType.ENTITY) {
            // Update local selection
            activeSelections!![uuid] = selection.copy(entityId = entityId)

            // Send packet to sync
            val packet = PacketSelectionUpdate(uuid, entityId = entityId)
            if (side == Endpoint.Side.CLIENT) {
                Client.send(packet)
            } else {
                Server.send(packet)
            }
        }
    }

    fun updateAreaSelection(uuid: UUID, start: BlockPos, end: BlockPos) {
        val selection = activeSelections!![uuid]
        if (selection?.selectionType == SelectionType.AREA) {
            // Update local selection
            activeSelections!![uuid] = selection.copy(start = start, end = end)

            // Send packet to sync
            val packet = PacketSelectionUpdate(uuid, start = start, end = end)
            if (side == Endpoint.Side.CLIENT) {
                Client.send(packet)
            } else {
                Server.send(packet)
            }
        }
    }

    fun deleteSelection(uuid: UUID) {
        // Remove from local selections
        activeSelections?.remove(uuid)

        // Send packet to sync
        val packet = PacketSelectionDelete(uuid)
        if (side == Endpoint.Side.CLIENT) {
            Client.send(packet)
        } else {
            Server.send(packet)
        }
    }

    override fun onConnect(uuid: UUID) {
        isClient {
            Client.send(PacketSelectionSyncRequest())
        }
    }

    // Updated SelectionData to handle all types
    data class SelectionData(
        val uuid: UUID, val selectionType: SelectionType, val blockPos: BlockPos? = null,     // For BLOCK type
        val entityId: Int? = null,         // For ENTITY type
        val start: BlockPos? = null,        // For AREA type
        val end: BlockPos? = null           // For AREA type
    ) {
        object SelectionDataSerializer : Serialize<SelectionData>(SelectionData::class) {
            override fun deserialize(buffer: Buffer): SelectionData {
                val uuid = buffer.readUUID()
                val selectionType = SelectionType.valueOf(buffer.readString())

                // Deserialize based on type
                return when (selectionType) {
                    SelectionType.BLOCK -> {
                        val blockPos = buffer.readBlockPos()
                        SelectionData(uuid, selectionType, blockPos = blockPos)
                    }

                    SelectionType.ENTITY -> {
                        val entityId = buffer.readInt()
                        SelectionData(uuid, selectionType, entityId = entityId)
                    }

                    SelectionType.AREA -> {
                        val start = buffer.readBlockPos()
                        val end = buffer.readBlockPos()
                        SelectionData(uuid, selectionType, start = start, end = end)
                    }
                }
            }

            override fun serialize(buffer: Buffer, value: SelectionData) {
                buffer.writeUUID(value.uuid)
                buffer.writeString(value.selectionType.name)

                // Serialize based on type
                when (value.selectionType) {
                    SelectionType.BLOCK -> {
                        buffer.writeBlockPos(value.blockPos ?: BlockPos.ZERO)
                    }

                    SelectionType.ENTITY -> {
                        buffer.writeInt(value.entityId ?: -1)
                    }

                    SelectionType.AREA -> {
                        buffer.writeBlockPos(value.start ?: BlockPos.ZERO)
                        buffer.writeBlockPos(value.end ?: BlockPos.ZERO)
                    }
                }
            }
        }
    }

    data class SelectionState(
        val selections: MutableMap<UUID, SelectionData>
    ) {

        constructor() : this(mutableMapOf())

        operator fun set(uuid: UUID, value: SelectionData) {
            selections[uuid] = value
        }

        operator fun get(uuid: UUID): SelectionData? {
            return selections[uuid]
        }

        fun remove(uuid: UUID) {
            selections.remove(uuid)
        }

        object SelectionStateSerializer : Serialize<SelectionState>(SelectionState::class) {
            override fun deserialize(buffer: Buffer): SelectionState {
                val selections = mutableMapOf<UUID, SelectionData>()
                try {
                    val size = buffer.readInt()
                    for (i in 0 until size) {
                        val selection =
                            Serial.read<SelectionData>(buffer) ?: error("Failed to deserialize SelectionData")
                        selections[selection.uuid] = selection
                    }
                } catch (_: Exception) { }

                return SelectionState(selections)
            }

            override fun serialize(buffer: Buffer, value: SelectionState) {
                buffer.writeInt(value.selections.size)
                for (selection in value.selections.values) {
                    Serial.write(buffer, selection)
                }
            }
        }
    }

    enum class SelectionType {
        BLOCK, ENTITY, AREA
    }

    companion object {
        val client: SelectionManager by lazy { Client.installed() }
        val server: SelectionManager by lazy { Server.installed() }
    }
}