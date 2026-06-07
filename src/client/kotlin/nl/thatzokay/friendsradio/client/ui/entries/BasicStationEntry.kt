package nl.thatzokay.friendsradio.client.ui.entries

import nl.thatzokay.friendsradio.records.Station
import nl.thatzokay.friendsradio.client.ui.widgets.StationListWidget

class BasicStationEntry(
    override val parent: StationListWidget<BasicStationEntry>,
    override val station: Station
) : StationEntry<BasicStationEntry>(parent, station)