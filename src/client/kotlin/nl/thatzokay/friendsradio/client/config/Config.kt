package nl.thatzokay.friendsradio.client.config

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.client.MinecraftClient
import nl.thatzokay.friendsradio.records.Station
import nl.thatzokay.friendsradio.client.utils.logger
import java.nio.file.Files
import java.nio.file.Path

val favorites = mutableListOf<Station>()
val customStations = mutableListOf<Station>()
val blacklist = mutableListOf<String>()

var globalVolume = 0.5f

private val defaultBlacklist = listOf("advertisement", "live broadcast", "ad")

private val configPath: Path
    get() = Path.of(MinecraftClient.getInstance().runDirectory.absolutePath, "config", "friendsradio.json")

private fun JsonObject.getString(key: String, default: String = ""): String =
    if (has(key)) get(key).asString else default

private fun Station.toJsonObject() = JsonObject().apply {
    addProperty("name", name)
    addProperty("url", url)
    addProperty("favicon", favicon)
}

private fun JsonObject.parseStation(): Station? {
    val url = getString("url")
    return if (url.isNotEmpty()) Station(getString("name", "Unknown"), url, getString("favicon")) else null
}

fun loadConfig() {
    try {
        if (!Files.exists(configPath)) return

        val json = JsonParser.parseString(Files.readString(configPath)).asJsonObject

        if (json.has("volume")) globalVolume = json.get("volume").asFloat

        favorites.clear()
        json.getAsJsonArray("favorites")?.forEach { element ->
            element.asJsonObject.parseStation()?.let { favorites.add(it) }
        }

        customStations.clear()
        json.getAsJsonArray("customStations")?.forEach { element ->
            element.asJsonObject.parseStation()?.let { customStations.add(it) }
        }

        blacklist.clear()
        val savedBlacklist = json.getAsJsonArray("blacklist")
        if (savedBlacklist != null) {
            savedBlacklist.forEach { blacklist.add(it.asString) }
        } else {
            blacklist.addAll(defaultBlacklist)
        }
    } catch (e: Exception) {
        logger.error("Failed to load config", e)
    }
}

fun saveConfig() {
    try {
        Files.createDirectories(configPath.parent)

        val json = JsonObject().apply {
            addProperty("volume", globalVolume)
            add("favorites", JsonArray().also { array -> favorites.forEach { array.add(it.toJsonObject()) } })
            add("customStations", JsonArray().also { array -> customStations.forEach { array.add(it.toJsonObject()) } })
            add("blacklist", JsonArray().also { array -> blacklist.forEach { array.add(it) } })
        }

        Files.writeString(configPath, json.toString())
    } catch (e: Exception) {
        logger.error("Failed to save config", e)
    }
}

fun removeCustomStation(url: String?) {
    customStations.removeIf { cs: Station -> cs.url == url }
    saveConfig()
}