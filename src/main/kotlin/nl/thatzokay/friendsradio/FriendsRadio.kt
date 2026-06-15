package nl.thatzokay.friendsradio

import com.tterrag.registrate.AbstractRegistrate
import com.tterrag.registrate.Registrate
import io.netty.buffer.Unpooled
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.math.Vec3d
import nl.thatzokay.friendsradio.block.RadioBlockEntity
import nl.thatzokay.friendsradio.network.RadioContraptionUpdatePayload
import nl.thatzokay.friendsradio.network.RadioItemUpdatePayload
import nl.thatzokay.friendsradio.network.RadioUpdatePayload
import nl.thatzokay.friendsradio.records.Station
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object FriendsRadio : ModInitializer {

	val REGISTRATE: AbstractRegistrate<*> = Registrate.create("friendsradio")

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

//		ServerPlayNetworking.registerGlobalReceiver(RadioContraptionUpdatePayload.ID) { server, player, handler, buf, responseSender ->
//			val data = RadioContraptionUpdatePayload.decode(buf)
//			server.execute {
//				val entity = player.world.getEntityById(data.entityId) ?: return@execute
//				val be = CreateCompat.findRadio(entity, data.pos) ?: return@execute
//
//				val station = Station(data.stationName, data.stationUrl, data.stationFavicon)
//				be.station = station
//				be.isPlaying = data.isPlaying
//
//				for (tracking in PlayerLookup.tracking(entity)) {
//					val outBuf = PacketByteBuf(Unpooled.buffer())
//					RadioContraptionUpdatePayload.encode(outBuf, data.entityId, data.pos, station, data.isPlaying)
//					ServerPlayNetworking.send(tracking, RadioContraptionUpdatePayload.ID, outBuf)
//				}
//			}
//		}

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

		REGISTRATE.register()
	}
}