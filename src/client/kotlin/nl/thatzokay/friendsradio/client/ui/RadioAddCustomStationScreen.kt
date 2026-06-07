package nl.thatzokay.friendsradio.client.ui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text
import nl.thatzokay.friendsradio.client.config.customStations
import nl.thatzokay.friendsradio.client.config.saveConfig
import nl.thatzokay.friendsradio.records.Station

class RadioAddCustomStationScreen(val parentScreen: Screen) :
    Screen(Text.literal("Add Custom Station")) {

    private lateinit var nameBox: TextFieldWidget
    private lateinit var urlBox: TextFieldWidget
    private lateinit var iconBox: TextFieldWidget

    override fun init() {
        super.init()
        val centerX = width / 2
        val startY = height / 2 - 55

        nameBox = TextFieldWidget(textRenderer, centerX - 150, startY, 300, 20,
            Text.literal("Station name"))
        nameBox.setMaxLength(100)
        nameBox.setPlaceholder(Text.literal( "Station name..."))
        addDrawableChild(nameBox)

        urlBox = TextFieldWidget(textRenderer, centerX - 150, startY + 42, 300, 20,
            Text.literal( "Stream URL"))
        urlBox.setMaxLength(512)
        urlBox.setPlaceholder(Text.literal( "https://..."))
        addDrawableChild(urlBox)

        iconBox = TextFieldWidget(textRenderer, centerX - 150, startY + 84, 300, 20,
            Text.literal( "Icon URL (optional)"))
        iconBox.setMaxLength(512)
        iconBox.setPlaceholder(Text.literal( "https://... (optional)"))
        addDrawableChild(iconBox)

        addDrawableChild(ButtonWidget.builder(Text.literal( "Save")) {
            val name = nameBox.text.trim()
            val url = urlBox.text.trim()
            val icon = iconBox.text.trim()
            if (name.isNotEmpty() && url.isNotEmpty()) {
                customStations.add(Station(name, url, icon))
                saveConfig()
                client?.setScreen(parentScreen)
            }
        }.dimensions(centerX - 155, startY + 115, 145, 20).build())

        addDrawableChild(ButtonWidget.builder(Text.literal("Back")) {
            client?.setScreen(parentScreen)
        }.dimensions(centerX + 10, startY + 115, 145, 20).build())
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackgroundTexture(context)
        super.render(context, mouseX, mouseY, delta)
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 80, 0xFFFFFF)

        val centerX = width / 2
        val startY = height / 2 - 55
        context.drawText(textRenderer, Text.literal( "Name:"), centerX - 150, startY - 11, 0xAAAAAA, false)
        context.drawText(textRenderer, Text.literal( "URL:"), centerX - 150, startY + 31, 0xAAAAAA, false)
        context.drawText(textRenderer, Text.literal( "Icon:"), centerX - 150, startY + 73, 0xAAAAAA, false)
    }

    override fun shouldPause(): Boolean = false
}