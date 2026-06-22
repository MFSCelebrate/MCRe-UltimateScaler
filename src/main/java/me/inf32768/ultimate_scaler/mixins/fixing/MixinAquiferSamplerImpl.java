package me.inf32768.ultimate_scaler.mixins.fixing;

import me.inf32768.ultimate_scaler.util.CoordinateHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 拦截 AquiferSampler.Impl 内部的所有 BlockPos.asLong 和 ChunkSectionPos.asLong 调用。
 * 
 * <p>核心逻辑（伪传参法）：
 * <ol>
 *   <li>在 apply 方法入口，将 NoisePos 中的坐标存入 ThreadLocal</li>
 *   <li>在 apply 方法出口（RETURN），清理 ThreadLocal</li>
 *   <li>在内部的 asLong 调用点，从 ThreadLocal 读取原始坐标替代打包值</li>
 * </ol>
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

    /**
     * 拦截 ChunkSectionPos.asLong(x, y, z) 调用。
     * 如果 ThreadLocal 中有原始 BlockPos，则使用原始坐标计算区块坐标后重新打包。
     */
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/ChunkSectionPos;asLong(III)J"
        ),
        remap = true
    )
    private long redirectChunkSectionPosAsLong(int x, int y, int z) {
        if (!config.fixChunkSectionSubSetOverflow) {
            return ChunkSectionPos.asLong(x, y, z);
        }
        BlockPos pos = CoordinateHolder.get();
        if (pos != null) {
            return ChunkSectionPos.asLong(
                ChunkSectionPos.getSectionCoord(pos.getX()),
                ChunkSectionPos.getSectionCoord(pos.getY()),
                ChunkSectionPos.getSectionCoord(pos.getZ())
            );
        }
        return ChunkSectionPos.asLong(x, y, z);
    }
}
