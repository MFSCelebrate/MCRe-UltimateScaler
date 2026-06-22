package me.inf32768.ultimate_scaler.mixins.fixing;

import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Invoker;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 修复 LongAVLTreeSet.subSet 参数顺序错误。
 * 当 from > to 时，交换参数，确保区间有效。
 */
@Mixin(LongAVLTreeSet.class)
public abstract class MixinLongAVLTreeSet {

    /**
     * 调用原始的 subSet 方法（不进行参数交换）。
     */
    @Invoker("subSet")
    abstract LongSortedSet invokeSubSet(long from, long to);

    /**
     * 覆盖 subSet 方法，修复参数顺序。
     */
    @Overwrite
    public LongSortedSet subSet(long from, long to) {
        if (!config.fixChunkSectionSubSetOverflow) {
            return invokeSubSet(from, to);
        }
        if (from > to) {
            long temp = from;
            from = to;
            to = temp;
        }
        return invokeSubSet(from, to);
    }
}
