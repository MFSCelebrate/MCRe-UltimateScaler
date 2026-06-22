package me.inf32768.ultimate_scaler.mixins.fixing;

import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

@Mixin(ChunkSection.class)
public abstract class MixinChunkSection {

    @ModifyArgs(
        method = "*",  // 匹配 ChunkSection 中所有方法
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/LongAVLTreeSet;subSet(JJ)Lit/unimi/dsi/fastutil/longs/LongSortedSet;",
            remap = false
        )
    )
    private void fixSubSetArgs(Args args) {
        if (!config.fixChunkSectionSubSetOverflow) return;

        long from = args.get(0);
        long to = args.get(1);

        // 如果 from > to，交换它们（确保正常区间）
        if (from > to) {
            args.set(0, to);
            args.set(1, from);
        }
    }
}
