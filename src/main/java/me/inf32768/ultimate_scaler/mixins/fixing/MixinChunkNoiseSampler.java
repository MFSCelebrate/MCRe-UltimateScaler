package me.inf32768.ultimate_scaler.mixins.fixing;

import net.minecraft.block.Blocks;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

@Mixin(ChunkNoiseSampler.class)
public abstract class MixinChunkNoiseSampler {

    @Shadow
    private int firstBlockX;

    @Shadow
    private int firstBlockZ;

    /**
     * 拦截 getAquiferSampler 方法，在极远坐标下返回安全的 AquiferSampler 实现。
     */
    @Inject(method = "getAquiferSampler", at = @At("RETURN"), cancellable = true)
    private void wrapAquiferSampler(CallbackInfoReturnable<AquiferSampler> cir) {
        if (!config.fixChunkSectionSubSetOverflow) return;

        AquiferSampler original = cir.getReturnValue();
        if (original == null) return;

        int chunkX = this.firstBlockX >> 4;
        int chunkZ = this.firstBlockZ >> 4;

        // 坐标在安全范围内，直接返回原始实例
        if (Math.abs(chunkX) <= 2097152 && Math.abs(chunkZ) <= 2097152) {
            return;
        }

        // 极远坐标：返回安全的匿名实现
        cir.setReturnValue(new AquiferSampler() {
            @Override
            public AquiferSampler.FluidLevel apply(DensityFunction.NoisePos pos, double density) {
                // 二次检查，防止传递了极端坐标
                if (Math.abs(pos.blockX()) > 33554432 || Math.abs(pos.blockZ()) > 33554432) {
                    return new AquiferSampler.FluidLevel(Integer.MIN_VALUE, Blocks.AIR.getDefaultState());
                }
                return original.apply(pos, density);
            }

            @Override
            public boolean needsFluidTick() {
                // 极远坐标下不进行流体刻，避免内部溢出
                return false;
            }
        });
    }
}
