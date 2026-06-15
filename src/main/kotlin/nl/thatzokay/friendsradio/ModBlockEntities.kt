package nl.thatzokay.friendsradio

import com.tterrag.registrate.util.entry.RegistryEntry
import net.minecraft.block.entity.BlockEntityType
import nl.thatzokay.friendsradio.FriendsRadio.REGISTRATE
import nl.thatzokay.friendsradio.block.RadioBlockEntity

object ModBlockEntities {

    val RADIO_BLOCK_ENTITY: RegistryEntry<BlockEntityType<RadioBlockEntity>> = REGISTRATE
        .blockEntity<RadioBlockEntity>("radio") { type, pos, state -> RadioBlockEntity(pos, state) }
        .validBlock(ModBlocks.RADIO_BLOCK)
        .register()

    fun register() {
        FriendsRadio.LOGGER.info("Registering block entities")
    }
}