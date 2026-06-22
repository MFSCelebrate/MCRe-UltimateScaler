package me.inf32768.ultimate_scaler.mixins.fixing;

import me.inf32768.ultimate_scaler.util.CoordinateHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 拦截 AquiferSampler.Impl 内部所有 BlockPos.asLong 调用。
 * 利用 ThreadLocal 传递原始 BlockPos，绕过打包溢出。
 */
@Mixin(targets = "net.minecraft.world.gen.chunk.AquiferSampler$Impl")
public abstract class MixinAquiferSamplerImpl {

    /**
     * 在 apply 方法入口，将坐标存入 ThreadLocal。
     */
    @Inject(
        method = "apply",
        at = @At("HEAD"),
        remap = true
    )
    private void onApplyHead(DensityFunction.NoisePos pos, double density, CallbackInfoReturnable<AquiferSampler.FluidLevel> cir) {
        if (!config.fixChunkSectionSubSetOverflow) return;
        CoordinateHolder.set(new BlockPos(pos.blockX(), pos.blockY(), pos.blockZ()));
    }

    /**
     * 在 apply 方法出口，清理 ThreadLocal。
     */
    @Inject(
        method = "apply",
        at = @At("RETURN"),
        remap = true
    )
    private void onApplyReturn(DensityFunction.NoisePos pos, double density, CallbackInfoReturnable<AquiferSampler.FluidLevel> cir) {
        if (!config.fixChunkSectionSubSetOverflow) return;
        CoordinateHolder.clear();
    }

    /**
     * 拦截 BlockPos.asLong(x, y, z) 调用。
     * 如果 ThreadLocal 中有原始 BlockPos，则使用原始坐标重新打包。
     */
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/BlockPos;asLong(III)J"
        ),
        remap = true
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
}
