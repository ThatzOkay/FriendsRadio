package nl.thatzokay.friendsradio.client

import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.util.InputUtil
import net.minecraft.util.ActionResult
import net.minecraft.util.TypeFilter
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import nl.thatzokay.friendsradio.ModBlocks
import nl.thatzokay.friendsradio.block.RadioBlock
import nl.thatzokay.friendsradio.block.RadioBlockEntity
import nl.thatzokay.friendsradio.block.RadioBlockEvents
import nl.thatzokay.friendsradio.client.audio.RadioAudioManager
import nl.thatzokay.friendsradio.client.audio.RadioAudioManager.knownContraptionRadios
import nl.thatzokay.friendsradio.client.config.loadConfig
import nl.thatzokay.friendsradio.client.ui.RadioScreen
import nl.thatzokay.friendsradio.client.utils.downloadingIcons
import nl.thatzokay.friendsradio.client.utils.findRadioStack
import nl.thatzokay.friendsradio.client.utils.iconCache
import nl.thatzokay.friendsradio.client.utils.logger
import nl.thatzokay.friendsradio.network.RadioContraptionUpdatePayload
import nl.thatzokay.friendsradio.records.Station
import org.lwjgl.glfw.GLFW


object FriendsRadioClient : ClientModInitializer {

	var openScreenKey: KeyBinding? = null

	override fun onInitializeClient() {
		logger.info("[FriendsRadio] Client initializing")

		iconCache.clear()
		downloadingIcons.clear()

		loadConfig()

		RadioBlockEvents.onPlaced  = { pos -> RadioAudioManager.onRadioLoaded(pos) }
		RadioBlockEvents.onRemoved = { pos -> RadioAudioManager.onRadioUnloaded(pos) }
		RadioBlockEvents.openUi = { entity -> MinecraftClient.getInstance().setScreen(RadioScreen(entity, null)) }

		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RADIO_BLOCK.get(), RenderLayer.getCutout())
		UseBlockCallback.EVENT.register { _, world, _, hitResult ->
			if (world.isClient && hitResult.type == HitResult.Type.BLOCK) {
				val blockHitResult = hitResult as BlockHitResult
				val state = world.getBlockState(blockHitResult.blockPos)

				val blockEntity = if (state.block is RadioBlock) {
					world.getBlockEntity(blockHitResult.blockPos) as? RadioBlockEntity
				} else null

				if (blockEntity != null) {
					MinecraftClient.getInstance().setScreen(RadioScreen(blockEntity, null))
					return@register ActionResult.SUCCESS
				}
			}
			ActionResult.PASS
		}

		openScreenKey = KeyBindingHelper.registerKeyBinding(KeyBinding(
			"key.friendsradio.open_radio",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_R,
			"category.friendsradio.keys"
		))

		ClientTickEvents.END_CLIENT_TICK.register { client ->
            onClientTick(client)
        }

		ClientChunkEvents.CHUNK_LOAD.register { world, chunk ->
			chunk.blockEntities.keys
				.filter { pos -> world.getBlockEntity(pos) is RadioBlockEntity }
				.forEach { pos -> RadioAudioManager.onRadioLoaded(pos) }
		}

		ClientChunkEvents.CHUNK_UNLOAD.register { _, chunk ->
			chunk.blockEntities.keys
				.forEach { pos -> RadioAudioManager.onRadioUnloaded(pos) }
		}

		ClientTickEvents.END_CLIENT_TICK.register { client ->
			val player = client.player ?: return@register
			RadioAudioManager.tick(player)
		}

		ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
			RadioAudioManager.stopAll()
		}

		ClientTickEvents.END_CLIENT_TICK.register { client ->
			if (!FabricLoader.getInstance().isModLoaded("create")) return@register
			val player = client.player ?: return@register
			val world = client.world ?: return@register
			if (world.time % 20 != 0L) return@register

			val searchBox = Box(player.pos, player.pos).expand(64.0)
			val foundRadios = mutableSetOf<RadioAudioManager.ContraptionRadioInfo>()

			world.getEntitiesByType(TypeFilter.instanceOf(AbstractContraptionEntity::class.java), searchBox) { true }
				.forEach { contraption ->
					contraption.contraption.presentBlockEntities
						.filter { (_, be) -> be is RadioBlockEntity }
						.forEach { (pos, be) ->
							val info = RadioAudioManager.ContraptionRadioInfo(
								contraption.id,
								pos,
								0
							)
							foundRadios.add(info)
							if (!knownContraptionRadios.contains(info)) {
								RadioAudioManager.onContraptionRadioLoaded(contraption.id, be as RadioBlockEntity, pos)
							}
						}
				}

			knownContraptionRadios.filter { it !in foundRadios }.forEach { (id, pos, ) ->
				RadioAudioManager.onContraptionRadioUnloaded(id, pos)
			}

			knownContraptionRadios.clear()
			knownContraptionRadios.addAll(foundRadios)
		}

//		ClientPlayNetworking.registerGlobalReceiver(RadioContraptionUpdatePayload.ID) { client, _, buf, _ ->
//			val data = RadioContraptionUpdatePayload.decode(buf)
//			client.execute {
//				val world = client.world ?: return@execute
//				val entity = world.getEntityById(data.entityId) ?: return@execute
//				val be = CreateCompat.findRadio(entity, data.pos) ?: return@execute
//
//				be.station = Station(data.stationName, data.stationUrl, data.stationFavicon)
//				be.isPlaying = data.isPlaying
//			}
//		}
    }

	private fun onClientTick(client: MinecraftClient) {
		if (!openScreenKey!!.wasPressed()) return
		if (client.player == null) return

        val heldRadio = findRadioStack(client.player!!) ?: return
		val (stack, hand) = heldRadio
        if (stack.isOf(ModBlocks.RADIO_ITEM.get())) {
			client.setScreen(RadioScreen(null, stack))
		}
	}
}