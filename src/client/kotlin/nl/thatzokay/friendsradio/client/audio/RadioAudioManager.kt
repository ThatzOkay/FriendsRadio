package nl.thatzokay.friendsradio.client.audio

import net.minecraft.client.MinecraftClient
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.particle.ParticleTypes
import net.minecraft.sound.SoundCategory
import net.minecraft.util.Arm
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import nl.thatzokay.friendsradio.ModBlocks
import nl.thatzokay.friendsradio.block.RadioBlock
import nl.thatzokay.friendsradio.block.RadioBlockEntity
import nl.thatzokay.friendsradio.client.player.RadioPlayer
import nl.thatzokay.friendsradio.client.utils.findPlayingRadioStack

object RadioAudioManager {

    private data class ActiveStream(
        val url: String,
        val player: RadioPlayer,
        val thread: Thread
    )

    private val activeStreams = mutableMapOf<BlockPos, ActiveStream>()
    private var activeStreamPlayer: ActiveStream? = null

    val knownRadios = mutableSetOf<BlockPos>()

    fun play(pos: BlockPos, url: String, volume: Float) {
        val existing = activeStreams[pos]

        // Already playing the same URL — just update volume
        if (existing != null && existing.url == url) {
            existing.player.setVolume(volume)
            return
        }

        // Stop whatever was playing here before
        stop(pos)

        val player = RadioPlayer(url)
        player.setVolume(volume)  // set before starting so first audio is correct volume
        val thread = Thread(player, "radio-stream-$pos").also {
            it.isDaemon = true  // don't block JVM shutdown
            it.start()
        }
        activeStreams[pos] = ActiveStream(url, player, thread)
    }

    fun stop(pos: BlockPos) {
        activeStreams.remove(pos)?.player?.stop()
    }

    fun playPlayer(url: String, volume: Float) {
        if (activeStreamPlayer != null && activeStreamPlayer!!.url == url) {
            activeStreamPlayer!!.player.setVolume(volume)
            return
        }

        stopPlayer()

        val player = RadioPlayer(url)
        player.setVolume(volume)
        val thread = Thread(player, "radio-stream-$url-${MinecraftClient.getInstance().player?.uuid}").also {
            it.isDaemon = true
            it.start()
        }
        activeStreamPlayer = ActiveStream(url, player, thread)
    }

    fun stopPlayer() {
        activeStreamPlayer ?: return
        activeStreamPlayer!!.player.stop()
        activeStreamPlayer = null
    }

    fun stopAll() {
        activeStreams.values.forEach { it.player.stop() }
        activeStreams.clear()
    }

    fun onRadioLoaded(pos: BlockPos) {
        knownRadios.add(pos)
    }

    fun onRadioUnloaded(pos: BlockPos) {
        knownRadios.remove(pos)
        stop(pos)
    }

    fun tick(player: PlayerEntity) {
        val world = MinecraftClient.getInstance().world ?: return
        val mc = MinecraftClient.getInstance()

        val masterVolume  = mc.options.getSoundVolume(SoundCategory.MASTER)
        val recordsVolume = mc.options.getSoundVolume(SoundCategory.RECORDS)
        val categoryVolume = masterVolume * recordsVolume

        val heldRadio = findPlayingRadioStack(mc.player!!)
        if (heldRadio != null) {
            val (stack, hand) = heldRadio
            val stationUrl = stack.nbt?.getString("StationUrl")

            if (stationUrl.isNullOrEmpty()) {
                stopPlayer()
                return
            }

            playPlayer(stationUrl, categoryVolume)
            spawnPlayerNoteParticles(world, player, hand)
        } else {
            stopPlayer()

            knownRadios.toList().forEach { pos ->
                val be = world.getBlockEntity(pos) as? RadioBlockEntity
                if (be == null) {
                    stop(pos)
                    knownRadios.remove(pos)
                    return@forEach
                }

                if (!be.isPlaying || be.station?.url.isNullOrEmpty()) {
                    stop(pos)
                    return@forEach
                }

                spawnNoteParticles(world, pos)

                val url = be.station!!.url
                val dist = player.squaredDistanceTo(Vec3d.ofCenter(pos))
                val maxDist = be.range * be.range
                val distanceVolume = if (dist >= maxDist) 0f else (1f - (dist / maxDist))
                val finalVolume = distanceVolume.toFloat() * categoryVolume

                play(pos, url, finalVolume)
            }
        }
    }

    private fun spawnPlayerNoteParticles(world: World, player: PlayerEntity, hand: Hand) {
        if (world.random.nextFloat() >= 0.2f) return

        val facing = player.horizontalFacing
        val arm = if (hand == Hand.MAIN_HAND) player.mainArm else player.mainArm.opposite
        val handSide = if (arm == Arm.RIGHT) facing.rotateYClockwise() else facing.rotateYCounterclockwise()

        // radio.json's thirdperson display: translation [0, 2.5, 0] lifts the model ~2.5/16 blocks
        // above the hand, and it's rendered at scale 0.8, so its front face sits ~0.4 blocks out
        val handHeight = player.standingEyeHeight * 0.6 + 2.5 / 16.0
        val handPos = player.pos
            .add(0.0, handHeight, 0.0)
            .add(Vec3d.of(handSide.vector).multiply(0.4))

        val front = handPos.add(Vec3d.of(facing.vector).multiply(0.4))

        world.addParticle(
            ParticleTypes.NOTE,
            front.x, front.y, front.z,
            world.random.nextDouble(), 0.0, 0.0
        )
    }

    private fun spawnNoteParticles(world: ClientWorld, pos: BlockPos) {
        if (world.random.nextFloat() >= 0.2f) return

        val facing = world.getBlockState(pos).get(RadioBlock.FACING)
        val left = Vec3d.of(facing.rotateYCounterclockwise().vector)
        val right = Vec3d.of(facing.rotateYClockwise().vector)
        val front = Vec3d.of(facing.vector)

        // block/radio.json body spans x:[2,14] y:[0,8] z:[5,11] (in 1/16ths): it sits in the
        // lower half of the block (top at y=0.5), is ~0.375 blocks wide each side of center,
        // and its front face is ~0.1875 blocks ahead of the block's center
        val base = Vec3d(pos.x + 0.5, pos.y + 0.3, pos.z + 0.5).add(front.multiply(0.1875))

        world.addParticle(
            ParticleTypes.NOTE,
            base.x + left.x * 0.375, base.y, base.z + left.z * 0.375,
            world.random.nextDouble(), 0.0, 0.0
        )
        world.addParticle(
            ParticleTypes.NOTE,
            base.x + right.x * 0.375, base.y, base.z + right.z * 0.375,
            world.random.nextDouble(), 0.0, 0.0
        )
    }
}