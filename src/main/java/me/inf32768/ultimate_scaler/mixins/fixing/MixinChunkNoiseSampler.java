package me.inf32768.ultimate_scaler.mixins.fixing;

import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Accessor;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 修复 ChunkSectionPos.asLong 打包溢出问题。
 * 当坐标超出 ±33554432 时，将坐标钳制到安全范围内，
 * 而不是让其截断溢出。
 */
@Mixin(ChunkSectionPos.class)
public abstract class MixinChunkSectionPos {

    @Accessor("field_33104")
    private static native long getBITS_X();

    @Accessor("field_33105")
    private static native long getBITS_Y();

    @Accessor("field_33106")
    private static native long getBITS_Z();

    @Accessor("field_33109")
    private static native int getBIT_SHIFT_X();

    @Accessor("field_33108")
    private static native int getBIT_SHIFT_Z();

    /**
     * 覆盖 asLong 方法，对坐标进行钳制以防止溢出。
     *
     * @reason 原版使用 26 位存储 X/Z，超出 ±33554432 时发生截断，
     *         导致打包后的 long 值不再单调递增，进而使 LongAVLTreeSet.subSet 崩溃。
     *         此修改将坐标钳制到安全范围内，保留含水层逻辑，同时防止崩溃。
     * @author INF32768
     */
    @Overwrite
    public static long asLong(int x, int y, int z) {
        if (!config.fixChunkSectionSubSetOverflow) {
            // 回退到原版逻辑（使用 Accessor 获取私有常量）
            long l = 0L;
            l |= ((long) x & getBITS_X()) << getBIT_SHIFT_X();
            l |= ((long) y & getBITS_Y()) << 0;
            l |= ((long) z & getBITS_Z()) << getBIT_SHIFT_Z();
            return l;
        }

        // ✨ 钳制坐标到安全范围（26 位有符号数的最大值）
        final int MAX_SAFE_COORD = 33554431;
        final int MIN_SAFE_COORD = -33554432;

        int clampedX = Math.max(MIN_SAFE_COORD, Math.min(MAX_SAFE_COORD, x));
        int clampedZ = Math.max(MIN_SAFE_COORD, Math.min(MAX_SAFE_COORD, z));
        // Y 轴只有 12 位，范围 -2048~2047，原版已经够用，但以防万一也做钳制
        final int MAX_SAFE_Y = 2047;
        final int MIN_SAFE_Y = -2048;
        int clampedY = Math.max(MIN_SAFE_Y, Math.min(MAX_SAFE_Y, y));

        // 使用钳制后的坐标进行打包
        long l = 0L;
        l |= ((long) clampedX & getBITS_X()) << getBIT_SHIFT_X();
        l |= ((long) clampedY & getBITS_Y()) << 0;
        l |= ((long) clampedZ & getBITS_Z()) << getBIT_SHIFT_Z();
        return l;
    }
}
