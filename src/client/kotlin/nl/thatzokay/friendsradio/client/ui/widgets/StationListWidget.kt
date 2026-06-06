package nl.thatzokay.friendsradio.client.ui.widgets

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.EntryListWidget
import nl.thatzokay.friendsradio.client.ui.entries.StationEntry
import nl.thatzokay.friendsradio.client.ui.records.Station

class StationListWidget<T: StationEntry<T>>(
    val client: MinecraftClient,
    width: Int,
    height: Int,
    y: Int,
    itemHeight: Int,
    private val entryFactory: (StationListWidget<T>, Station) -> T
) : EntryListWidget<T>(client, width, height, y, height - 40, itemHeight) {

    override fun getRowWidth(): Int {
        return 310
    }

    fun clearStations() {
        clearEntries()
    }

    fun addStation(station: Station) {
        addEntry(entryFactory(this, station))
    }

    fun removeStation(station: Station) = removeEntry(
        children().firstOrNull { it.station == station }
    )

    fun addStationToTop(station: Station) {
        addEntryToTop(entryFactory(this, station))
    }

    fun getStationTop(index: Int): Int {
        return super.getRowTop(index)
    }

    override fun appendNarrations(builder: NarrationMessageBuilder?) {

    }
}