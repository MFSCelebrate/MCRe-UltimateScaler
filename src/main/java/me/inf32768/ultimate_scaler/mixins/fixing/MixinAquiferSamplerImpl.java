package me.inf32768.ultimate_scaler.mixins.fixing;

import me.inf32768.ultimate_scaler.util.CoordinateHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 拦截 AquiferSampler.Impl 内部的所有 BlockPos.asLong 和 ChunkSectionPos.asLong 调用。
 * 如果 ThreadLocal 中有原始坐标，则使用原始坐标替代打包后的值。
 * 
 * <p>这是“伪传参法”的核心实现：
 * <ul>
 *   <li>原版：BlockPos → asLong() 打包 → 传递 long → 解包 → 使用坐标</li>
 *   <li>伪传参法：BlockPos → 塞进 ThreadLocal → 在目标方法中读取 → 直接使用坐标</li>
 * </ul>
 */
@Mixin(targets = "net.minecraft.world.gen.chunk.AquiferSampler$Impl")
public abstract class MixinAquiferSamplerImpl {

    /**
     * 拦截 BlockPos.asLong(x, y, z) 调用。
     * 如果 ThreadLocal 中有原始 BlockPos，则直接使用原始坐标重新打包，
     * 否则回退到原版逻辑。
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
            // 使用原始坐标重新打包
            return BlockPos.asLong(pos.getX(), pos.getY(), pos.getZ());
        }
        // 降级：ThreadLocal 中没有坐标（理论上不应发生）
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
