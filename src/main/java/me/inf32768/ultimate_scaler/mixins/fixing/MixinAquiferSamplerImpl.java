package me.inf32768.ultimate_scaler.mixins.fixing;

import me.inf32768.ultimate_scaler.util.CoordinateHolder;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 最终安全方案：在极远坐标下跳过含水层，防止数组越界崩溃。
 * 安全范围内含水层完全正常。
 */
@Mixin(targets = "net.minecraft.class_6350$class_5832") // AquiferSampler.Impl
public abstract class MixinAquiferSamplerImpl {

    /**
     * 在 apply 方法入口检测坐标，如果超出安全范围则返回空气。
     */
    @Inject(
        method = "apply",
        at = @At("HEAD"),
        cancellable = true,
        remap = true
    )
    private void onApply(DensityFunction.NoisePos pos, double density, CallbackInfoReturnable<AquiferSampler.FluidLevel> cir) {
        if (!config.fixChunkSectionSubSetOverflow) return;

        int x = pos.blockX();
        int z = pos.blockZ();
        final int MAX_SAFE_COORD = 33554400;

        if (Math.abs(x) > MAX_SAFE_COORD || Math.abs(z) > MAX_SAFE_COORD) {
            // 极远坐标：返回空气状态，防止数组越界
            cir.setReturnValue(new AquiferSampler.FluidLevel(Integer.MIN_VALUE, Blocks.AIR.getDefaultState()));
        }
    }
}
