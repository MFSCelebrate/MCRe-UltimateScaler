package me.inf32768.ultimate_scaler.mixins.fixing;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

@Mixin(ChunkRegion.class)
public abstract class MixinChunkRegion {

    @Shadow
    private ServerWorld world;

    /**
     * 拦截 getChunk(int, int) 调用，防止因坐标溢出导致的 IllegalStateException。
     */
    @Inject(method = "getChunk(II)Lnet/minecraft/world/chunk/Chunk;", at = @At("HEAD"), cancellable = true)
    private void redirectGetChunk(int chunkX, int chunkZ, CallbackInfoReturnable<Chunk> cir) {
        if (!config.fixGetChunkIllegal) return;

        ChunkPos pos = new ChunkPos(chunkX, chunkZ);

        // 如果坐标超出合理范围（32位整数极限附近），返回空区块
        if (pos.x < -2147483600 || pos.x > 2147483600 || pos.z < -2147483600 || pos.z > 2147483600) {
            RegistryEntry<Biome> biome = world.getBiome(pos.getStartPos());
            cir.setReturnValue(new EmptyChunk(world, pos, biome));
            return;
        }

        // 尝试获取区块，如果为 null 则返回空区块
        Chunk chunk = world.getChunk(chunkX, chunkZ);
        if (chunk == null) {
            RegistryEntry<Biome> biome = world.getBiome(pos.getStartPos());
            chunk = new EmptyChunk(world, pos, biome);
        }
        cir.setReturnValue(chunk);
    }
}
