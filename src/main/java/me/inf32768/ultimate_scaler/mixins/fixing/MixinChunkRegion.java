package me.inf32768.ultimate_scaler.mixins.fixing;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

/**
 * 覆盖 ChunkRegion.getChunk 方法，当请求的区块坐标超出合理范围时，
 * 返回 EmptyChunk 而不是抛出 IllegalStateException。
 */
@Mixin(ChunkRegion.class)
public abstract class MixinChunkRegion {

    @Shadow
    private ServerWorld world;

    @Shadow
    private Chunk centerPos;

    /**
     * 拦截 getChunk(int, int) 调用，在坐标超出 ±2,147,483,000 时返回空区块。
     */
    @Inject(method = "getChunk(II)Lnet/minecraft/world/chunk/Chunk;", at = @At("HEAD"), cancellable = true)
    private void redirectGetChunkInt(int chunkX, int chunkZ, CallbackInfoReturnable<Chunk> cir) {
        if (!config.fixGetChunkIllegal) return;

        // 如果坐标溢出 int 范围或接近极限（小于 -2e9 或大于 2e9），直接返回空区块
        if (chunkX < -2147483600 || chunkX > 2147483600 || chunkZ < -2147483600 || chunkZ > 2147483600) {
            cir.setReturnValue(new EmptyChunk(world, new ChunkPos(chunkX, chunkZ)));
            return;
        }

        // 否则让原逻辑走，但如果世界中没有该区块，也返回空区块（原逻辑会抛异常，我们在这里捕获）
        // 但用 @Inject 没法阻止内部异常，所以我们只能提前返回，但如果坐标在合理范围内，原逻辑仍可能因为其他原因抛异常。
        // 我们也可以直接委托给 world.getChunk，但需要获取 chunk 对象，但 world 是 ServerWorld，它有 getChunk 方法。
        // 我们选择完全重写：直接调用 world.getChunk，如果为 null 则返回 EmptyChunk。
        Chunk chunk = world.getChunk(chunkX, chunkZ);
        if (chunk == null) {
            chunk = new EmptyChunk(world, new ChunkPos(chunkX, chunkZ));
        }
        cir.setReturnValue(chunk);
    }

    /**
     * 拦截 getChunk(ChunkPos) 调用，同样处理。
     */
    @Inject(method = "getChunk(Lnet/minecraft/util/math/ChunkPos;)Lnet/minecraft/world/chunk/Chunk;", at = @At("HEAD"), cancellable = true)
    private void redirectGetChunkPos(ChunkPos pos, CallbackInfoReturnable<Chunk> cir) {
        if (!config.fixGetChunkIllegal) return;

        if (pos.x < -2147483600 || pos.x > 2147483600 || pos.z < -2147483600 || pos.z > 2147483600) {
            cir.setReturnValue(new EmptyChunk(world, pos));
            return;
        }

        Chunk chunk = world.getChunk(pos);
        if (chunk == null) {
            chunk = new EmptyChunk(world, pos);
        }
        cir.setReturnValue(chunk);
    }
}
