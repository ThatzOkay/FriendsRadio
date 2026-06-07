package nl.thatzokay.friendsradio.network

import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import nl.thatzokay.friendsradio.records.Station

object RadioItemUpdatePayload {
    val ID = Identifier("friendsradio", "radio_item_update")

    fun encode(buf: PacketByteBuf, station: Station, isPlaying: Boolean) {
        buf.writeString(station.name)
        buf.writeString(station.url)
        buf.writeString(station.favicon)
        buf.writeBoolean(isPlaying)
    }

    fun decode (buf: PacketByteBuf): RadioItemUpdateData {
        return RadioItemUpdateData(
            stationName = buf.readString(),
            stationUrl  = buf.readString(),
            stationFavicon = buf.readString(),
            isPlaying   = buf.readBoolean()
        )
    }

}

data class RadioItemUpdateData(
    val stationName: String,
    val stationUrl: String,
    val stationFavicon: String,
    val isPlaying: Boolean
)