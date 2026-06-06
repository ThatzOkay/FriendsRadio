package nl.thatzokay.friendsradio.client.ui.widgets

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import nl.thatzokay.friendsradio.records.FilterOption

class DropdownWidget(
    x: Int, y: Int, width: Int, height: Int,
    private val prefix: String,
    private val options: Array<FilterOption>,
    private var selectedIndex: Int = 0,
    private val onChange: ((FilterOption) -> Unit)? = null
) : ButtonWidget(
    x, y, width, height,
    Text.literal(prefix + options[selectedIndex].name + " ▼"),
    { widget -> (widget as DropdownWidget).toggle() },
    { _ -> Text.literal(options[selectedIndex].name) }
) {
    companion object {
        private val groups = mutableMapOf<String, MutableList<DropdownWidget>>()

        fun registerGroup(groupId: String, vararg widgets: DropdownWidget) {
            groups[groupId] = widgets.toMutableList()
        }

        fun unregisterGroup(groupId: String) {
            groups.remove(groupId)
        }
    }

    var isOpen: Boolean = false
        private set

    fun getSelected(): FilterOption = options[selectedIndex]

    fun toggle() {
        if (!isOpen) closeOthersInGroup()
        isOpen = !isOpen
    }

    fun close() { isOpen = false }

    private fun closeOthersInGroup() {
        groups.values
            .filter { it.contains(this) }
            .flatten()
            .filter { it !== this }
            .forEach { it.close() }
    }

    override fun renderButton(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderButton(context, mouseX, mouseY, delta)
        renderPopup(context!!, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (isOpen && handlePopupClick(mouseX, mouseY, button)) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    fun renderPopup(context: DrawContext, mouseX: Int, mouseY: Int) {
        if (!isOpen) return
        val itemHeight = 15
        val popupHeight = options.size * itemHeight

        context.matrices.push()
        context.matrices.translate(0f, 0f, 1000f)

        context.fill(x, y + height, x + width, y + height + popupHeight, 0xEE000000.toInt())
        context.fill(x, y + height, x + width, y + height + 1,           0xFF555555.toInt())
        context.fill(x, y + height + popupHeight - 1, x + width, y + height + popupHeight, 0xFF555555.toInt())
        context.fill(x, y + height, x + 1, y + height + popupHeight,     0xFF555555.toInt())
        context.fill(x + width - 1, y + height, x + width, y + height + popupHeight, 0xFF555555.toInt())

        for (i in options.indices) {
            val itemY = y + height + (i * itemHeight)
            val isHovered = mouseX in x..(x + width) && mouseY >= itemY && mouseY < itemY + itemHeight
            if (isHovered) context.fill(x + 1, itemY, x + width - 1, itemY + itemHeight, 0xFF555555.toInt())
            val textColor = when {
                i == selectedIndex -> 0xFFFFFF00.toInt()
                isHovered          -> 0xFFFFFFFF.toInt()
                else               -> 0xFFAAAAAA.toInt()
            }
            context.drawText(MinecraftClient.getInstance().textRenderer, options[i].name, x + 5, itemY + 4, textColor, false)
        }

        context.matrices.pop()
    }

    fun handlePopupClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isOpen || button != 0) return false
        val popupHeight = options.size * 15
        if (mouseX >= x && mouseX <= x + width && mouseY >= y + height && mouseY < y + height + popupHeight) {
            val clickedIndex = ((mouseY - (y + height)) / 15).toInt()
            if (clickedIndex in options.indices) {
                selectedIndex = clickedIndex
                message = Text.literal(prefix + options[selectedIndex].name + " ▼")
                close()
                onChange?.invoke(options[selectedIndex])
                return true
            }
        }
        return false // No longer self-closing on outside clicks
    }
}