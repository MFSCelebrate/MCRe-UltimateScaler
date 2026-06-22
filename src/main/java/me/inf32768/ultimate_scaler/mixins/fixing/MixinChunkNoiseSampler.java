package me.inf32768.ultimate_scaler.mixins.fixing;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
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
     * 拦截 getAquiferSampler 方法，在返回时用代理包装原始实例。
     */
    @Inject(method = "getAquiferSampler", at = @At("RETURN"), cancellable = true)
    private void wrapAquiferSampler(CallbackInfoReturnable<AquiferSampler> cir) {
        if (!config.fixChunkSectionSubSetOverflow) return;

        AquiferSampler original = cir.getReturnValue();
        if (original == null) return;

        // 获取当前区块的坐标（从 Shadow 字段读取）
        int chunkX = this.firstBlockX >> 4;
        int chunkZ = this.firstBlockZ >> 4;

        // 如果区块坐标在安全范围内（±33554432/16 = ±2097152），直接返回原始实例
        if (Math.abs(chunkX) <= 2097152 && Math.abs(chunkZ) <= 2097152) {
            return;
        }

        // 否则用代理包装
        cir.setReturnValue(new SafeAquiferSampler(original, chunkX, chunkZ));
    }

    /**
     * AquiferSampler 的安全代理类。
     * 在极远坐标下，拦截可能触发内部溢出的方法。
     */
    private static class SafeAquiferSampler implements AquiferSampler {
        private final AquiferSampler delegate;
        private final int chunkX;
        private final int chunkZ;

        SafeAquiferSampler(AquiferSampler delegate, int chunkX, int chunkZ) {
            this.delegate = delegate;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        /**
         * 判断是否需要流体刻。
         * 在极远坐标下，直接返回 false，避免进入内部逻辑。
         */
        @Override
        public boolean needsFluidTick() {
            // 如果坐标超出安全范围，跳过流体刻
            if (Math.abs(chunkX) > 2097152 || Math.abs(chunkZ) > 2097152) {
                return false;
            }
            return delegate.needsFluidTick();
        }

        /**
         * 获取流体级别。
         * 在极远坐标下，返回一个安全的默认值（空气）。
         */
        @Override
        public AquiferSampler.FluidLevel getFluidLevel(int x, int y, int z) {
            // 如果坐标超出安全范围，返回空气
            if (Math.abs(x) > 33554432 || Math.abs(z) > 33554432) {
                return new AquiferSampler.FluidLevel(Integer.MIN_VALUE, net.minecraft.block.Blocks.AIR.getDefaultState());
            }
            return delegate.getFluidLevel(x, y, z);
        }

        /**
         * 标记方块需要后处理。
         * 在极远坐标下，跳过标记，避免后续处理崩溃。
         */
        @Override
        public void markBlockForPostProcessing(BlockPos pos) {
            if (Math.abs(pos.getX()) > 33554432 || Math.abs(pos.getZ()) > 33554432) {
                return;
            }
            delegate.markBlockForPostProcessing(pos);
        }

        /**
         * 获取原始实例的字符串表示（用于调试）。
         */
        @Override
        public String toString() {
            return "SafeAquiferSampler{delegate=" + delegate + ", chunkX=" + chunkX + ", chunkZ=" + chunkZ + "}";
        }
    }
}
