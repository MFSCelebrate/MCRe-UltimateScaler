package me.inf32768.ultimate_scaler.mixins.fixing;

import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 修复 LongAVLTreeSet.subSet 参数顺序，防止因坐标打包溢出导致的 from > to。
 */
@Mixin(LongAVLTreeSet.class)
public abstract class MixinLongAVLTreeSet {

    @ModifyArgs(
        method = "subSet(JJ)",
        at = @At(value = "HEAD"),
        remap = false
    )
    private void fixSubSetArgs(Args args) {
        if (!config.fixChunkSectionSubSetOverflow) return;

        long from = args.get(0);
        long to = args.get(1);

        // 如果 from > to，交换它们
        if (from > to) {
            args.set(0, to);
            args.set(1, from);
        }
    }
}
