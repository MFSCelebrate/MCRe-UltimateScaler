// MixinBlockPos.java
package me.inf32768.ultimate_scaler.mixins.offset;

import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Accessor;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

@Mixin(BlockPos.class)
public abstract class MixinBlockPos {

    @Accessor("BITS_X")
    private static native long getBITS_X();

    @Accessor("BITS_Y")
    private static native long getBITS_Y();

    @Accessor("BITS_Z")
    private static native long getBITS_Z();

    @Accessor("BIT_SHIFT_X")
    private static native int getBIT_SHIFT_X();

    @Accessor("BIT_SHIFT_Z")
    private static native int getBIT_SHIFT_Z();

    /**
     * @reason 防止 X/Z 坐标打包溢出，将 X/Z 扩展为 32 位，Y 保持 12 位
     * @author INF32768
     */
    @Overwrite
    public static long asLong(int x, int y, int z) {
        if (!config.expandWorldBorder) {
            // 回退到原版逻辑（用 Accessor 获取私有常量）
            long l = 0L;
            l |= ((long) x & getBITS_X()) << getBIT_SHIFT_X();
            l |= ((long) y & getBITS_Y()) << 0;
            l |= ((long) z & getBITS_Z()) << getBIT_SHIFT_Z();
            return l;
        }

        // 🚀 新打包方案：X 用 32 位，Y 用 12 位（原版），Z 用 32 位
        long packed = 0L;
        packed |= ((long) x & 0xFFFFFFFFL) << 44;  // X 在最高位
        packed |= ((long) y & 0xFFFL) << 32;       // Y 在中间，12 位
        packed |= ((long) z & 0xFFFFFFFFL);        // Z 在最低位
        return packed;
    }
}
