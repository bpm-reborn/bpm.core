package bpm.server.lua

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.neoforged.neoforge.server.ServerLifecycleHooks

object World : LuaBuiltin {

    private val server: MinecraftServer by lazy {
        ServerLifecycleHooks.getCurrentServer() ?: throw IllegalStateException("Server not available")
    }

    private val overworld by lazy {
        server.getLevel(Level.OVERWORLD) ?: throw IllegalStateException("Overworld not available")
    }

    @JvmStatic
    fun signalAt(x: Int, y: Int, z: Int): Int {
        val state = overworld.getBlockState(BlockPos(x, y, z))
        //If it's a jukebox, check if it's playing a record
        if (state.block == net.minecraft.world.level.block.Blocks.JUKEBOX) {
            return if (state.getValue(net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD)) 15 else 0
        }
        val signal = state.getSignal(overworld, BlockPos(x, y, z), Direction.DOWN)
        return signal
    }

    @JvmStatic
    fun spawnLightningAt(x: Float, y: Float, z: Float) {
        val lightningBolt = EntityType.LIGHTNING_BOLT.create(overworld)
        if (lightningBolt != null) {
            lightningBolt.moveTo(x.toDouble(), y.toDouble(), z.toDouble())
            overworld.addFreshEntity(lightningBolt)
        }
    }

    @JvmStatic
    fun playSoundAt(x: Float, y: Float, z: Float, soundName: String, volume: Float, pitch: Float) {
        val sound = getSoundByName(soundName)
        overworld.playSound(
            null, // No specific player, play for all
            x.toDouble(),
            y.toDouble(),
            z.toDouble(),
            sound,
            SoundSource.BLOCKS,
            volume,
            pitch
        )
    }


    @JvmStatic
    fun getPlayerInRadius(originX: Float, originY: Float, originZ: Float, radius: Float): List<String> {
        val entities = overworld.getEntities(
            null,
            AABB(
                originX.toDouble() - radius,
                originY.toDouble() - radius,
                originZ.toDouble() - radius,
                originX.toDouble() + radius,
                originY.toDouble() + radius,
                originZ.toDouble() + radius
            )
        )
        return entities
            .filterIsInstance<net.minecraft.world.entity.player.Player>()
            .map { it.stringUUID }
    }

    private fun getSoundByName(name: String): net.minecraft.sounds.SoundEvent {
        val registry = BuiltInRegistries.SOUND_EVENT
        val event = registry.get(net.minecraft.resources.ResourceLocation.tryParse(name))
        return event ?: net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.value()
    }


    @JvmStatic
    fun spawnParticle(
        x: Float,
        y: Float,
        z: Float,
        count: Int,
        velX: Float,
        velY: Float,
        velZ: Float,
        spread: Float,
        particleName: String
    ) {
        val particle = getParticleByName(particleName)
        //Position, count, velocity, spread
        overworld.sendParticles(
            particle,
            x.toDouble(),
            y.toDouble(),
            z.toDouble(),
            count,
            velX.toDouble(),
            velY.toDouble(),
            velZ.toDouble(),
            spread.toDouble()
        )
    }

    private fun getParticleByName(name: String): ParticleOptions {
        return when (name.lowercase()) {
            "explosion" -> ParticleTypes.EXPLOSION
            "smoke" -> ParticleTypes.SMOKE
            "flame" -> ParticleTypes.FLAME
            "heart" -> ParticleTypes.HEART
            "crit" -> ParticleTypes.CRIT
            "enchanted_hit" -> ParticleTypes.ENCHANTED_HIT
            "portal" -> ParticleTypes.PORTAL
            "water" -> ParticleTypes.FALLING_WATER
            "lava" -> ParticleTypes.LAVA
            "redstone" -> ParticleTypes.DUST_PLUME
            // Add more particle types as needed
            else -> ParticleTypes.SMOKE // Default particle if name not recognized
        }
    }

    override val name: String = "World"

}