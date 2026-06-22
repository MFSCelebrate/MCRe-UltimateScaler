package me.inf32768.ultimate_scaler.mixins.fixing;

import me.inf32768.ultimate_scaler.util.CoordinateHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 在 ChunkNoiseSampler 采样时，将当前 BlockPos 存入 ThreadLocal，
 * 供下游的 AquiferSampler 使用，从而绕过打包/解包。
 * 
 * <p>核心思路：
 * <ul>
 *   <li>在采样开始时（sampleStartDensity），将当前坐标存入 ThreadLocal</li>
 *   <li>在采样结束时（stopInterpolation），清理 ThreadLocal</li>
 *   <li>AquiferSampler.Impl 内部的打包调用会从 ThreadLocal 读取原始坐标</li>
 * </ul>
 */
@Mixin(ChunkNoiseSampler.class)
public abstract class MixinChunkNoiseSampler {

    @Shadow
    private int firstBlockX;

    @Shadow
    private int firstBlockY;

    @Shadow
    private int firstBlockZ;

    /**
     * 在采样开始时，将当前坐标存入 ThreadLocal。
     */
    @Inject(method = "sampleStartDensity", at = @At("HEAD"))
    private void onSampleStart(CallbackInfo ci) {
        if (!config.fixChunkSectionSubSetOverflow) return;
        CoordinateHolder.set(new BlockPos(firstBlockX, firstBlockY, firstBlockZ));
    }

    /**
     * 在采样结束时，清理 ThreadLocal，防止内存泄漏。
     */
    @Inject(method = "stopInterpolation", at = @At("HEAD"))
    private void onStopInterpolation(CallbackInfo ci) {
        if (!config.fixChunkSectionSubSetOverflow) return;
        CoordinateHolder.clear();
    }
}
