package nl.thatzokay.friendsradio.client.player

import nl.thatzokay.friendsradio.client.utils.logger
import java.io.BufferedInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.sound.sampled.*
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min


class RadioPlayer(val streamUrl: String) : Runnable {

    private var dataLine: SourceDataLine? = null
    private var isPlaying: Boolean? = null
    private var currentVolume: Float? = null

    override fun run() {
        Thread.currentThread().contextClassLoader = this.javaClass.classLoader;

        isPlaying = true
        var connection: HttpURLConnection? = null
        try {
            val url = URI.create(streamUrl).toURL()
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            connection.setInstanceFollowRedirects(true)
            connection.connect();

            try {
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    logger.warn("Server returned HTTP error code: $responseCode")
                }
            } catch (e: IOException) {
                logger.error(e.message)
            }

            try {
                val rawStream = connection.inputStream
                val bufferedStream = BufferedInputStream(rawStream)
                val audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(bufferedStream)
                val baseFormat = audioInputStream.format

                var sampleRate = baseFormat.sampleRate
                if (sampleRate.toInt() == AudioSystem.NOT_SPECIFIED || sampleRate <= 0) {
                    sampleRate = 44100F
                    logger.warn("Not specified sampleRate: $sampleRate")
                }
                var channels = baseFormat.getChannels()
                if (channels == AudioSystem.NOT_SPECIFIED || channels <= 0) {
                    channels = 2
                }

                val decodedFormat = AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate,
                    16,
                    channels,
                    channels * 2,
                    sampleRate,
                    false
                )

                val decodedInputStream = AudioSystem.getAudioInputStream(decodedFormat, audioInputStream)
                val info = DataLine.Info(SourceDataLine::class.java, decodedFormat)

                dataLine = AudioSystem.getLine(info) as SourceDataLine
                dataLine?.open(decodedFormat)
                dataLine?.start()

                updateVolumeControl()

                val buffer = ByteArray(4096)

                while (isPlaying == true) {
                    val bytesRead = decodedInputStream.read(buffer, 0, buffer.size)
                    if (bytesRead == -1) break
                    dataLine?.write(buffer, 0, bytesRead)
                }

                dataLine?.drain()
                dataLine?.stop()
                dataLine?.close()
                decodedInputStream.close()
            } catch (e: Exception) {
                logger.error(e.message)
            }
        } catch (e: Exception) {
            logger.error(e.message)
        } finally {
            isPlaying = false
            connection?.disconnect()
        }
    }

    fun setVolume(volume: Float) {
        currentVolume = volume
        updateVolumeControl()
    }

    fun stop() {
        isPlaying = false
    }

    private fun updateVolumeControl() {
        if (dataLine != null && dataLine!!.isOpen && dataLine!!.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            val volumeControl = dataLine!!.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val minimum = volumeControl.minimum
            val maximum = volumeControl.maximum
            var db = if (currentVolume == 0.0f) minimum else (log10(currentVolume!!.toDouble()) * 20.0).toFloat()
            db = max(minimum, min(db, maximum))
            volumeControl.value = db
        }
    }

    fun fetchCurrentSong(): String? {
        var connection: HttpURLConnection? = null
        try {
            val url = URI.create(streamUrl).toURL()
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            connection.setInstanceFollowRedirects(true)
            connection.connect();

            val metaInt = connection.getHeaderFieldInt("icy-metaint", 0)
            if (metaInt == 0) {
                return null
            }

            try {
                val inputStream = connection.inputStream
                var bytesToSkip = metaInt
                while (bytesToSkip > 0) {
                    val skipped: Long = inputStream.skip(bytesToSkip.toLong())
                    if (skipped <= 0) {
                        if (inputStream.read() == -1) break
                        bytesToSkip--
                    } else {
                        bytesToSkip -= skipped.toInt()
                    }
                }

                val metaLen: Int = inputStream.read() * 16

                if (metaLen <= 0) {
                    return null
                }

                val metaData = ByteArray(metaLen)
                var bytesRead = 0

                while (bytesRead < metaLen) {
                    val read: Int = inputStream.read(metaData, bytesRead, metaLen - bytesRead)
                    if (read == -1) break
                    bytesRead += read
                }

                val metaString = String(metaData, StandardCharsets.UTF_8)

                var start = metaString.indexOf("StreamTitle='")
                if (start != -1) {
                    start += 13
                    val end = metaString.indexOf("';", start)
                    if (end != -1) {
                        return metaString.substring(start, end).trim { it <= ' ' }
                    }
                }
            } catch (e: Exception) {
                logger.error(e.message)
            }
        } catch (e: Exception) {
            logger.error(e.message)
        } finally {
            connection?.disconnect()
        }
        return null
    }
}