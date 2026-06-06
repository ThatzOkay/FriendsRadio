package nl.thatzokay.friendsradio

import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object FriendsRadio : ModInitializer {
	const val MOD_ID = "friendsradio"
	val LOGGER: Logger? = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		ModBlocks.register()
	}
}