package nl.thatzokay.friendsradio.block

import com.mojang.authlib.minecraft.client.MinecraftClient
import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.ShapeContext
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
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


class RadioBlock(settings: Settings) : BlockWithEntity(settings) {

    companion object {
        val FACING: DirectionProperty = Properties.HORIZONTAL_FACING
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getPlacementState(ctx: ItemPlacementContext?): BlockState? {
        return defaultState.with(FACING, ctx!!.horizontalPlayerFacing.opposite)
    }

    override fun onPlaced(
        world: World?,
        pos: BlockPos?,
        state: BlockState?,
        placer: LivingEntity?,
        itemStack: ItemStack?
    ) {
        super.onPlaced(world, pos, state, placer, itemStack)
        val stackNbt = itemStack?.nbt ?: return
        val be = world?.getBlockEntity(pos) as? RadioBlockEntity ?: return
        be.volume = stackNbt.getFloat("Volume").coerceIn(0.0f, 1.0f)
        be.markDirtyAndSync()
        if (world.isClient) pos?.let { RadioBlockEvents.onPlaced?.invoke(it) }
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState) =
        RadioBlockEntity(pos, state)

    @Deprecated("Deprecated in Java")
    override fun getRenderType(state: BlockState) = BlockRenderType.MODEL

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
    override fun onStateReplaced(
        state: BlockState,
        world: World,
        pos: BlockPos,
        newState: BlockState,
        moved: Boolean
    ) {
        if (!state.isOf(newState.block)) {
            val be = world.getBlockEntity(pos) as? RadioBlockEntity
            if (be != null && !world.isClient) {
                val stack = ItemStack(this.asItem())
                val nbt = stack.orCreateNbt
                nbt.putFloat("Volume", be.volume)
                dropStack(world, pos, stack)
            }
        }
        if (world.isClient) RadioBlockEvents.onRemoved?.invoke(pos)
        super.onStateReplaced(state, world, pos, newState, moved)
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