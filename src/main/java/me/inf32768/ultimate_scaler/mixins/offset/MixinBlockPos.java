package me.inf32768.ultimate_scaler.mixins.offset;

import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

@Mixin(BlockPos.class)
public abstract class MixinBlockPos {

    /**
     * @reason 防止坐标打包溢出，将 X/Z 扩展为 32 位
     * @author INF32768
     */
    @Overwrite
    public static long asLong(int x, int y, int z) {
        if (!config.expandWorldBorder) {
            // 回退到原版逻辑（使用反射或直接实现原版算法）
            return vanillaAsLong(x, y, z);
        }
        // 新打包方案：X 用 32 位，Y 用 12 位（原版），Z 用 32 位
        // 注意：Y 只有 12 位，但 Y 坐标范围 -2048~2047 足够原版使用
        long packed = 0L;
        packed |= ((long) x & 0xFFFFFFFFL) << 44; // X 在最高位
        packed |= ((long) y & 0xFFFL) << 32;      // Y 在中间，12 位
        packed |= ((long) z & 0xFFFFFFFFL);       // Z 在最低位
        return packed;
    }

    // 原版算法（为了回退）
    private static long vanillaAsLong(int x, int y, int z) {
        long l = 0L;
        l |= ((long) x & BlockPos.BITS_X) << BlockPos.BIT_SHIFT_X;
        l |= ((long) y & BlockPos.BITS_Y) << 0;
        l |= ((long) z & BlockPos.BITS_Z) << BlockPos.BIT_SHIFT_Z;
        return l;
    }
}
