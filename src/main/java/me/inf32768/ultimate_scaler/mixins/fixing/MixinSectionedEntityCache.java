package me.inf32768.ultimate_scaler.mixins.fixing;

import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.world.entity.SectionedEntityCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

@Mixin(SectionedEntityCache.class)
public abstract class MixinSectionedEntityCache {

    @Shadow
    private LongSortedSet trackedPositions;

    /**
     * 覆盖 getSections 方法，修复 subSet 参数顺序。
     *
     * @reason 当坐标溢出时，from 可能大于 to，此处交换参数保证区间有效
     * @author INF32768
     */
    @Overwrite
    private LongSortedSet getSections(int chunkX, int chunkZ) {
        long l = net.minecraft.util.math.ChunkSectionPos.asLong(chunkX, 0, chunkZ);
        long m = net.minecraft.util.math.ChunkSectionPos.asLong(chunkX, -1, chunkZ);
        long from = l;
        long to = m + 1L;

        if (config.fixChunkSectionSubSetOverflow && from > to) {
            long temp = from;
            from = to;
            to = temp;
        }

        return this.trackedPositions.subSet(from, to);
    }
}
