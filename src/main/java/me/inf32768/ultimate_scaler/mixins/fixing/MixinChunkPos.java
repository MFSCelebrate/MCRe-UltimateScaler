package me.inf32768.ultimate_scaler.mixins.fixing;

import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 覆盖 ChunkPos 构造函数，将超出 int 范围的参数钳制到合法边界。
 * 防止因 BlockPos 溢出导致 ChunkPos 坐标变成极大的负数。
 */
@Mixin(ChunkPos.class)
public abstract class MixinChunkPos {

    /**
     * 覆盖 (int, int) 构造函数，对 x 和 z 进行钳制。
     */
    @Overwrite
    public ChunkPos(int x, int z) {
        if (config.fixChunkPosIntOverFlow) {
            // 如果 x 或 z 小于 -2,000,000,000，很可能是正向溢出导致的负值，将其设为 Integer.MAX_VALUE
            if (x < -2000000000) x = Integer.MAX_VALUE;
            if (z < -2000000000) z = Integer.MAX_VALUE;
            // 如果大于 2,000,000,000 但还在 int 范围内，可能是真正的坐标，但原版本身限制了最大 30,000,000，但我们不限制
            // 只处理负溢出
        }
        // 注意：因为覆盖了构造函数，我们不能调用 super，只能自己赋值
        // 但字段是 final，在构造函数中赋值是允许的
        // 但我们不能直接访问 this.x，因为它们是 final，但我们可以通过反射？不，可以直接赋值
        // 但这是抽象类，我们无法直接访问 final 字段？实际上，在 Mixin 中，我们可以直接使用 this.x 和 this.z 吗？
        // 因为我们的 Mixin 目标类是 ChunkPos，我们可以直接使用 this.x = x; this.z = z; 但字段是 public final，所以可以。
        // 但为了安全，我们使用 ((ChunkPos)(Object)this).x 等，但更简单是直接 super()？但 ChunkPos 没有无参构造。
        // 所以我们必须手动设置。但因为它们是 final，必须在构造函数中完成，所以我们直接赋值：
        // 由于我们覆盖了整个构造函数，原构造函数并不存在，所以我们必须完全实现。
        // 但原构造函数只是简单赋值，我们复制该逻辑。
        this.x = x;
        this.z = z;
    }

    /**
     * 覆盖 (long) 构造函数，解包后可能溢出，同样钳制。
     */
    @Overwrite
    public ChunkPos(long pos) {
        int x = (int)pos;
        int z = (int)(pos >> 32);
        if (config.fixChunkPosIntOverFlow) {
            if (x < -2147483600) x = Integer.MAX_VALUE;
            if (z < -2147483600) z = Integer.MAX_VALUE;
        }
        this.x = x;
        this.z = z;
    }
}
