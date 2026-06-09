package nl.thatzokay.friendsradio.client.ui.entries

import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import nl.thatzokay.friendsradio.records.Station
import nl.thatzokay.friendsradio.client.utils.drawMarqueeText
import nl.thatzokay.friendsradio.client.utils.fallbackIcon
import nl.thatzokay.friendsradio.client.utils.getIcon
import nl.thatzokay.friendsradio.client.ui.widgets.StationListWidget
import nl.thatzokay.friendsradio.client.utils.isFavorite
import nl.thatzokay.friendsradio.client.utils.logger
import nl.thatzokay.friendsradio.client.utils.toggleFavorite
import nl.thatzokay.friendsradio.network.RadioItemUpdatePayload
import nl.thatzokay.friendsradio.network.RadioUpdatePayload
import java.util.*

open class StationEntry<T : StationEntry<T>>(
    open val parent: StationListWidget<T>,
    open val station: Station
) : AlwaysSelectedEntryListWidget.Entry<T>() {
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
        } else if (parent.selectedOrNull == this) {
            context.fill(left, top, left + entryWidth, top + entryHeight, 0x60808080)
        }

        var textOffset = 5

        if (!station.url.isEmpty()) {
            val favorite = isFavorite(station.url)
            val starHovered = mouseX >= left && mouseX <= left + 15 && mouseY >= top && mouseY <= top + entryHeight

            val star = when {
                favorite -> "§e★"
                starHovered -> "§f☆"
                else -> "§8☆"
            }
            context.drawText(parent.client.textRenderer, star, left + 5, top + 5, 0xFFFFFF, false)

            if (!station.favicon.isEmpty()) {
                val icon = getIcon(station.favicon)
                //logger.info("Icon: {}", icon)
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

    @Suppress("UNCHECKED_CAST")
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if(station.url.isEmpty()) {
            return false
        }

        if (mouseX >= parent.rowLeft && mouseX <= parent.rowLeft + 15) {
            toggleFavorite(station)
            return true
        }

        if (button == 0) {
            parent.setSelected(this as T?)

            val be = parent.blockEntity
            val itemStack = parent.itemStack

            if (be != null) {
                be.station = station
                be.isPlaying = true

                val buf = PacketByteBuf(Unpooled.buffer())
                RadioUpdatePayload.encode(
                    buf,
                    be.pos,
                    station,
                    true
                )

                ClientPlayNetworking.send(RadioUpdatePayload.ID, buf)
                be.markDirtyAndSync()
                return true
            } else if (itemStack != null) {
                val nbt = itemStack.orCreateNbt
                nbt.putString("StationName", station.name)
                nbt.putString("StationUrl", station.url)
                nbt.putString("StationFavicon", station.favicon)
                nbt.putBoolean("IsPlaying", true)

                val buf = PacketByteBuf(Unpooled.buffer())
                RadioItemUpdatePayload.encode(
                    buf,
                    station,
                    true
                )

                ClientPlayNetworking.send(RadioItemUpdatePayload.ID, buf)
                MinecraftClient.getInstance().player?.inventory?.markDirty()
            }

            return true
        }
        return false
    }

    override fun getNarration(): Text? {
        return Text.literal(station.name)
    }
}