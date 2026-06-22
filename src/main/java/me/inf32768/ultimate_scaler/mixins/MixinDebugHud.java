package me.inf32768.ultimate_scaler.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import me.inf32768.ultimate_scaler.util.Util;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static me.inf32768.ultimate_scaler.option.UltimateScalerOptions.config;

@Environment(EnvType.CLIENT)
@Mixin(DebugHud.class)
public abstract class MixinDebugHud {
    // 缓存变量（不变）
    @Unique
    private static BlockPos previousCamPos;
    @Unique
    private static BigDecimal[] previousOffset;
    @Unique
    private static BigDecimal[] previousScale;
    @Unique
    private static String[] terrainPosLines;

    /**
     * 原有的 @Inject，添加 TerrainXYZ 并修改品牌名
     */
    @Inject(at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 4),
            method = "getLeftText")
    protected void getLeftText(CallbackInfoReturnable<List<String>> cir,
                               @Local List<String> list) {
        // ========== 新增：修改品牌名 ==========
        if (!list.isEmpty()) {
            String first = list.get(0);
            if (first.startsWith("Minecraft ")) {
                // 保留原版本号等信息，只替换前缀
                list.set(0, "Minecraft - MCRe UltimateScaler " + first.substring("Minecraft ".length()));
            }
        }

        // ========== 原有的 TerrainXYZ 逻辑（保持不变） ==========
        MinecraftClient mc = MinecraftClient.getInstance();
        BlockPos pos = null;
        if (mc.getCameraEntity() != null) {
            pos = mc.getCameraEntity().getBlockPos();
        }
        if (pos == null) {
            return;
        }

        if (config.showTerrainPos) {
            if (!pos.equals(previousCamPos) ||
                !Arrays.equals(config.globalBigDecimalOffset, previousOffset) ||
                !Arrays.equals(config.globalBigDecimalScale, previousScale)) {
                previousCamPos = pos;
                previousOffset = config.globalBigDecimalOffset;
                previousScale = config.globalBigDecimalScale;

                if (config.bigIntegerRewrite) {
                    String x = Util.RepositionBigDecimal(pos.getX(), Direction.Axis.X).toString();
                    String y = Util.RepositionBigDecimal(pos.getY(), Direction.Axis.Y).toString();
                    String z = Util.RepositionBigDecimal(pos.getZ(), Direction.Axis.Z).toString();
                    terrainPosLines = new String[]{
                        String.format(Locale.ROOT, "TerrainXYZ: %s %s %s", x, y, z),
                        String.format(Locale.ROOT, "TerrainXYZ (double): %.0f %.0f %.0f",
                                      Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z))
                    };
                } else {
                    double x = Util.RepositionDouble(pos.getX(), Direction.Axis.X);
                    double y = Util.RepositionDouble(pos.getY(), Direction.Axis.Y);
                    double z = Util.RepositionDouble(pos.getZ(), Direction.Axis.Z);
                    terrainPosLines = new String[]{
                        String.format(Locale.ROOT, "TerrainXYZ: %.0f %.0f %.0f", x, y, z),
                        null
                    };
                }
            }

            if (config.bigIntegerRewrite) {
                list.add(terrainPosLines[0]);
                list.add(terrainPosLines[1]);
            } else {
                list.add(terrainPosLines[0]);
            }
        }
    }
}
