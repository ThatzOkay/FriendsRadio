package nl.thatzokay.friendsradio.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.util.InputUtil
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import nl.thatzokay.friendsradio.ModBlocks
import nl.thatzokay.friendsradio.block.RadioBlock
import nl.thatzokay.friendsradio.block.RadioBlockEntity
import nl.thatzokay.friendsradio.block.RadioBlockEvents
import nl.thatzokay.friendsradio.client.audio.RadioAudioManager
import nl.thatzokay.friendsradio.client.config.loadConfig
import nl.thatzokay.friendsradio.client.create.CreateCompat
import nl.thatzokay.friendsradio.client.ui.RadioScreen
import nl.thatzokay.friendsradio.client.utils.downloadingIcons
import nl.thatzokay.friendsradio.client.utils.findRadioStack
import nl.thatzokay.friendsradio.client.utils.iconCache
import nl.thatzokay.friendsradio.client.utils.logger
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

		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RADIO_BLOCK, RenderLayer.getCutout())
		UseBlockCallback.EVENT.register { playerEntity, world, _, hitResult ->
			if (world.isClient && hitResult.type == HitResult.Type.BLOCK) {
				val blockHitResult = hitResult as BlockHitResult
				val state = world.getBlockState(blockHitResult.blockPos)

				val blockEntity = if (state.block is RadioBlock) {
					world.getBlockEntity(blockHitResult.blockPos) as? RadioBlockEntity
				} else null

				// Not a radio in the world, check if player is riding a contraption
				val finalEntity = blockEntity ?: run {
					val vehicle = playerEntity.vehicle ?: return@run null
					CreateCompat.findRadioInContraption(vehicle, blockHitResult.blockPos)
				}

				if (finalEntity != null) {
					MinecraftClient.getInstance().setScreen(RadioScreen(finalEntity, null))
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
    }

	private fun onClientTick(client: MinecraftClient) {
		if (!openScreenKey!!.wasPressed()) return
		if (client.player == null) return

        val heldRadio = findRadioStack(client.player!!) ?: return
		val (stack, hand) = heldRadio
        if (stack.isOf(ModBlocks.RADIO_ITEM)) {
			client.setScreen(RadioScreen(null, stack))
		}
	}
}