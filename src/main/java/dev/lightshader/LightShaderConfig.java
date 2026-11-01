package dev.lightshader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LightShaderConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("lightshader.json");
    private static LightShaderConfig instance = new LightShaderConfig();

    public Preset preset = Preset.BALANCED;
    public boolean enabled = true;
    public boolean autoQuality = false;
    public int minFps = 45;

    public static LightShaderConfig get() {
        return instance;
    }

    public static void load() {
        if (Files.exists(PATH)) {
            try (Reader r = Files.newBufferedReader(PATH)) {
                LightShaderConfig loaded = GSON.fromJson(r, LightShaderConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                LightShaderClient.LOGGER.warn("Could not read {}, using defaults", PATH, e);
            }
        }
        if (instance.preset == null) {
            instance.preset = Preset.BALANCED;
        }
        instance.minFps = Math.max(10, Math.min(240, instance.minFps));
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer w = Files.newBufferedWriter(PATH)) {
                GSON.toJson(instance, w);
            }
        } catch (IOException e) {
            LightShaderClient.LOGGER.warn("Could not write {}", PATH, e);
        }
    }
}
