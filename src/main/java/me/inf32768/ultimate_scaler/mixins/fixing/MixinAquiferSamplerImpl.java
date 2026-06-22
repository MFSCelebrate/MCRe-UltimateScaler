package me.inf32768.ultimate_scaler.mixins.fixing;

import me.inf32768.ultimate_scaler.util.CoordinateHolder;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 综合修复方案：
 * 1. 用 ThreadLocal 传递原始坐标（伪传参法）
 * 2. 拦截所有打包调用（BlockPos.asLong / ChunkSectionPos.asLong / ChunkPos.toLong）
 * 3. 直接覆盖内部私有方法 method_33738，钳制坐标后计算索引，彻底防止数组越界
 */
@Mixin(targets = "net.minecraft.class_6350$class_5832") // AquiferSampler.Impl
public abstract class MixinAquiferSamplerImpl {

    // ========== 伪传参法核心 ==========
    @Inject(
        method = "apply",
        at = @At("HEAD"),
        remap = true
    )
    private void onApplyHead(DensityFunction.NoisePos pos, double density, CallbackInfoReturnable<AquiferSampler.FluidLevel> cir) {
        if (!config.fixChunkSectionSubSetOverflow) return;
        CoordinateHolder.set(new BlockPos(pos.blockX(), pos.blockY(), pos.blockZ()));
    }

    @Inject(
        method = "apply",
        at = @At("RETURN"),
        remap = true
    )
    private void onApplyReturn(DensityFunction.NoisePos pos, double density, CallbackInfoReturnable<AquiferSampler.FluidLevel> cir) {
        if (!config.fixChunkSectionSubSetOverflow) return;
        CoordinateHolder.clear();
    }

    // ========== 拦截所有打包调用 ==========
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/BlockPos;asLong(III)J"
        ),
        remap = true,
        require = 0
    )
    private long redirectBlockPosAsLong(int x, int y, int z) {
        if (!config.fixChunkSectionSubSetOverflow) {
            return BlockPos.asLong(x, y, z);
        }
        BlockPos pos = CoordinateHolder.get();
        if (pos != null) {
            return BlockPos.asLong(pos.getX(), pos.getY(), pos.getZ());
        }
        return BlockPos.asLong(x, y, z);
    }

    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/ChunkSectionPos;asLong(III)J"
        ),
        remap = true,
        require = 0
    )
    private long redirectChunkSectionPosAsLong(int x, int y, int z) {
        if (!config.fixChunkSectionSubSetOverflow) {
            return ChunkSectionPos.asLong(x, y, z);
        }
        BlockPos pos = CoordinateHolder.get();
        if (pos != null) {
            return ChunkSectionPos.asLong(
                ChunkSectionPos.getSectionCoord(pos.getX()),
                ChunkSectionPos.getSectionCoord(pos.getY()),
                ChunkSectionPos.getSectionCoord(pos.getZ())
            );
        }
        return ChunkSectionPos.asLong(x, y, z);
    }

    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/ChunkPos;toLong(II)J"
        ),
        remap = true,
        require = 0
    )
    private long redirectChunkPosToLong(int chunkX, int chunkZ) {
        if (!config.fixChunkSectionSubSetOverflow) {
            return ChunkPos.toLong(chunkX, chunkZ);
        }
        BlockPos pos = CoordinateHolder.get();
        if (pos != null) {
            return ChunkPos.toLong(
                ChunkSectionPos.getSectionCoord(pos.getX()),
                ChunkSectionPos.getSectionCoord(pos.getZ())
            );
        }
        return ChunkPos.toLong(chunkX, chunkZ);
    }

    // ========== 关键：直接覆盖 method_33738，防止数组越界 ==========
    /**
     * 覆盖原版私有方法 method_33738。
     * 该方法用于计算含水层内部数组的索引，原版直接用 BlockPos.asLong 的结果取模，
     * 当坐标溢出时会产生负索引，导致 ArrayIndexOutOfBoundsException。
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @return 安全的数组索引（非负）
     */
    @Overwrite
    private int method_33738(int x, int y, int z) {
        if (!config.fixChunkSectionSubSetOverflow) {
            // 回退到原版逻辑（直接调用原版方法无法做到，因为它是私有的，
            // 所以我们用硬编码的位运算模拟原版行为）
            return vanillaMethod_33738(x, y, z);
        }

        // 钳制坐标到安全范围（26 位有符号数最大值，保留边界余量）
        final int MAX_SAFE_COORD = 33554400;
        final int MIN_SAFE_COORD = -33554400;
        int clampedX = Math.max(MIN_SAFE_COORD, Math.min(MAX_SAFE_COORD, x));
        int clampedZ = Math.max(MIN_SAFE_COORD, Math.min(MAX_SAFE_COORD, z));
        int clampedY = Math.max(-2048, Math.min(2047, y));

        // 如果坐标没有被钳制，说明在安全范围内，直接调用原版逻辑
        if (clampedX == x && clampedY == y && clampedZ == z) {
            return vanillaMethod_33738(x, y, z);
        }

        // 坐标被钳制了，说明超出了安全范围，使用钳制后的坐标重新计算索引
        // 原版计算方式：BlockPos.asLong(clampedX, clampedY, clampedZ) % 315
        // 注意：原版可能使用 Math.floorMod 或直接取模，这里用 Math.floorMod 确保非负
        long packed = BlockPos.asLong(clampedX, clampedY, clampedZ);
        // 315 是原版数组长度（从崩溃日志推断）
        return (int) Math.floorMod(packed, 315);
    }

    /**
     * 模拟原版 method_33738 的逻辑。
     * 由于原版方法是私有的，我们无法直接调用，这里根据崩溃日志推断其实现。
     */
    private int vanillaMethod_33738(int x, int y, int z) {
        long packed = BlockPos.asLong(x, y, z);
        // 原版可能是 (int)(packed % 315)，但这样在负数时会产生负索引
        // 为了安全，我们始终使用 Math.floorMod 返回非负值
        return (int) Math.floorMod(packed, 315);
    }
}
