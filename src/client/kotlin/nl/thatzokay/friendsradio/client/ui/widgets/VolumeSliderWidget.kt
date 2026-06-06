package nl.thatzokay.friendsradio.client.ui.widgets

import net.minecraft.client.gui.widget.SliderWidget
import net.minecraft.text.Text
import kotlin.math.roundToInt

class VolumeSliderWidget(x: Int, y: Int, width: Int, height: Int, title: Text, value: Double, val callback: (Double) -> Unit) :
    SliderWidget(x, y, width, height, title, value) {

    init {
        updateMessage()
    }

    override fun updateMessage() {
        message = Text.literal("Volume: ${(value * 100.0).roundToInt()} %")
    }

    override fun applyValue() {
        callback(value)
    }
}