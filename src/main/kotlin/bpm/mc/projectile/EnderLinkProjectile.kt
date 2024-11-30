package bpm.mc.projectile

import bpm.mc.block.EnderControllerTileEntity
import bpm.mc.links.EnderNet
import bpm.mc.links.WorldPos
import bpm.mc.registries.ModEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.util.UUID

class EnderLinkProjectile : Projectile {

    private var controllerPos: BlockPos? = null
    private var controllerUUID: UUID? = null
    private var hasLinked = false

    // Basic constructor required by Minecraft
    constructor(entityType: EntityType<out Projectile>, world: Level) : super(entityType, world)

    // Constructor for when spawning the projectile
    constructor(
        world: Level,
        player: Player,
        controllerPos: BlockPos,
        controllerUUID: UUID
    ) : this(ModEntities.ENDER_LINK_PROJECTILE, world) {
        setOwner(player)
        setPos(player.eyePosition)
        this.controllerPos = controllerPos
        this.controllerUUID = controllerUUID
    }

    override fun tick() {
        super.tick()

        // Check for hits while moving
        val hitResult = ProjectileUtil.getHitResultOnMoveVector(
            this,
            this::canHitEntity,
            ClipContext.Block.COLLIDER
        )

        if (hitResult.type != HitResult.Type.MISS) {
            hitTargetOrDeflectSelf(hitResult)
        }

        // Update position
        val motion = deltaMovement
        setPos(x + motion.x, y + motion.y, z + motion.z)


        //Drop trail of particles
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.ENCHANTED_HIT, x, y + 0.22, z, 0.0, -0.25, 0.0)
        } else {
            if (tickCount % 5 == 0) {
                //Play sounds that get higher pitched as it gets closer to the target, and does "notes" along the path
                level().playSound(null, x, y, z, SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.PLAYERS, 1.0f, 1.0f)
            } else {
                level().playSound(null, x, y, z, SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.PLAYERS, 1.0f, 1.0f)
            }
        }

        // Discard if it's been alive too long
        if (tickCount > 100) {
            discard()
        }
    }

    override fun onHitBlock(result: BlockHitResult) {
        if (!level().isClientSide && !hasLinked) {
            val hitPos = result.blockPos
            val hitDirection = result.direction
            //Spawn some particles
//            // Play sound
//            level().playSound(
//                null,
//                hitPos.x.toDouble(),
//                hitPos.y.toDouble() + 0.5,
//                hitPos.z.toDouble(),
//                SoundEvents.PORTAL_TRAVEL,
//                SoundSource.PLAYERS,
//                1.0f,
//                1.0f
//            )

            // Get the controller block entity
            controllerPos?.let { pos ->
                val controller = level().getBlockEntity(pos) as? EnderControllerTileEntity
                if (controller != null) {
                    val worldPos = WorldPos(level().dimension(), hitPos, hitDirection)
                    EnderNet.addLink(controllerUUID!!, worldPos)
                }
            }
        }
        discard()
    }

    // No synced data needed for this entity
    override fun defineSynchedData(builder: SynchedEntityData.Builder) = Unit

    override fun readAdditionalSaveData(compound: CompoundTag) {
        if (compound.contains("ControllerPos")) {
            controllerPos = BlockPos.of(compound.getLong("ControllerPos"))
        }
        if (compound.hasUUID("ControllerUUID")) {
            controllerUUID = compound.getUUID("ControllerUUID")
        }
        hasLinked = compound.getBoolean("HasLinked")
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        controllerPos?.let { pos ->
            compound.putLong("ControllerPos", pos.asLong())
        }
        controllerUUID?.let { uuid ->
            compound.putUUID("ControllerUUID", uuid)
        }
        compound.putBoolean("HasLinked", hasLinked)
    }
}