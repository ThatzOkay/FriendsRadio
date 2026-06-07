package nl.thatzokay.friendsradio.block

import net.minecraft.util.math.BlockPos

object RadioBlockEvents {
    var onPlaced:  ((pos: BlockPos) -> Unit)? = null
    var onRemoved: ((pos: BlockPos) -> Unit)? = null
}