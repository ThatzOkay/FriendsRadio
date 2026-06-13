package nl.thatzokay.friendsradio.client.utils

import com.google.gson.JsonParser
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import nl.thatzokay.friendsradio.ModBlocks
import nl.thatzokay.friendsradio.client.config.favorites
import nl.thatzokay.friendsradio.client.config.saveConfig
import nl.thatzokay.friendsradio.records.Station
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import org.apache.batik.transcoder.image.PNGTranscoder
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

val iconCache: MutableMap<String, Optional<Identifier>> = ConcurrentHashMap()

val downloadingIcons: MutableSet<String> = ConcurrentHashMap.newKeySet()

val fallbackIcon = Identifier("friendsradio", "icon.png")

private val EMPTY_ICON = Optional.empty<Identifier>()

val logger: org.slf4j.Logger = LoggerFactory.getLogger("friendsradio")


fun toggleFavorite(station: Station) {
    if (isFavorite(station.url)) favorites.removeIf { f: Station? -> f?.url == station.url }
    else favorites.add(station)
    saveConfig()
}

fun isFavorite(url: String?): Boolean {
    return favorites.stream().anyMatch { f: Station? -> f?.url == url }
}

fun findPlayingRadioStack(player: PlayerEntity): Pair<ItemStack, Hand>? {
    for (hand in Hand.entries) {
        val stack = player.getStackInHand(hand)
        if (stack.isOf(ModBlocks.RADIO_ITEM) && stack.nbt?.getBoolean("IsPlaying") == true) {
            return stack to hand
        }
    }
    return null
}

fun findRadioStack(player: PlayerEntity): Pair<ItemStack, Hand>? {
    for (hand in Hand.entries) {
        val stack = player.getStackInHand(hand)
        if (stack.isOf(ModBlocks.RADIO_ITEM)) {
            return stack to hand
        }
    }
    return null
}

fun drawMarqueeText(
    context: DrawContext,
    font: TextRenderer,
    text: String,
    x: Int,
    y: Int,
    width: Int,
    color: Int,
    isHovered: Boolean
) {
    val textWidth = font.getWidth(text)
    if (textWidth == width) {
        context.drawText(font, text, x, y, color, false)
    } else {
        if (isHovered) {
            val time = Util.getMeasuringTimeMs()
            val speed = 0.08
            val overflow = textWidth - width
            val totalScroll = overflow + 100
            val offset = ((time * speed) % (totalScroll * 2)).toInt()
            var scrollX = if (offset > totalScroll) (totalScroll * 2) - offset else offset
            scrollX = max(0, min(scrollX, overflow))
            context.enableScissor(x, y, x + width, y + font.fontHeight)
            context.drawText(font, text, x - scrollX, y, color, false)
            context.disableScissor()
        } else {
            val cut = font.trimToWidth(text, width - font.getWidth("...")) + "..."
            context.drawText(font, cut, x, y, color, false)
        }
    }
}

fun getIcon(iconUrl: String): Identifier? {
    if (iconUrl.isEmpty()) {
        logger.error("[FreindsRadio] iconUrl empty")
        return null
    }
    if (iconUrl.startsWith("data:")) {
        return try {
            val raw = iconUrl.substringAfter("base64,")
            val padded = raw + "=".repeat((4 - raw.length % 4) % 4)
            val bytes = Base64.getDecoder().decode(padded)
            registerIconTexture(iconUrl, NativeImage.read(bytes.inputStream()))
        } catch (e: Exception) {
            logger.warn("Failed to decode base64 icon: ${e.message}")
            iconCache[iconUrl] = EMPTY_ICON
            null
        }
    }
    if (!iconUrl.startsWith("http://") && !iconUrl.startsWith("https://")) {
        logger.error("[FreindsRadio] iconUrl invalid $iconUrl")
        return null
    }
    val cachedIcon = iconCache[iconUrl]
    if (cachedIcon != null) {
        //logger.info("[FreindsRadio] returning cached icon: $iconUrl")
        return cachedIcon.orElse(null)
    }
    if (downloadingIcons.add(iconUrl)) {
        CompletableFuture.runAsync {
            logger.info("Attempting download for $iconUrl")
            try {
                val connection = java.net.URI(iconUrl).toURL().openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.setRequestProperty("Accept", "image/*")
                connection.instanceFollowRedirects = true
                connection.connect()

                if (connection.responseCode !in 200..299) {
                    logger.error("Failed to download icon from $iconUrl: ${connection.responseCode}")
                    iconCache[iconUrl] = EMPTY_ICON
                    connection.disconnect()
                    return@runAsync
                }

                val contentType = connection.contentType ?: ""
                val isSvg = contentType.contains("svg") || iconUrl.substringAfterLast("?").substringAfterLast(".").lowercase() == "svg" || iconUrl.endsWith(".svg")
                val isIco = contentType.contains("ico") || iconUrl.substringAfterLast("?").lowercase() == "ico" || iconUrl.endsWith(".ico")

                val stream = connection.inputStream
                val buffered: BufferedImage? = when {
                    isSvg -> renderSvg(stream, 128)
                    isIco -> readIco(stream)
                    else -> ImageIO.read(stream)
                }
                connection.disconnect()

                if (buffered == null || buffered.width == 0 || buffered.height == 0) {
                    logger.error("Failed to decode icon from $iconUrl")
                    iconCache[iconUrl] = EMPTY_ICON
                    return@runAsync
                }

                val image = bufferedToNativeImage(buffered)
                MinecraftClient.getInstance().execute {
                    val texture = registerIconTexture(iconUrl, image)
                    if (texture != null) {
                        iconCache[iconUrl] = Optional.of(texture)
                    } else {
                        iconCache[iconUrl] = EMPTY_ICON
                    }
                }
            } catch (e: Throwable) {
                logger.error("Failed to download icon from $iconUrl", e)
                iconCache[iconUrl] = EMPTY_ICON
            } finally {
                downloadingIcons.remove(iconUrl)
            }
        }
    }
    return null
}

private fun readIco(stream: java.io.InputStream): BufferedImage? {
    val bytes = stream.readBytes()
    // ICO header: 6 bytes, then directory entries of 16 bytes each
    // Each entry has offset+size of the actual image data
    // Images inside are either PNG (magic: 89 50 4E 47) or BMP
    val count = ((bytes[4].toInt() and 0xFF) or ((bytes[5].toInt() and 0xFF) shl 8))
    var bestSize = 0
    var bestOffset = 0
    var bestLength = 0
    for (i in 0 until count) {
        val base = 6 + i * 16
        val w = bytes[base].toInt() and 0xFF
        val size = if (w == 0) 256 else w
        val length = ((bytes[base+8].toLong() and 0xFF) or
                ((bytes[base+9].toLong() and 0xFF) shl 8) or
                ((bytes[base+10].toLong() and 0xFF) shl 16) or
                ((bytes[base+11].toLong() and 0xFF) shl 24)).toInt()

        val offset = ((bytes[base+12].toLong() and 0xFF) or
                ((bytes[base+13].toLong() and 0xFF) shl 8) or
                ((bytes[base+14].toLong() and 0xFF) shl 16) or
                ((bytes[base+15].toLong() and 0xFF) shl 24)).toInt()
        if (size > bestSize) { bestSize = size; bestOffset = offset; bestLength = length }
    }
    if (bestLength == 0) return null
    val imageBytes = bytes.copyOfRange(bestOffset, bestOffset + bestLength)
    return ImageIO.read(imageBytes.inputStream()) // works if frame is PNG; BMP frames need extra handling
}

private fun iconIdentifier(iconUrl: String): Identifier? {
    val hash = UUID.nameUUIDFromBytes(iconUrl.toByteArray(Charsets.UTF_8)).toString().replace("-", "")
    return Identifier.of("friendsradio", "icon_$hash")
}

private fun registerIconTexture(iconUrl: String, image: NativeImage): Identifier? {
    val id = iconIdentifier(iconUrl) ?: return null
    return try {
        MinecraftClient.getInstance().textureManager.registerTexture(id, NativeImageBackedTexture(image))
        id
    } catch (e: Exception) {
        logger.error("registerIconTexture failed for $iconUrl", e)
        null
    }
}

private fun bufferedToNativeImage(buffered: BufferedImage): NativeImage {
    val bytes = ByteArrayOutputStream().also { ImageIO.write(buffered, "png", it) }.toByteArray()
    return NativeImage.read(bytes.inputStream())
}

fun getArtworkUrl(songTitle: String): String? {
    if (songTitle.isBlank()) return null
    try {
        val query = if (songTitle.indexOf(" - ") > 0) {
            val dashIdx = songTitle.indexOf(" - ")
            "${songTitle.substring(0, dashIdx)} ${songTitle.substring(dashIdx + 3).trim()}"
        } else songTitle.trim()

        val encoded = URLEncoder.encode(query, "UTF-8")
        val apiUrl = "https://itunes.apple.com/search?term=$encoded&media=music&limit=1"

        val connection = java.net.URI(apiUrl).toURL().openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36")
        connection.connect()

        if (connection.responseCode !in 200..299) {
            logger.error("Failed to get artwork for song: $songTitle, ${connection.responseCode}")
            connection.disconnect()
            return null
        }

        val json = connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        connection.disconnect()

        val jsonObj = JsonParser.parseString(json).asJsonObject
        val results = jsonObj.getAsJsonArray("results")
        if (results.size() == 0) return null

        val track = results[0].asJsonObject
        if (track.has("artworkUrl100")) {
            return track.get("artworkUrl100").asString.replace("100x100bb", "600x600bb")
        }
    } catch (e: Exception) {
        logger.error("Failed to get artwork for song: $songTitle", e)
    }
    return null
}

private fun formatSongTitle(title: String?): String {
    if (title.isNullOrEmpty()) return title!!
    val up = title.chars().filter { codePoint: Int -> Character.isUpperCase(codePoint) }.count()
    val let = title.chars().filter { codePoint: Int -> Character.isLetter(codePoint) }.count()
    if (let > 0 && up.toDouble() / let > 0.6) {
        val words =
            title.lowercase(Locale.getDefault()).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val sb = StringBuilder()
        for (w in words) {
            if (!w.isEmpty()) sb.append(w[0].uppercaseChar()).append(w.substring(1)).append(" ")
        }
        return sb.toString().trim { it <= ' ' }
    }
    return title
}

@Throws(Exception::class)
private fun renderSvg(inputStream: InputStream, size: Int = 128): BufferedImage? {
    val svgBytes = inputStream.readAllBytes()

    val transcoder = BufferedImageTranscoder()
    transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, size.toFloat())
    transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, size.toFloat())

    val input = TranscoderInput(ByteArrayInputStream(svgBytes))
    transcoder.transcode(input, null)
    return transcoder.image
}

internal class BufferedImageTranscoder : ImageTranscoder() {
    var image: BufferedImage? = null
        private set

    override fun createImage(w: Int, h: Int): BufferedImage {
        return BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    }

    override fun writeImage(img: BufferedImage?, out: TranscoderOutput?) {
        this.image = img
    }
}