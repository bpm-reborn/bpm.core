package bpm.mc.links

import bpm.common.memory.Buffer
import bpm.common.memory.readList
import bpm.common.memory.writeList
import bpm.common.serial.Serial
import bpm.common.serial.Serialize
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.dimension.DimensionType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class WorldPos(
    val level: ResourceKey<Level>,
    val pos: BlockPos,
    var direction: Direction? = null
)

object WorldPosSerializer : Serialize<WorldPos>(WorldPos::class) {

    override fun deserialize(buffer: Buffer): WorldPos {
        val level = buffer.readResourceKey<Level>()
        val pos = buffer.readBlockPos()
        if (!buffer.readBoolean()) return WorldPos(level, pos)
        return WorldPos(level, pos, buffer.readEnum(Direction::class.java))
    }

    override fun serialize(buffer: Buffer, value: WorldPos) {
        buffer.writeResourceKey(value.level)
        buffer.writeBlockPos(value.pos)
        buffer.writeBoolean(value.direction != null)
        value.direction?.let { buffer.writeEnum(it) }
    }
}

data class EnderControllerState(
    val uuid: UUID,
    var worldPos: WorldPos,
    val links: MutableSet<WorldPos> = ConcurrentHashMap.newKeySet()
)

object EnderControllerStateSerializer : Serialize<EnderControllerState>(EnderControllerState::class) {

    override fun deserialize(buffer: Buffer): EnderControllerState {
        val uuid = buffer.readUUID()
        val worldPos: WorldPos = Serial.read(buffer) ?: throw NullPointerException()
        val links = buffer.readList<WorldPos>()
        return EnderControllerState(uuid, worldPos).apply { this.links.addAll(links) }
    }

    override fun serialize(buffer: Buffer, value: EnderControllerState) {
        buffer.writeUUID(value.uuid)
        Serial.write(buffer, value.worldPos)
        buffer.writeList(value.links.toList())
    }
}

data class EnderNetState(
    val controllers: MutableMap<UUID, EnderControllerState> = ConcurrentHashMap(),
    val controllerLookups: MutableMap<WorldPos, UUID> = ConcurrentHashMap()
)

object EnderNetStateSerializer : Serialize<EnderNetState>(EnderNetState::class) {

    override fun deserialize(buffer: Buffer): EnderNetState {
        val controllers = buffer.readList<EnderControllerState>()
        val controllerLookups = controllers.associate { it.worldPos to it.uuid }
        return EnderNetState().apply {
            this.controllers.putAll(controllers.associateBy { it.uuid })
            this.controllerLookups.putAll(controllerLookups)
        }
    }

    override fun serialize(buffer: Buffer, value: EnderNetState) {
        buffer.writeList(value.controllers.values.toList())
    }
}