package nl.thatzokay.friendsradio

import com.simibubi.create.AllInteractionBehaviours.interactionBehaviour
import com.simibubi.create.AllMovementBehaviours.movementBehaviour
import com.tterrag.registrate.AbstractRegistrate
import com.tterrag.registrate.Registrate
import com.tterrag.registrate.util.entry.RegistryEntry
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.AbstractBlock
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier
import nl.thatzokay.friendsradio.FriendsRadio.REGISTRATE
import nl.thatzokay.friendsradio.block.RadioBlock
import nl.thatzokay.friendsradio.create.RadioInteractionBehavior

object ModBlocks {

    val RADIO_BLOCK: RegistryEntry<RadioBlock> = REGISTRATE
        .block("radio", ::RadioBlock)
        .properties {
            it.sounds(BlockSoundGroup.COPPER)
                .strength(2.0f, 6.0f)
                .nonOpaque()
        }
        .apply {
            if (FabricLoader.getInstance().isModLoaded("create")) {
                onRegister(interactionBehaviour(RadioInteractionBehavior()))
            }
        }
        .register()

    val RADIO_ITEM: RegistryEntry<BlockItem> = REGISTRATE
        .item("radio") { settings -> BlockItem(RADIO_BLOCK.get(), settings) }
        .properties { it.maxCount(1) }
        .register()

    fun register() {
        FriendsRadio.LOGGER.info("Registering blocks")

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register { entries ->
            entries.add(RADIO_ITEM.get())
        }
    }
}