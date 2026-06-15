package nl.thatzokay.friendsradio.create

import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.behaviour.MovingInteractionBehaviour
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import nl.thatzokay.friendsradio.FriendsRadio
import nl.thatzokay.friendsradio.block.RadioBlockEntity
import nl.thatzokay.friendsradio.block.RadioBlockEvents

class RadioInteractionBehavior : MovingInteractionBehaviour() {

    override fun handlePlayerInteraction(
        player: PlayerEntity?,
        activeHand: Hand?,
        localPos: BlockPos?,
        contraptionEntity: AbstractContraptionEntity?
    ): Boolean {
        FriendsRadio.LOGGER.info("Handle interaction")
        if (player?.world?.isClient == true) {
            FriendsRadio.LOGGER.info("Handle interaction client")
            val blockEntity = contraptionEntity?.contraption?.presentBlockEntities?.get(localPos)
            FriendsRadio.LOGGER.info("Handle interaction entity $blockEntity")
            if (blockEntity == null) {
                FriendsRadio.LOGGER.error("Radio not found")
                return false
            }
            RadioBlockEvents.openUi?.let { it(blockEntity as RadioBlockEntity) }
        }
        return true
    }
}