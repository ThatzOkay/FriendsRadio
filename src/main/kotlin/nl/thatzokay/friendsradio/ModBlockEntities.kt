package nl.thatzokay.friendsradio

import net.minecraft.block.entity.BlockEntityType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import nl.thatzokay.friendsradio.block.RadioBlockEntity

object ModBlockEntities {
    val RADIO_BLOCK_ENTITY: BlockEntityType<RadioBlockEntity> by lazy {
        BlockEntityType.Builder
            .create(::RadioBlockEntity, ModBlocks.RADIO_BLOCK)
            .build(null)
    }

    fun register() {
        Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier("friendsradio", "radio"),
            RADIO_BLOCK_ENTITY
        )
    }
}