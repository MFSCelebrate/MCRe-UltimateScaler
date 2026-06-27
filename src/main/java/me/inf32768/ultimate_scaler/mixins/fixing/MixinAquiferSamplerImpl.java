package me.inf32768.ultimate_scaler.mixins.fixing;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 修复 AquiferSampler.Impl 内部数组索引溢出问题。
 * 已移除对 BlockPos.asLong 的 @Redirect，因为 MixinBlockPos 已全局钳制。
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
     * 即使 MixinBlockPos 钳制了 asLong，但 index 直接使用原始坐标计算，仍可能越界，所以保留此覆盖。
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
}
