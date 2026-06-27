package me.inf32768.ultimate_scaler.mixins.fixing;

import net.minecraft.world.gen.chunk.AquiferSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 修复 AquiferSampler.Impl 内部数组索引溢出问题。
 * 通过钳制所有坐标差值到有效范围，确保索引永不越界。
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

    @Shadow
    private AquiferSampler.FluidLevel[] waterLevels; // 需要 shadow 数组以计算 sizeY

    /**
     * 覆盖原版 index 方法。
     * 当修复开关开启时，使用安全索引计算：
     * - 计算 i, j, k = 坐标 - 起始偏移
     * - 通过数组长度反推 sizeY = waterLevels.length / (sizeX * sizeZ)
     * - 将 i, j, k 钳制到 [0, sizeX-1], [0, sizeY-1], [0, sizeZ-1]
     * - 返回合法索引
     */
    @Overwrite
    private int index(int x, int y, int z) {
        if (!config.fixChunkSectionSubSetOverflow) {
            int i = x - this.startX;
            int j = y - this.startY;
            int k = z - this.startZ;
            return (j * this.sizeZ + k) * this.sizeX + i;
        }

        // 安全模式：钳制所有差值
        int i = x - this.startX;
        int j = y - this.startY;
        int k = z - this.startZ;

        // 计算 Y 方向尺寸
        int sizeY = this.waterLevels.length / (this.sizeX * this.sizeZ);

        // 钳制到 [0, size-1]
        i = Math.max(0, Math.min(this.sizeX - 1, i));
        j = Math.max(0, Math.min(sizeY - 1, j));
        k = Math.max(0, Math.min(this.sizeZ - 1, k));

        return (j * this.sizeZ + k) * this.sizeX + i;
    }
}
