package nl.thatzokay.friendsradio.client.ui.widgets

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.toast.Toast
import net.minecraft.client.toast.ToastManager
import nl.thatzokay.friendsradio.client.config.blacklist
import nl.thatzokay.friendsradio.client.config.saveConfig
import nl.thatzokay.friendsradio.records.Station
import nl.thatzokay.friendsradio.client.utils.fallbackIcon
import nl.thatzokay.friendsradio.client.utils.getIcon
import org.lwjgl.glfw.GLFW

class ToastWidget(val station: Station, val songName: String, durationSeconds: Int, size: Int) : Toast {

    val widths: IntArray  = intArrayOf(160, 190, 220)
    val heights: IntArray = intArrayOf(32,  46,  60)
    val icons: IntArray   = intArrayOf(20,  36,  50)

    private val toastSize = size.coerceIn(0, 2)
    private var timeLeftMs: Long = durationSeconds * 1000L
    private var lastRenderTime: Long = -1L
    private var wasClicked = false

    override fun draw(context: DrawContext?, manager: ToastManager?, startTime: Long): Toast.Visibility {
        context ?: return Toast.Visibility.HIDE

        val now = net.minecraft.util.Util.getMeasuringTimeMs()

        if (lastRenderTime < 0) {
            lastRenderTime = now
            return Toast.Visibility.SHOW
        }

        val delta = now - lastRenderTime
        lastRenderTime = now

        val w    = widths[toastSize]
        val h    = heights[toastSize]
        val icon = icons[toastSize]

        val mc      = MinecraftClient.getInstance()
        val mouseX  = mc.mouse.x * mc.window.scaledWidth  / mc.window.width
        val mouseY  = mc.mouse.y * mc.window.scaledHeight / mc.window.height

        val matrix = context.matrices.peek().positionMatrix
        val toastX = matrix.get(3, 0)
        val toastY = matrix.get(3, 1)

        val hovered = mouseX >= toastX && mouseX <= toastX + w &&
                mouseY >= toastY && mouseY <= toastY + h

        if (!hovered) timeLeftMs -= delta
        if (timeLeftMs <= 0) return Toast.Visibility.HIDE

        context.fill(0, 0, w, h, 0xDD000000.toInt())

        val iconY = (h - icon) / 2

        if (station.favicon.isNotEmpty()) {
            val favicon = getIcon(station.favicon)
            if (favicon != null) {
                context.drawTexture(favicon, 5, iconY, 0.0f, 0.0f, icon, icon, icon, icon)
            } else {
                renderFallback(context, icon, iconY)
            }
        } else {
            renderFallback(context, icon, iconY)
        }

        val textRenderer = mc.textRenderer
        val textX    = icon + 10
        val maxTextW = w - textX - 15
        val stationY = h / 2 - textRenderer.fontHeight - 2
        val songY    = h / 2 + 2

        context.drawText(textRenderer, textRenderer.trimToWidth(station.name, maxTextW), textX, stationY, 0x55FF55, false)

        if (songName.isNotEmpty()) {
            val textWidth = textRenderer.getWidth(songName)
            if (textWidth <= maxTextW) {
                context.drawText(textRenderer, songName, textX, songY, 0xFFFFFF, false)
            } else {
                val overflow     = textWidth - maxTextW
                val totalScroll  = overflow + 50
                val offset       = ((now * 0.04) % (totalScroll * 2)).toInt()
                val scrollX      = if (offset > totalScroll) (totalScroll * 2) - offset else offset
                val clampedScroll = scrollX.coerceIn(0, overflow)
                context.enableScissor(textX, songY, textX + maxTextW, songY + textRenderer.fontHeight)
                context.drawText(textRenderer, songName, textX - clampedScroll, songY, 0xFFFFFF, false)
                context.disableScissor()
            }
        }

        val btnW = 10
        val btnH = 10
        val btnX = w - btnW - 3
        val btnY = h - btnH - 3

        val btnHovered = mouseX >= toastX + btnX && mouseX <= toastX + btnX + btnW &&
                mouseY >= toastY + btnY && mouseY <= toastY + btnY + btnH

        context.fill(btnX, btnY, btnX + btnW, btnY + btnH, if (btnHovered) -0xcccd else -0x55780000)
        context.fill(btnX + 2, btnY + btnH / 2 - 1, btnX + btnW - 2, btnY + btnH / 2 + 1, -0x1)

        val mouseLeftDown = GLFW.glfwGetMouseButton(mc.window.handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS

        if (mouseLeftDown && !wasClicked) {
            wasClicked = true
            if (btnHovered) {
                blacklist.add(station.url)
                saveConfig()
                return Toast.Visibility.HIDE
            }
        } else if (!mouseLeftDown) {
            wasClicked = false
        }

        return Toast.Visibility.SHOW
    }

    private fun renderFallback(context: DrawContext, iconSize: Int, iconY: Int) {
        context.drawTexture(fallbackIcon, 5, iconY, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize)
    }
}