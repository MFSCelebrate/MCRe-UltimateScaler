package xyz.inf32768.ultimatescaler.config;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import xyz.inf32768.ultimatescaler.ModMetadata;
import xyz.inf32768.ultimatescaler.versionutil.RegistryAccessor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * 本模组的配置类，集中了所有配置选项的定义和加载、读取配置文件的功能。
 */
public final class Config {
    /**
     * 配置文件路径，用于存储和读取配置文件。
     */
    public static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("ultimate_scaler.toml");
    /**
     * 配置文件版本，用于判断配置文件的版本是否与当前模组的版本兼容、是否需要更新。
     * 这一变量会被一并存入配置文件中。
     */
    public static final int CONFIG_VERSION = 3;
    /**
     * 配置实例，用于存储配置选项的值，运行时所有的配置选项的值都通过这个实例来访问和修改。
     */
    public static ConfigImpl impl;

    static {
        // 类加载时尝试读取配置文件
        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            ModMetadata.LOGGER.error(e.getMessage());
        }
    }

    //Don't let anyone instantiate this class
    private Config() {
    }

    /**
     * 读取配置文件，并将读取到的配置选项的值赋值给 {@link #impl} 实例。期间会检查并迁移旧版配置文件，并自动替换无效值。
     *
     * @throws IOException 配置文件读取或写入时出错
     */
    public static void loadConfig() throws IOException {
        if (!CONFIG_PATH.toFile().exists()) {
            // 默认配置文件不存在
            if (Files.exists(FabricLoader.getInstance().getConfigDir().resolve("ultimatescaler.toml"))) {
                // 存在旧版配置文件，迁移配置
                impl = new Toml().read(FabricLoader.getInstance().getConfigDir().resolve("ultimatescaler.toml").toFile()).to(ConfigImpl.class);
                Files.deleteIfExists(FabricLoader.getInstance().getConfigDir().resolve("ultimatescaler.toml"));
            }
            // 不存在任何配置文件，使用默认配置
            impl = new ConfigImpl();
        } else {
            try {
                // 存在配置文件，尝试读取配置
                impl = new Toml().read(CONFIG_PATH.toFile()).to(ConfigImpl.class);
            } catch (Exception e) {
                // 配置文件格式错误，由于不知道是哪里出错，因此只打印错误信息，并使用默认配置
                ModMetadata.LOGGER.error("[Ultimate Scaler] Failed to load config file, resetting to default values: {}", e.getMessage());
                impl = new ConfigImpl();
                saveConfig();
                return;
            }
            // 执行到这，说明配置文件格式正确，接下来校验字段合法性
            try {
                for (Field entry : ConfigImpl.class.getFields()) {
                    if (entry.get(impl) == null) {
                        // 字段值为空，使用默认值
                        ModMetadata.LOGGER.error("[Ultimate Scaler] Failed to load config entry, resetting to default value: {}", entry.getName());
                        entry.set(impl, ConfigImpl.class.getField(entry.getName()).get(new ConfigImpl()));
                    }
                }
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
            try {
                // 确保配置文件中的方块 ID 有效
                Objects.requireNonNull(RegistryAccessor.get(BuiltInRegistries.BLOCK, ResourceLocation.parse(impl.replaceDefaultFluidBlock)));
                Objects.requireNonNull(RegistryAccessor.get(BuiltInRegistries.BLOCK, ResourceLocation.parse(impl.replaceUndergroundLavaBlock)));
            } catch (NullPointerException e) {
                // 方块 ID 无效，使用默认值
                ModMetadata.LOGGER.error("[Ultimate Scaler] Failed to load block, resetting to default values: {}", e.getMessage());
                impl.replaceDefaultFluidBlock = "minecraft:air";
                impl.replaceUndergroundLavaBlock = "minecraft:air";
            }
        }
    }

    /**
     * 将 {@link #impl} 实例中的配置选项的值写入配置文件。若配置文件不存在，则会自动创建。
     *
     * @throws IOException 配置文件读取或写入时出错
     * @see ConfigManager
     */
    public static void saveConfig() throws IOException {
        if (!CONFIG_PATH.toFile().exists()) {
            if (impl == null) {
                loadConfig();
            }
            Files.createFile(CONFIG_PATH);
            ModMetadata.LOGGER.info("[Ultimate Scaler] Created new config file at {}", CONFIG_PATH);
        }
        new TomlWriter().write(impl, CONFIG_PATH.toFile());
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