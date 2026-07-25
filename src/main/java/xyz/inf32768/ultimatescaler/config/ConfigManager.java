package xyz.inf32768.ultimatescaler.config;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import xyz.inf32768.ultimatescaler.ModMetadata;
import xyz.inf32768.ultimatescaler.versionutil.RegistryAccessor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConfigManager {
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("ultimate_scaler.toml").toFile();
    /**
     * 配置实例，用于存储配置选项的值，运行时所有的配置选项的值都通过这个实例来访问和修改。
     */
    public static Config config;

    static {
        loadConfig();
    }

    /**
     * 读取配置文件，并将读取到的配置选项的值赋值给 {@link ConfigManager#config} 实例。期间会检查并迁移旧版配置文件，并自动替换无效值。
     */
    public static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            // 默认配置文件不存在
            if (Files.exists(FabricLoader.getInstance().getConfigDir().resolve("ultimatescaler.toml"))) {
                // 存在旧版配置文件，迁移配置
                config = new Toml().read(FabricLoader.getInstance().getConfigDir().resolve("ultimatescaler.toml").toFile()).to(Config.class);
                try {
                    Files.deleteIfExists(FabricLoader.getInstance().getConfigDir().resolve("ultimatescaler.toml"));
                } catch (IOException ignored) {
                } // 文件删不掉，不管它
            }
            // 不存在任何配置文件，使用默认配置
            config = new Config();
        } else {
            try {
                // 存在配置文件，尝试读取配置
                config = new Toml().read(CONFIG_FILE).to(Config.class);
            } catch (Exception e) {
                // 配置文件格式错误，由于不知道是哪里出错，因此只打印错误信息，并使用默认配置
                ModMetadata.LOGGER.error("Failed to load config file, resetting to default values", e);
                config = new Config();
                try {
                    saveConfig();
                } catch (IOException ex) {
                    ModMetadata.LOGGER.error("Failed to save config file", ex);
                }
                return;
            }
            // 执行到这，说明配置文件格式正确，接下来校验字段合法性
            try {
                for (Field entry : Config.class.getFields()) {
                    if (entry.get(config) == null) {
                        // 字段值为空，使用默认值
                        ModMetadata.LOGGER.warn("Failed to load config entry, resetting to default value: {}", entry.getName());
                        entry.set(config, Config.class.getField(entry.getName()).get(new Config()));
                    }
                }
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
            try {
                // 确保配置文件中的方块 ID 有效
                Objects.requireNonNull(RegistryAccessor.get(BuiltInRegistries.BLOCK, ResourceLocation.parse(config.worldGen.fluidReplace.replaceDefaultFluidBlock)));
                Objects.requireNonNull(RegistryAccessor.get(BuiltInRegistries.BLOCK, ResourceLocation.parse(config.worldGen.fluidReplace.replaceUndergroundLavaBlock)));
            } catch (NullPointerException e) {
                // 方块 ID 无效，使用默认值
                ModMetadata.LOGGER.error("Failed to load block, resetting to default values: {}", e.getMessage());
                config.worldGen.fluidReplace.replaceDefaultFluidBlock = "minecraft:air";
                config.worldGen.fluidReplace.replaceUndergroundLavaBlock = "minecraft:air";
            }
        }
    }

    /**
     * 将 {@link ConfigManager#config} 实例中的配置选项的值写入配置文件。若配置文件不存在，则会自动创建。
     *
     * @throws IOException 配置文件读取或写入时出错
     */
    public static void saveConfig() throws IOException {
        if (!CONFIG_FILE.exists()) {
            Files.createFile(CONFIG_FILE.toPath());
            ModMetadata.LOGGER.info("Created new config file at {}", CONFIG_FILE.getAbsolutePath());
        }
        new TomlWriter().write(Objects.requireNonNull(config), CONFIG_FILE);

        // 在文件开头添加 Wiki 引导
        List<String> lines = Files.readAllLines(CONFIG_FILE.toPath());
        List<String> newLines = new ArrayList<>();
        newLines.add("# Check out our GitHub Wiki for detailed explanations of all the configuration options:\n# https://github.com/INF32768/UltimateScaler/wiki/UserGuide.Configuration.en\n\n");
        newLines.addAll(lines);
        Files.write(CONFIG_FILE.toPath(), newLines);
    }
}
