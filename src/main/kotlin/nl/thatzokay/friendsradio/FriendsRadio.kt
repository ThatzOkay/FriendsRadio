package nl.thatzokay.friendsradio

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.util.math.Vec3d
import nl.thatzokay.friendsradio.block.RadioBlockEntity
import nl.thatzokay.friendsradio.network.RadioItemUpdatePayload
import nl.thatzokay.friendsradio.network.RadioUpdatePayload
import nl.thatzokay.friendsradio.records.Station
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object FriendsRadio : ModInitializer {
	const val MOD_ID = "friendsradio"
	val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		LOGGER.info("Initializing FriendRadio")
		ModBlocks.register()
		ModBlockEntities.register()

		ServerPlayNetworking.registerGlobalReceiver(RadioUpdatePayload.ID) { server, player, handler, buf, responseSender ->
			val data = RadioUpdatePayload.decode(buf)
			server.execute {
				val be = player.world.getBlockEntity(data.pos) as? RadioBlockEntity ?: return@execute
				if (player.squaredDistanceTo(Vec3d.ofCenter(data.pos)) > 64.0 * 64.0) return@execute

				val station = Station(data.stationName, data.stationUrl, data.stationFavicon)

				be.station = station
				be.isPlaying   = data.isPlaying
				be.markDirtyAndSync()
			}
		}

		ServerPlayNetworking.registerGlobalReceiver(RadioItemUpdatePayload.ID) { server, player, handler, buf, responseSender ->
			val data = RadioItemUpdatePayload.decode(buf)
			server.execute {
				val itemStack = player.offHandStack

				itemStack.nbt?.putString("StationName", data.stationName)
				itemStack.nbt?.putString("StationUrl", data.stationUrl)
				itemStack.nbt?.putString("StationFavicon", data.stationFavicon)


				player.inventory.markDirty()
			}
		}
	}
}