package nl.thatzokay.friendsradio.block

import com.mojang.authlib.minecraft.client.MinecraftClient
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.DirectionProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World


class RadioBlock(settings: Settings) : Block(settings) {

    companion object {
        val FACING: DirectionProperty = Properties.HORIZONTAL_FACING
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getPlacementState(ctx: ItemPlacementContext?): BlockState? {
        return defaultState.with(FACING, ctx!!.horizontalPlayerFacing.opposite)
    }

    @Deprecated("Deprecated in Java")
    override fun getOutlineShape(
        state: BlockState,
        world: BlockView,
        pos: BlockPos,
        context: ShapeContext
    ): VoxelShape {
        return when (state.get(FACING)) {
            Direction.NORTH -> VoxelShapes.cuboid(2/16.0, 0.0, 5/16.0, 14/16.0, 8/16.0, 11/16.0)
            Direction.SOUTH -> VoxelShapes.cuboid(2/16.0, 0.0, 5/16.0, 14/16.0, 8/16.0, 11/16.0)
            Direction.WEST  -> VoxelShapes.cuboid(5/16.0, 0.0, 2/16.0, 11/16.0, 8/16.0, 14/16.0)
            Direction.EAST  -> VoxelShapes.cuboid(5/16.0, 0.0, 2/16.0, 11/16.0, 8/16.0, 14/16.0)
            else            -> VoxelShapes.fullCube()
        }
    }

    override fun isTransparent(state: BlockState?, world: BlockView?, pos: BlockPos?): Boolean {
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        if (world.isClient) {
           player.openHandledScreen(state.createScreenHandlerFactory(world, pos))
        }
        return ActionResult.SUCCESS
    }
}