package me.inf32768.ultimate_scaler.mixins.fixing;

import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 修复 ChunkSectionPos.asLong 打包溢出问题。
 * 当坐标超出 ±33554432 时，将坐标钳制到安全范围内（±33554400），
 * 防止截断导致 LongAVLTreeSet.subSet 参数顺序错误，从而防止崩溃。
 * 同时保留含水层等所有地形特征（只是被截断在边界处）。
 */
@Mixin(ChunkSectionPos.class)
public abstract class MixinChunkSectionPos {

    // 原版位布局常量（1.21.8 固定）
    private static final int SIZE_BITS_XZ = 26;
    private static final int SIZE_BITS_Y = 64 - 2 * SIZE_BITS_XZ; // = 12
    private static final long BITS_X = (1L << SIZE_BITS_XZ) - 1L;
    private static final long BITS_Y = (1L << SIZE_BITS_Y) - 1L;
    private static final long BITS_Z = (1L << SIZE_BITS_XZ) - 1L;
    private static final int BIT_SHIFT_Z = SIZE_BITS_Y; // = 12
    private static final int BIT_SHIFT_X = SIZE_BITS_Y + SIZE_BITS_XZ; // = 38

    /**
     * 覆盖 asLong 方法，对坐标进行钳制以防止溢出。
     *
     * @reason 原版使用 26 位存储 X/Z，超出 ±33554432 时发生截断，
     *         导致打包后的 long 值不再单调递增，进而使 LongAVLTreeSet.subSet 崩溃。
     *         此修改将坐标钳制到 ±33554400 的安全范围内，保留含水层逻辑，同时防止崩溃。
     * @author INF32768
     */
    @Overwrite
    public static long asLong(int x, int y, int z) {
        if (!config.fixChunkSectionSubSetOverflow) {
            // 回退到原版逻辑（直接使用硬编码常量）
            long l = 0L;
            l |= ((long) x & BITS_X) << BIT_SHIFT_X;
            l |= ((long) y & BITS_Y) << 0;
            l |= ((long) z & BITS_Z) << BIT_SHIFT_Z;
            return l;
        }

        // 钳制坐标到安全范围（26 位有符号数的最大值，但保留边界余量）
        final int MAX_SAFE_COORD = 33554400;
        final int MIN_SAFE_COORD = -33554400;
        int clampedX = Math.max(MIN_SAFE_COORD, Math.min(MAX_SAFE_COORD, x));
        int clampedZ = Math.max(MIN_SAFE_COORD, Math.min(MAX_SAFE_COORD, z));
        // Y 轴钳制到 12 位范围 -2048~2047
        final int MAX_SAFE_Y = (1 << (SIZE_BITS_Y - 1)) - 1; // 2047
        final int MIN_SAFE_Y = -(1 << (SIZE_BITS_Y - 1));   // -2048
        int clampedY = Math.max(MIN_SAFE_Y, Math.min(MAX_SAFE_Y, y));

        long l = 0L;
        l |= ((long) clampedX & BITS_X) << BIT_SHIFT_X;
        l |= ((long) clampedY & BITS_Y) << 0;
        l |= ((long) clampedZ & BITS_Z) << BIT_SHIFT_Z;
        return l;
    }
}
