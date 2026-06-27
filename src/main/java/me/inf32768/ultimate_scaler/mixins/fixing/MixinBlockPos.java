package me.inf32768.ultimate_scaler.mixins.fixing;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 修复 BlockPos.asLong 因坐标超出 26 位有符号范围（±33554432）导致的溢出问题。
 * 通过钳制 X/Z 坐标到安全区间，确保打包结果不会产生非法 from/to 区间。
 */
@Mixin(BlockPos.class)
public abstract class MixinBlockPos {

    private static final int MAX_SAFE_COORD = 33554432;   // 2^25
    private static final int MIN_SAFE_COORD = -33554432;  // -2^25

    /**
     * 在 BlockPos.asLong 执行前，钳制 X 和 Z 坐标。
     * 如果坐标超出范围，则替换返回值为钳制后的打包结果。
     */
    @Inject(
        method = "asLong",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void clampAsLong(int x, int y, int z, CallbackInfoReturnable<Long> cir) {
        if (!config.fixBlockPosOverflow) return;

        // 钳制 X 和 Z
        int clampedX = MathHelper.clamp(x, MIN_SAFE_COORD, MAX_SAFE_COORD - 1); // 最大值为 33554431
        int clampedZ = MathHelper.clamp(z, MIN_SAFE_COORD, MAX_SAFE_COORD - 1);

        // 如果坐标未变，不干预，让原版逻辑走（避免重复调用）
        if (clampedX == x && clampedZ == z) return;

        // 重新计算打包值
        long packed = BlockPos.asLong(clampedX, y, clampedZ);
        cir.setReturnValue(packed);
    }
}
