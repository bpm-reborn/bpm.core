package bpm.mc.links

import bpm.common.bootstrap.BpmIO
import bpm.common.logging.KotlinLogging
import bpm.common.network.Listener
import bpm.common.network.Server
import bpm.common.packets.Packet
import bpm.mc.block.EnderControllerTileEntity
import bpm.network.level
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import java.util.UUID

object EnderNet : Listener {

    private val logger = KotlinLogging.logger {}
    private var state = EnderNetState()

    fun load(level: ServerLevel) {
        state = BpmIO.loadEnderNetState(level) ?: EnderNetState()
        logger.info { "Loaded EnderNet state with ${state.controllers.size} controllers" }
        Server.send(PacketEnderNetLoad(state))
    }

    fun save(level: ServerLevel) = BpmIO.saveEnderNetState(level, state)

    fun addController(controller: EnderControllerTileEntity) {
        val uuid = controller.getUUID()
        val worldPos = WorldPos(controller.level!!.dimension(), controller.blockPos)

        state.controllers[uuid] = EnderControllerState(uuid, worldPos)
        state.controllerLookups[worldPos] = uuid

        logger.info { "Added controller $uuid at $worldPos" }

        Server.send(PacketEnderNetControllerAdded(uuid, worldPos))
    }

    fun removeController(controller: EnderControllerTileEntity) {
        val uuid = controller.getUUID()
        val controllerState = state.controllers[uuid] ?: return

        state.controllers.remove(uuid)
        state.controllerLookups.remove(controllerState.worldPos)

        logger.info { "Removed controller $uuid" }

        Server.send(PacketEnderNetControllerRemoved(uuid))
    }


    fun getController(uuid: UUID): EnderControllerTileEntity? {
        val controllerState = state.controllers[uuid] ?: return null
        val serverLevel = controllerState.worldPos.level.level
        return serverLevel.getBlockEntity(controllerState.worldPos.pos) as? EnderControllerTileEntity
    }

    fun getControllerAt(level: Level, pos: BlockPos): EnderControllerTileEntity? {
        val worldPos = WorldPos(level.dimension(), pos)
        val uuid = state.controllerLookups[worldPos] ?: return null
        return getController(uuid)
    }

    fun getControllerUUID(level: Level, pos: BlockPos): UUID? {
        val worldPos = WorldPos(level.dimension(), pos)
        return state.controllerLookups[worldPos]
    }

    fun getState(): EnderNetState {
        return state
    }

    fun updateControllerPosition(uuid: UUID, newLevel: Level, newPos: BlockPos) {
        val controllerState = state.controllers[uuid] ?: return
        val oldWorldPos = controllerState.worldPos
        val newWorldPos = WorldPos(newLevel.dimension(), newPos)

        state.controllerLookups.remove(oldWorldPos)
        controllerState.worldPos = newWorldPos
        state.controllerLookups[newWorldPos] = uuid

        logger.info { "Updated controller $uuid position from $oldWorldPos to $newWorldPos" }
        Server.send(PacketEnderNetControllerMoved(uuid, newWorldPos))
    }


    fun addLink(controllerUUID: UUID, worldPos: WorldPos) {
        state.controllers[controllerUUID]?.links?.add(worldPos)
        logger.info { "Added link from $controllerUUID to $worldPos" }

        Server.send(PacketEnderNetLinkAdded(controllerUUID, worldPos))
    }

    fun removeLink(controllerUUID: UUID, worldPos: WorldPos) {
        state.controllers[controllerUUID]?.links?.remove(worldPos)
        logger.info { "Removed link from $controllerUUID to $worldPos" }
        Server.send(PacketEnderNetLinkRemoved(controllerUUID, worldPos))
    }


    fun getLinks(controllerUUID: UUID): Set<WorldPos> {
        return state.controllers[controllerUUID]?.links ?: emptySet()
    }


    fun clear() {
        state = EnderNetState()
        logger.info { "Cleared EnderNet state" }
    }

    override fun onPacket(packet: Packet, from: UUID) {
        when (packet) {
            is PacketEnderNetControllerAdded -> {
                state.controllers[packet.uuid] = EnderControllerState(packet.uuid, packet.worldPos)
                state.controllerLookups[packet.worldPos] = packet.uuid
                logger.info { "Received controller added packet $packet" }
            }

            is PacketEnderNetControllerRemoved -> {
                state.controllerLookups.remove(state.controllers[packet.uuid]?.worldPos)
                state.controllers.remove(packet.uuid)
                logger.info { "Received controller removed packet $packet" }
            }

            is PacketEnderNetControllerMoved -> {
                state.controllers[packet.uuid]?.worldPos = packet.worldPos
                logger.info { "Received controller moved packet $packet" }
            }

            is PacketEnderNetLinkAdded -> {
                state.controllers[packet.controllerUUID]?.links?.add(packet.linkPos)
                logger.info { "Received link added packet $packet" }
            }

            is PacketEnderNetLinkRemoved -> {
                state.controllers[packet.controllerUUID]?.links?.remove(packet.linkPos)
                logger.info { "Received link removed packet $packet" }
            }

            is PacketEnderNetLoad -> {
                state = packet.state
                logger.info { "Received load packet $packet" }
            }
        }
    }

}