package me.inf32768.ultimate_scaler.mixins.fixing;

import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.world.entity.SectionedEntityCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 拦截 SectionedEntityCache 中所有对 trackedPositions.subSet 的调用，
 * 通过交换参数防止 from > to 导致的 IllegalArgumentException。
 */
@Mixin(SectionedEntityCache.class)
public abstract class MixinSectionedEntityCache {

    @Shadow
    private LongSortedSet trackedPositions;

    /**
     * 重定向所有 subSet 调用，交换参数顺序如果 from > to。
     */
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/LongSortedSet;subSet(JJ)Lit/unimi/dsi/fastutil/longs/LongSortedSet;"
        )
    )
    private LongSortedSet fixSubSet(LongSortedSet instance, long from, long to) {
        if (config.fixChunkSectionSubSetOverflow && from > to) {
            long temp = from;
            from = to;
            to = temp;
        }
        return instance.subSet(from, to);
    }
}
