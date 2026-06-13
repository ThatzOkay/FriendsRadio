package nl.thatzokay.friendsradio.client.create

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.entity.Entity
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import nl.thatzokay.friendsradio.block.RadioBlockEntity
import nl.thatzokay.friendsradio.client.utils.logger

object CreateCompat {
    val isLoaded = FabricLoader.getInstance().isModLoaded("create")

    fun findRadioInContraption(vehicle: Entity, targetPos: BlockPos): RadioBlockEntity? {
        logger.info("findRadioInContraption called: vehicle={}, targetPos={}", vehicle.type, targetPos)

        if (!isLoaded) {
            logger.error("Create not loaded")
            return null
        }

        try {
            logger.info("Attempting to access contraption from vehicle")

            val getContraption = vehicle.javaClass.getMethod("getContraption")
            val contraption = getContraption.invoke(vehicle)

            if (contraption == null) {
                logger.error("No contraption found on vehicle")
                return null
            }

            logger.info("Contraption found: ${contraption.javaClass.name}")

            val toLocalVector = vehicle.javaClass.getMethod(
                "toLocalVector",
                Vec3d::class.java,
                Float::class.javaPrimitiveType
            )

            val worldVec = Vec3d.of(targetPos).add(0.5, 0.5, 0.5)
            logger.info("World vector: $worldVec")

            val localVec = toLocalVector.invoke(vehicle, worldVec, 1.0f) as Vec3d
            val localPos = BlockPos.ofFloored(localVec)

            logger.info("Converted to local pos: $localPos")

            logger.info("Fields in contraption:")
            contraption.javaClass.declaredFields.forEach {
                logger.info(it.name)
            }

            val presentTEs = contraption.javaClass.getField("presentTileEntities")
            @Suppress("UNCHECKED_CAST")
            val teMap = presentTEs.get(contraption) as? Map<BlockPos, NbtCompound>

            if (teMap == null) {
                logger.error("presentTileEntities is null or invalid type")
                return null
            }

            logger.info("TileEntities in contraption: ${teMap.size}")

            val nbt = teMap[localPos]
            if (nbt == null) {
                logger.info("No NBT found at localPos={}", localPos)
                return null
            }

            logger.debug("NBT found at position, creating fake RadioBlockEntity")

            val fakeEntity = RadioBlockEntity(targetPos, vehicle.world.getBlockState(targetPos))
            fakeEntity.readNbt(nbt)

            logger.info("Successfully reconstructed RadioBlockEntity at {}", targetPos)

            return fakeEntity

        } catch (e: Exception) {
            logger.warn("Failed to find radio in contraption (vehicle=${vehicle.type}, targetPos=$targetPos)", e)
        }

        return null
    }
}