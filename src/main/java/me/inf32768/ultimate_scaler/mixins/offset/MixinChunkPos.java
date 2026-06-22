// MixinChunkPos.java
package me.inf32768.ultimate_scaler.mixins.offset;

import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

@Mixin(ChunkPos.class)
public abstract class MixinChunkPos {

    /**
     * @reason 与 BlockPos.asLong 保持一致，扩展 X/Z 为 32 位
     * @author INF32768
     */
    @Overwrite
    public static long toLong(int chunkX, int chunkZ) {
        if (!config.expandWorldBorder) {
            // 原版逻辑（原版已经是 64 位打包，无需修改）
            return ((long) chunkX & 0xFFFFFFFFL) << 32 | ((long) chunkZ & 0xFFFFFFFFL);
        }
        // 新打包方案：X 用 32 位，Z 用 32 位
        long packed = 0L;
        packed |= ((long) chunkX & 0xFFFFFFFFL) << 32;
        packed |= ((long) chunkZ & 0xFFFFFFFFL);
        return packed;
    }
}
