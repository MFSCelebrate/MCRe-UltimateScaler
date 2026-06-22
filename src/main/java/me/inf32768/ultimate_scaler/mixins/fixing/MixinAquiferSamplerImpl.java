package me.inf32768.ultimate_scaler.mixins.fixing;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 修复 AquiferSampler.Impl 内部所有数组索引溢出问题。
 * 同时覆盖 index 和 method_33738（getFluidBlockY）两个方法。
 */
@Mixin(targets = "net.minecraft.world.gen.chunk.AquiferSampler$Impl")
public abstract class MixinAquiferSamplerImpl {

    @Shadow
    private int startX;

    @Shadow
    private int startY;

    @Shadow
    private int startZ;

    @Shadow
    private int sizeX;

    @Shadow
    private int sizeZ;

    /**
     * 覆盖原版 index 方法，钳制坐标后计算索引。
     */
    @Overwrite
    private int index(int x, int y, int z) {
        if (!config.fixChunkSectionSubSetOverflow) {
            int i = x - this.startX;
            int j = y - this.startY;
            int k = z - this.startZ;
            return (j * this.sizeZ + k) * this.sizeX + i;
        }

        final int MAX_SAFE_COORD = 33554400;
        final int MIN_SAFE_COORD = -33554400;
        int clampedX = Math.max(MIN_SAFE_COORD, Math.min(MAX_SAFE_COORD, x));
        int clampedY = Math.max(-2048, Math.min(2047, y));
        int clampedZ = Math.max(MIN_SAFE_COORD, Math.min(MAX_SAFE_COORD, z));

        int i = clampedX - this.startX;
        int j = clampedY - this.startY;
        int k = clampedZ - this.startZ;
        return (j * this.sizeZ + k) * this.sizeX + i;
    }

    /**
     * 覆盖原版 method_33738（getFluidBlockY）。
     * 该方法内部使用 BlockPos.asLong 取模计算数组索引，
     * 我们在这里钳制坐标后重新计算。
     */
    @Overwrite
    private int method_33738(int x, int y, int z) {
        if (!config.fixChunkSectionSubSetOverflow) {
            // 回退到原版逻辑：直接使用 BlockPos.asLong 取模
            long packed = BlockPos.asLong(x, y, z);
            return (int) Math.floorMod(packed, 315);
        }

        // 钳制坐标
        final int MAX_SAFE_COORD = 33554400;
        final int MIN_SAFE_COORD = -33554400;
        int clampedX = Math.max(MIN_SAFE_COORD, Math.min(MAX_SAFE_COORD, x));
        int clampedY = Math.max(-2048, Math.min(2047, y));
        int clampedZ = Math.max(MIN_SAFE_COORD, Math.min(MAX_SAFE_COORD, z));

        // 使用钳制后的坐标重新打包并取模
        long packed = BlockPos.asLong(clampedX, clampedY, clampedZ);
        return (int) Math.floorMod(packed, 315);
    }
}
