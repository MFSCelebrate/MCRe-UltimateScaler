package xyz.inf32768.ultimatescaler.config;

import com.moandjiezana.toml.Toml;
import org.lwjgl.glfw.GLFW;

import java.math.BigDecimal;

/**
 * 本模组的配置类，集中了所有配置选项的定义和加载、读取配置文件的功能。
 */
public final class Config {
    //Don't let anyone instantiate this class
    private Config() {
    }

    /**
     * 定义“边境之地位置”选项的枚举类。
     */
    public enum FarLandsPos {
        BETA,
        RELEASE,
        DEFAULT,
        REMOVED,
        CUSTOM
    }

    /**
     * 所有配置选项的定义类，定义了所有配置选项的名称、类型和默认值。这样定义的好处是可以用 {@link Toml#to(Class)} 自动将配置文件转换为配置实例。
     * <p>
     * 有关配置选项的详细说明，请参考 Wiki 中的页面<a href="https://github.com/INF32768/UltimateScaler/wiki/UserGuide.Configuration.zh">《配置全解》</a>。
     */
    public static class ConfigImpl {
        public int CONFIG_VERSION = 3;
        public BigDecimal[] globalBigDecimalOffset = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        public BigDecimal[] globalBigDecimalScale = {BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE};
        public int optionMenuKeyCode = GLFW.GLFW_KEY_U;
        public short optionMenuModifierValue = 2;
        public boolean showTerrainPos = true;
        public FarLandsPos farLandsPos = FarLandsPos.DEFAULT;
        public double maintainPrecisionCustomDivisor = 33554432;
        public boolean limitReturnValue = false;
        public int maxNoiseLogarithmValue = 7;
        public boolean extraYOffset = false;
        public boolean bigIntegerRewrite = false;
        public boolean fixEndRings = false;
        public boolean fixChunkGenerationOutOfBound = true;
        public boolean expandDatapackValueRange = true;
        public boolean expandWorldBorder = true;
        public boolean fixMineshaftCannotGenerate = true;
        public boolean replaceDefaultFluid = false;
        public String replaceDefaultFluidBlock = "minecraft:air";
        public boolean replaceUndergroundLava = false;
        public String replaceUndergroundLavaBlock = "minecraft:air";
        public boolean publicTerrainPos = true;
    }
}