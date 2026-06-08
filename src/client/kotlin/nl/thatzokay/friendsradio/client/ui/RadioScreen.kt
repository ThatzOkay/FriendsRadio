package nl.thatzokay.friendsradio.client.ui

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.Text
import nl.thatzokay.friendsradio.block.RadioBlockEntity
import nl.thatzokay.friendsradio.client.audio.RadioAudioManager
import nl.thatzokay.friendsradio.client.config.customStations
import nl.thatzokay.friendsradio.client.config.favorites
import nl.thatzokay.friendsradio.client.ui.entries.BasicStationEntry
import nl.thatzokay.friendsradio.client.ui.widgets.DropdownWidget
import nl.thatzokay.friendsradio.client.ui.widgets.StationListWidget
import nl.thatzokay.friendsradio.client.utils.drawMarqueeText
import nl.thatzokay.friendsradio.client.utils.fallbackIcon
import nl.thatzokay.friendsradio.client.utils.getIcon
import nl.thatzokay.friendsradio.network.RadioItemUpdatePayload
import nl.thatzokay.friendsradio.network.RadioUpdatePayload
import nl.thatzokay.friendsradio.records.FilterOption
import nl.thatzokay.friendsradio.records.Station
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CompletableFuture

class RadioScreen(val blockEntity: RadioBlockEntity?, val itemStack: ItemStack?) : Screen(Text.literal("Radio")) {

    private var listWidget: StationListWidget<BasicStationEntry>? = null

    private var searchBox: TextFieldWidget? = null
    private var searchButton: ButtonWidget? = null
    private var countryDropdown: DropdownWidget? = null
    private var genreDropdown: DropdownWidget? = null

    private var showOnlyFavorites: Boolean = false

    private val countries: Array<FilterOption> = arrayOf(
        FilterOption("Worldwide", ""),
        FilterOption("Poland", "PL"),
        FilterOption("USA", "US"),
        FilterOption("UK", "GB"),
        FilterOption("Sweden", "SE"),
        FilterOption("Germany", "DE"),
        FilterOption("Netherlands", "NL"),
        FilterOption("France", "FR"),
        FilterOption("Italy", "IT"),
        FilterOption("Japan", "JP")
    )

    private val genres: Array<FilterOption> = arrayOf(
        FilterOption("All", ""),
        FilterOption("Rock", "rock"),
        FilterOption("Pop", "pop"),
        FilterOption("Hip Hop", "hiphop"),
        FilterOption("Electronic", "electronic"),
        FilterOption("Country", "country"),
        FilterOption("Jazz", "jazz"),
        FilterOption("Classical", "classical"),
        FilterOption("Reggae", "reggae"),
        FilterOption("Blues", "blues"),
        FilterOption("R&B", "rnb")
    )

    override fun init() {
        super.init()

        val centerX = width / 2

        searchBox = TextFieldWidget(
            client!!.textRenderer, centerX - 150, 30, 140, 20,
            Text.literal("Search stations...")
        )

        addDrawableChild(searchBox)

        searchButton = ButtonWidget.builder(Text.literal("Search"), {
            if (searchBox == null || searchBox!!.text.isEmpty()) return@builder
            performSearch(searchBox!!.text)
        }).dimensions(centerX - 5, 30, 70, 20).build()
        addDrawableChild(searchButton)

        addDrawableChild(ButtonWidget.builder(Text.literal("★ Favorites")) { button ->
            showOnlyFavorites = !showOnlyFavorites
            if (showOnlyFavorites) {
                button.message = Text.literal("🔍 Search")
                searchBox!!.visible = false
                searchButton!!.visible = false
                countryDropdown!!.visible = false
                genreDropdown!!.visible = false
                loadFavorites()
            } else {
                button.message = Text.literal("★ Favorites")
                searchBox!!.visible = true
                searchButton!!.visible = true
                countryDropdown!!.visible = true
                genreDropdown!!.visible = true
                performSearch(searchBox!!.text)
            }
        }.dimensions(centerX + 70, 30, 80, 20).build())

        countryDropdown = DropdownWidget(centerX - 150, 55, 145, 20, "Country: ", countries, 0, { performSearch(searchBox!!.text) })
        addDrawableChild(countryDropdown)

        genreDropdown = DropdownWidget(centerX + 5, 55, 145, 20, "Genre: ", genres, 0, { performSearch(searchBox!!.text) })
        addDrawableChild(genreDropdown)

        DropdownWidget.registerGroup("filters", countryDropdown!!, genreDropdown!!)

        listWidget = StationListWidget(MinecraftClient.getInstance(), blockEntity, itemStack, width, height - 40, 85, 25,
            ::BasicStationEntry)
        addDrawableChild(listWidget)

        performSearch("")

        val bottomButtonsY = this.height - 52
        val bW = 100
        val bGap = 5
        val totalW = bW * 3 + bGap * 2
        val bStartX = centerX - totalW / 2

        addDrawableChild(
            ButtonWidget.builder(Text.literal("⏹ Stop Radio"), {
                if (blockEntity != null) {
                    blockEntity.station = null
                    blockEntity.isPlaying = false

                    val buf = PacketByteBuf(Unpooled.buffer())

                    RadioUpdatePayload.encode(
                        buf,
                        blockEntity.pos,
                        Station("", "", ""),
                        false
                    )

                    ClientPlayNetworking.send(RadioUpdatePayload.ID, buf)
                    blockEntity.markDirtyAndSync()
                } else if (itemStack != null) {
                    itemStack.nbt?.putString("StationName", "")
                    itemStack.nbt?.putString("StationUrl", "")
                    itemStack.nbt?.putString("StationFavicon", "")
                    itemStack.nbt?.putBoolean("IsPlaying", false)

                    val buf = PacketByteBuf(Unpooled.buffer())
                    RadioItemUpdatePayload.encode(
                        buf,
                        Station("","",""),
                        false
                    )

                    ClientPlayNetworking.send(RadioItemUpdatePayload.ID, buf)
                    MinecraftClient.getInstance().player?.inventory?.markDirty()
                }
            })
                .dimensions(bStartX + (bW + bGap) * 1, bottomButtonsY, bW, 20).build()
        )

//        addDrawableChild(
//            ButtonWidget.builder(Text.literal("\uD83C\uDFB5 History"), {})
//                .dimensions(bStartX + bW + bGap, bottomButtonsY, bW, 20).build()
//        )

//        addDrawableChild(
//            VolumeSliderWidget(bStartX + (bW + bGap) * 1, bottomButtonsY, bW, 20, Text.empty(),
//                (blockEntity?.volume?.toDouble()
//                    ?: (itemStack?.nbt?.getFloat("Volume")
//                        ?.toDouble()
//                        ?: 1.0))
//            ) { volume ->
//
//                if (blockEntity != null) {
//                    val be = blockEntity ?: return@VolumeSliderWidget
//                    be.volume = volume.toFloat()
//
//                    val buf = PacketByteBufs.create()
//                    RadioUpdatePayload.encode(
//                        buf,
//                        be.pos,
//                        be.station ?: Station("", "", ""),
//                        volume.toFloat(),
//                        be.isPlaying
//                    )
//                    ClientPlayNetworking.send(RadioUpdatePayload.ID, buf)
//                } else {
//                    itemStack?.orCreateNbt?.putFloat("Volume", volume.toFloat())
//
//                    val buf = PacketByteBufs.create()
//                    RadioItemUpdatePayload.encode(
//                        buf,
//                        volume.toFloat())
//                    ClientPlayNetworking.send(RadioItemUpdatePayload.ID, buf)
//                }
//            }
//        )

        val bW2 = (totalW - bGap) / 3

//        addDrawableChild(
//            ButtonWidget.builder(Text.literal("⚙ Settings"), {})
//                .dimensions(bStartX, bottomButtonsY + 25, bW2, 20).build()
//        )

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Custom stations"), {
                MinecraftClient.getInstance().setScreen(RadioCustomStationsScreen(this@RadioScreen))
            })
                .dimensions(bStartX + bW2 + bGap, bottomButtonsY + 25, bW2, 20).build()
        )

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Close"), {
                if (MinecraftClient.getInstance() != null) MinecraftClient.getInstance().setScreen(null)
            })
                .dimensions(bStartX + (bW2 + bGap) * 2, bottomButtonsY + 25, bW2, 20).build()
        )
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderBackgroundTexture(context)
        super.render(context, mouseX, mouseY, delta)

        val title = if (showOnlyFavorites) Text.literal("Favorites") else Text.literal("Radio")

        context!!.drawCenteredTextWithShadow(client!!.textRenderer, title, width / 2 - client!!.textRenderer.getWidth(title) / 2, 10, 0xFFFFFF)

        val radioInfo = RadioAudioManager.knownRadios.find { it.pos == blockEntity?.pos }

        if (radioInfo != null) {
            val activeStream = RadioAudioManager.activeStreams[radioInfo.pos]

            if (activeStream?.lastSongName?.isNotEmpty() == true) {
                val songText = "♪ " + activeStream.lastSongName
                val barWidth = 310
                val barX = width / 2 - barWidth / 2
                val barY = height - 75
                val barHeight = 20

                context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xAA000000.toInt())
                context.fill(barX, barY, barX + barWidth, barY + 1, 0xFF555555.toInt())
                context.fill(barX, barY + barHeight - 1, barX + barWidth, barY + barHeight, 0xFF555555.toInt())
                context.fill(barX, barY, barX + 1, barY + barHeight, 0xFF555555.toInt())
                context.fill(barX + barWidth - 1, barY, barX + barWidth, barY + barHeight, 0xFF555555.toInt())

                val artworkUrl = activeStream.currentArtworkUrl
                val iconSize = 64
                if (artworkUrl.isNotEmpty()) {
                    val artIcon = getIcon(artworkUrl)
                    val displayIcon = artIcon ?: fallbackIcon

                    context.fill(barX - iconSize - 4, barY, barX - 2, barY + iconSize, -0x56000000)
                    context.fill(barX - iconSize - 4, barY, barX - 2, barY + 1, -0xaaaaab)
                    context.fill(barX - iconSize - 4, barY + iconSize - 1, barX - 2, barY + iconSize, -0xaaaaab)
                    context.fill(barX - iconSize - 4, barY, barX - iconSize - 3, barY + iconSize, -0xaaaaab)
                    context.fill(barX - 3, barY, barX - 2, barY + iconSize, -0xaaaaab)
                    context.drawTexture(
                        displayIcon,
                        barX - iconSize - 3,
                        barY,
                        0F,
                        0F,
                        iconSize,
                        iconSize,
                        iconSize,
                        iconSize
                    )
                }
                val textY = barY + (barHeight - client!!.textRenderer.fontHeight) / 2;
                drawMarqueeText(context, client!!.textRenderer, songText, barX + 5, textY, barWidth - 10, 0x55FF55, true)
            }
        }

        if (!showOnlyFavorites) {
            if (countryDropdown != null) countryDropdown!!.renderPopup(context, mouseX, mouseY)
            if (genreDropdown != null) genreDropdown!!.renderPopup(context, mouseX, mouseY)
        }
    }

    override fun shouldPause(): Boolean {
        return false
    }

    override fun close() {
        DropdownWidget.unregisterGroup("filters")
        super.close()
    }

    private fun performSearch(query: String) {
        listWidget?.clearStations()
        listWidget?.addStation(Station("Downloading stations...", "", ""))
        CompletableFuture.runAsync {
            var apiUrl = "https://all.api.radio-browser.info/json/stations/search?limit=50&hidebroken=true&order=clickcount&reverse=true&codec=MP3"

            val currentCountry = countryDropdown?.getSelected() ?: countries[0]
            val currentGenre = genreDropdown?.getSelected() ?: genres[0]

            currentCountry.value?.isEmpty()?.let {
                if (!it) {
                    apiUrl += "&countrycode=${currentCountry.value}"
                }
            }

            currentGenre.value?.isEmpty()?.let {
                if (!it) {
                    apiUrl += "&tag=${currentGenre.value}"
                }
            }

            query.isEmpty().let {
                if (!it) {
                    apiUrl += "&name=${URLEncoder.encode(query, StandardCharsets.UTF_8)}"
                }
            }

            try {
                val client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()

                val request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "MinecraftRadioMod/1.0")
                    .GET()
                    .build()

                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                val array = JsonParser.parseString(response.body()).asJsonArray

                MinecraftClient.getInstance().execute {
                    listWidget?.clearStations()
                    if (array.size() == 0) {
                        listWidget?.addStation(Station("No results found.", "", ""))
                        return@execute
                    }

                    for (element in array) {
                        val obj: JsonObject = element.asJsonObject
                        val stationName = if (obj.has("name") && !obj.get("name").isJsonNull)
                            obj.get("name").asString.trim()
                        else
                            "Unknown Station"
                        val streamUrl = if (obj.has("url_resolved") && !obj.get("url_resolved").isJsonNull)
                            obj.get("url_resolved").asString
                        else
                            ""
                        val faviconUrl = if (obj.has("favicon") && !obj.get("favicon").isJsonNull)
                            obj.get("favicon").asString
                        else
                            ""

                        if (streamUrl.isNotEmpty()) {
                            listWidget?.addStation(Station(stationName, streamUrl, faviconUrl))
                        }
                    }


                    for (customStation in customStations) {
                        listWidget?.addStationToTop(customStation)
                    }

                    listWidget?.scrollAmount = 0.0
                }
            } catch (_: Exception) {
                MinecraftClient.getInstance().execute {
                    listWidget?.clearStations()
                    listWidget?.addStation(Station("Internet error.", "", ""))
                }
            }
        }
    }

    private fun loadFavorites() {
        listWidget?.clearStations()
        if(favorites.isEmpty()) {
            listWidget?.addStation(Station("No favorites found.", "", ""))
            listWidget?.addStation(Station("Click the star [☆] to add one!", "", ""))
            return
        } else {
            for (favorite in favorites) {
                listWidget?.addStation(favorite)
            }
        }
    }
}