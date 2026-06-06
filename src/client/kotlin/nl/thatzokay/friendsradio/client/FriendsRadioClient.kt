package nl.thatzokay.friendsradio.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.ActionResult
import nl.thatzokay.friendsradio.FriendsRadio
import nl.thatzokay.friendsradio.ModBlocks
import nl.thatzokay.friendsradio.block.RadioBlock
import nl.thatzokay.friendsradio.client.config.loadConfig
import nl.thatzokay.friendsradio.client.ui.RadioScreen
import java.util.logging.Logger

object FriendsRadioClient : ClientModInitializer {
	override fun onInitializeClient() {
		loadConfig()

		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RADIO, RenderLayer.getCutout())
		UseBlockCallback.EVENT.register { playerEntity, world, hand, hitResult ->
			if (world.isClient && hitResult.type == net.minecraft.util.hit.HitResult.Type.BLOCK) {
				val block = world.getBlockState(hitResult.blockPos).block
				if (block is RadioBlock) {
					MinecraftClient.getInstance().setScreen(RadioScreen(this))
					return@register ActionResult.SUCCESS
				}
			}
			ActionResult.PASS
		}
	}
}