package nl.thatzokay.friendsradio.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import nl.thatzokay.friendsradio.records.Station

object RadioUpdatePayload {
    val ID = Identifier("friendsradio", "radio_update")

    fun encode(buf: PacketByteBuf, pos: BlockPos, station: Station, isPlaying: Boolean) {
        buf.writeBlockPos(pos)
        buf.writeString(station.name)
        buf.writeString(station.url)
        buf.writeString(station.favicon)
        buf.writeBoolean(isPlaying)
    }

    fun decode(buf: PacketByteBuf): RadioUpdateData {
        return RadioUpdateData(
            pos         = buf.readBlockPos(),
            stationName = buf.readString(),
            stationUrl  = buf.readString(),
            stationFavicon = buf.readString(),
            isPlaying   = buf.readBoolean()
        )
    }
}

data class RadioUpdateData(
    val pos: BlockPos,
    val stationName: String,
    val stationUrl: String,
    val stationFavicon: String,
    val isPlaying: Boolean
)