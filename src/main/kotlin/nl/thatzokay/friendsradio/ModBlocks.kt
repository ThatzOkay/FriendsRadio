package nl.thatzokay.friendsradio

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.block.AbstractBlock
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier
import nl.thatzokay.friendsradio.block.RadioBlock

object ModBlocks {

    val RADIO_BLOCK: RadioBlock? = Registry.register(
        Registries.BLOCK,
        Identifier("friendsradio", "radio"),
        RadioBlock(
            AbstractBlock.Settings.create()
                .sounds(BlockSoundGroup.COPPER)
                .strength(2.0f, 6.0f)
                .nonOpaque()
        )
    )

    val RADIO_ITEM: BlockItem? = Registry.register(
        Registries.ITEM,
        Identifier("friendsradio", "radio"),
        BlockItem(RADIO_BLOCK, Item.Settings().maxCount(1))
    )

    fun register() {
        FriendsRadio.LOGGER?.info("Registering blocks")

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register { entries ->
            entries.add(RADIO_ITEM)
        }
    }
}