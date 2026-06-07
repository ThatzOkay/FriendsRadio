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
import net.minecraft.util.hit.HitResult
import nl.thatzokay.friendsradio.ModBlocks
import nl.thatzokay.friendsradio.block.RadioBlock
import nl.thatzokay.friendsradio.block.RadioBlockEntity
import nl.thatzokay.friendsradio.block.RadioBlockEvents
import nl.thatzokay.friendsradio.client.audio.RadioAudioManager
import nl.thatzokay.friendsradio.client.config.loadConfig
import nl.thatzokay.friendsradio.client.ui.RadioScreen
import nl.thatzokay.friendsradio.client.utils.findPlayingRadioStack
import nl.thatzokay.friendsradio.client.utils.findRadioStack
import org.lwjgl.glfw.GLFW


object FriendsRadioClient : ClientModInitializer {

	var openScreenKey: KeyBinding? = null

	override fun onInitializeClient() {
		loadConfig()

		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RADIO_BLOCK, RenderLayer.getCutout())
		UseBlockCallback.EVENT.register { playerEntity, world, hand, hitResult ->
			if (world.isClient && hitResult.type == HitResult.Type.BLOCK) {
				val block = world.getBlockState(hitResult.blockPos).block
				val blockEntity = world.getBlockEntity(hitResult.blockPos) as RadioBlockEntity?
				if (block is RadioBlock) {
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

		RadioBlockEvents.onPlaced  = { pos -> RadioAudioManager.onRadioLoaded(pos) }
		RadioBlockEvents.onRemoved = { pos -> RadioAudioManager.onRadioUnloaded(pos) }

		ClientTickEvents.END_CLIENT_TICK.register { client ->
            onClientTick(client)
        }

		ClientChunkEvents.CHUNK_LOAD.register { world, chunk ->
			chunk.blockEntities.keys
				.filter { pos -> world.getBlockEntity(pos) is RadioBlockEntity }
				.forEach { pos -> RadioAudioManager.onRadioLoaded(pos) }
		}

		ClientChunkEvents.CHUNK_UNLOAD.register { world, chunk ->
			chunk.blockEntities.keys
				.forEach { pos -> RadioAudioManager.onRadioUnloaded(pos) }
		}

		ClientTickEvents.END_CLIENT_TICK.register { client ->
			val player = client.player ?: return@register
			RadioAudioManager.tick(player)
		}

		ClientPlayConnectionEvents.DISCONNECT.register { handler, client ->
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