package nl.thatzokay.friendsradio.client.ui.entries

import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Identifier
import nl.thatzokay.friendsradio.client.config.customStations
import nl.thatzokay.friendsradio.client.config.removeCustomStation
import nl.thatzokay.friendsradio.client.ui.records.Station
import nl.thatzokay.friendsradio.client.ui.widgets.StationListWidget
import nl.thatzokay.friendsradio.client.utils.drawMarqueeText
import nl.thatzokay.friendsradio.client.utils.fallbackIcon
import nl.thatzokay.friendsradio.client.utils.getIcon
import java.util.*

class CustomStationEntry(override val parent: StationListWidget<CustomStationEntry>, override val station: Station) : StationEntry<CustomStationEntry>(parent, station) {

    override fun render(
        context: DrawContext,
        index: Int,
        top: Int,
        left: Int,
        entryWidth: Int,
        entryHeight: Int,
        mouseX: Int,
        mouseY: Int,
        hovered: Boolean,
        tickDelta: Float
    ) {
        if (hovered && !station.url.isEmpty()) {
        context.fill(left, top, left + entryWidth, top + entryHeight, 0x40FFFFFF)
    }

        var textOffset = 5

        if (!station.url.isEmpty()) {
            val deleteHovered = mouseX >= left && mouseX <= left + 15 && mouseY >= top && mouseY <= top + top
            val deleteIcon = if (deleteHovered) "§c✗" else "§8✗"

            context.drawText(parent.client.textRenderer, deleteIcon, left + 5, top + 5, 0xFFFFFF, false)

            if (!station.favicon.isEmpty()) {
                val icon = getIcon(station.favicon)
                context.drawTexture(
                    Objects.requireNonNullElse<Identifier>(icon, fallbackIcon),
                    left + 20, top + 2, 0.0f, 0.0f, 20, 20, 20,
                    20
                )
            } else {
                context.drawTexture(fallbackIcon, left + 20, top + 2, 0.0f, 0.0f, 20, 20, 20, 20)
            }
            textOffset = 45
        }

        val textColor = if (hovered && station.url.isNotEmpty()) 0xFFFF55 else 0xFFFFFF
        drawMarqueeText(context, parent.client.textRenderer, station.name,
            left + textOffset, top + 5, entryWidth - textOffset - 15, textColor,
            hovered)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!this.station.url.isEmpty() && button == 0) {
            val left = parent.rowLeft
            val entryIndex = parent.children().indexOf(this)
            val top: Int = parent.getStationTop(entryIndex)

            val isDeleteClicked = mouseX >= left && mouseX <= left + 15 && mouseY >= top && mouseY <= top + 22

            if (isDeleteClicked) {
                removeCustomStation(station.url)
                parent.removeStation(station)
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }
}