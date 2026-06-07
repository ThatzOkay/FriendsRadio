package nl.thatzokay.friendsradio.block

import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import nl.thatzokay.friendsradio.ModBlockEntities
import nl.thatzokay.friendsradio.records.Station

class RadioBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.RADIO_BLOCK_ENTITY, pos, state) {

    var station: Station? = null
    var volume: Float = 1.0f
    var isPlaying: Boolean = false
    var range: Float = 32.0f

    override fun readNbt(nbt: NbtCompound) {  // ← remove the ?, Yarn's signature is non-null
        super.readNbt(nbt)
        val name    = nbt.getString("StationName")
        val url     = nbt.getString("StationUrl")
        val favicon = nbt.getString("StationFavIcon")
        // Only restore station if we actually saved one
        station     = if (url.isNotEmpty()) Station(name, url, favicon) else null
        isPlaying   = nbt.getBoolean("IsPlaying")
        range       = nbt.getFloat("Range").let { if (it == 0.0f) 32.0f else it }
    }

    override fun writeNbt(nbt: NbtCompound) {  // ← remove the ?
        super.writeNbt(nbt)
        nbt.putString("StationName",    station?.name    ?: "")  // ← null → ""
        nbt.putString("StationUrl",     station?.url     ?: "")
        nbt.putString("StationFavIcon", station?.favicon ?: "")
        nbt.putBoolean("IsPlaying",     isPlaying)
        nbt.putFloat("Range",           range)
    }

    fun markDirtyAndSync() {
        markDirty()
        val serverWorld = world as? ServerWorld ?: return
        serverWorld.chunkManager.markForUpdate(pos)
    }

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> =
        BlockEntityUpdateS2CPacket.create(this)

    override fun toInitialChunkDataNbt(): NbtCompound =
        createNbt()
}