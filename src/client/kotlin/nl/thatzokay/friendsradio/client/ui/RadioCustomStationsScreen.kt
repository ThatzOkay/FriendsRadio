package nl.thatzokay.friendsradio.client.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import nl.thatzokay.friendsradio.client.config.customStations
import nl.thatzokay.friendsradio.client.ui.entries.CustomStationEntry
import nl.thatzokay.friendsradio.records.Station
import nl.thatzokay.friendsradio.client.ui.widgets.StationListWidget

class RadioCustomStationsScreen(val parentScreen: Screen) :
    Screen(Text.literal("Custom Stations")) {

    override fun init() {
        super.init()
        val cx = this.width / 2

        val list = StationListWidget(MinecraftClient.getInstance(),
            null,
            null,
            width,
            height - 40, 85, 25,
            ::CustomStationEntry)
        if (customStations.isEmpty()) {
            list.addStation(Station(
                "No custom station yet. Click below to add one", "", ""
            ))
        } else {
            for (station in customStations) {
                list.addStation(station)
            }
        }

        addDrawableChild(list)

        addDrawableChild(ButtonWidget.builder(Text.literal("Add custom station")) {
            MinecraftClient.getInstance().setScreen(RadioAddCustomStationScreen(this@RadioCustomStationsScreen))
        }.dimensions(cx - 155, height - 30, 145, 20).build())

        addDrawableChild(ButtonWidget.builder(Text.literal("Back")) {
            MinecraftClient.getInstance().setScreen(parentScreen)
        }.dimensions(cx + 10, height - 30, 145, 20).build())
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackgroundTexture(context)
        super.render(context, mouseX, mouseY, delta)
        context!!.drawCenteredTextWithShadow(client!!.textRenderer, title, width / 2, 10, 0xFFFFFF)
    }

    override fun shouldPause(): Boolean {
        return false
    }
}