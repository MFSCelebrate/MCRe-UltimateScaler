package me.inf32768.ultimate_scaler.mixins.fixing;

import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 修复因坐标打包溢出导致的 {@link LongAVLTreeSet#subSet(long, long)} 参数顺序错误。
 * <p>
 * 当 X/Z 坐标超出 ±33554432 时，打包后的 long 值可能产生不可预期的位模式，
 * 导致 from > to，引发 IllegalArgumentException。
 * 此 Mixin 在调用 subSet 前交换参数，确保 from <= to。
 */
@Mixin(ChunkSection.class)
public abstract class MixinChunkSection {

    /**
     * 拦截 subSet 调用，修正参数顺序。
     * <p>
     * 原版调用链：ChunkSection.method_31771 -> LongAVLTreeSet.subSet(from, to)
     * 我们在此处交换 from 和 to，并可选地 clamp 到合理范围。
     */
    @ModifyArgs(
        method = "method_31771",  // 映射名，对应 ChunkSection 中的某个方法
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

        // 可选：额外 clamp 到实际世界边界，防止内存耗尽（但会丢失极远区块）
        // long MAX_COORD = 30000000L;  // 可根据配置调整
        // if (args.get(0) < -MAX_COORD) args.set(0, -MAX_COORD);
        // if (args.get(1) > MAX_COORD) args.set(1, MAX_COORD);
    }
}
