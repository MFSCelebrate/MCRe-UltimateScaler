package me.inf32768.ultimate_scaler.mixins.fixing;

import me.inf32768.ultimate_scaler.util.CoordinateHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
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
 * 拦截 AquiferSampler.Impl 内部所有打包调用。
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
     * 拦截 BlockPos.asLong(x, y, z)。
     */
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/BlockPos;asLong(III)J"
        ),
        remap = true,
        require = 0
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
     * 拦截 ChunkSectionPos.asLong(x, y, z)。
     */
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/ChunkSectionPos;asLong(III)J"
        ),
        remap = true,
        require = 0
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

    /**
     * 拦截 ChunkPos.toLong(chunkX, chunkZ)。
     */
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/ChunkPos;toLong(II)J"
        ),
        remap = true,
        require = 0
    )
    private long redirectChunkPosToLong(int chunkX, int chunkZ) {
        if (!config.fixChunkSectionSubSetOverflow) {
            return ChunkPos.toLong(chunkX, chunkZ);
        }
        BlockPos pos = CoordinateHolder.get();
        if (pos != null) {
            return ChunkPos.toLong(
                ChunkSectionPos.getSectionCoord(pos.getX()),
                ChunkSectionPos.getSectionCoord(pos.getZ())
            );
        }
        return ChunkPos.toLong(chunkX, chunkZ);
    }
}
