package com.oresight.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

/**
 * Handles reading/writing OreSight's settings under
 * <mc instance>/config/oresight/config.json, plus named profile snapshots
 * under config/oresight/profiles/*.json.
 *
 * Kept deliberately simple (Gson + plain files) rather than pulling in a
 * config-library dependency, so the mod stays lightweight.
 */
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configDir = FabricLoader.getInstance().getConfigDir().resolve("oresight");
    private final Path mainFile = configDir.resolve("config.json");
    private final Path profilesDir = configDir.resolve("profiles");

    private ModConfig config = new ModConfig();

    public ModConfig getConfig() {
        return config;
    }

    public void load() {
        try {
            Files.createDirectories(profilesDir);
            if (Files.exists(mainFile)) {
                try (Reader r = Files.newBufferedReader(mainFile, StandardCharsets.UTF_8)) {
                    ModConfig loaded = GSON.fromJson(r, ModConfig.class);
                    if (loaded != null) {
                        config = loaded;
                    }
                }
            } else {
                config.blocks = Categories.buildDefaultBlockMap();
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (config.blocks == null || config.blocks.isEmpty()) {
            config.blocks = Categories.buildDefaultBlockMap();
        }
    }

    public void save() {
        try {
            Files.createDirectories(configDir);
            try (Writer w = Files.newBufferedWriter(mainFile, StandardCharsets.UTF_8)) {
                GSON.toJson(config, w);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetToDefaults() {
        config = new ModConfig();
        config.blocks = Categories.buildDefaultBlockMap();
        save();
    }

    public void saveProfile(String name) {
        try {
            Files.createDirectories(profilesDir);
            Path p = profilesDir.resolve(sanitize(name) + ".json");
            try (Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
                GSON.toJson(config, w);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** @return true if a profile with that name existed and was loaded. */
    public boolean loadProfile(String name) {
        Path p = profilesDir.resolve(sanitize(name) + ".json");
        if (!Files.exists(p)) {
            return false;
        }
        try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            ModConfig loaded = GSON.fromJson(r, ModConfig.class);
            if (loaded != null) {
                config = loaded;
                config.activeProfile = name;
                save();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> listProfiles() {
        List<String> names = new ArrayList<>();
        try {
            if (Files.exists(profilesDir)) {
                try (Stream<Path> stream = Files.list(profilesDir)) {
                    stream.filter(p -> p.toString().endsWith(".json"))
                            .forEach(p -> {
                                String fn = p.getFileName().toString();
                                names.add(fn.substring(0, fn.length() - ".json".length()));
                            });
                }
            }
        } catch (IOException ignored) {
            // no profiles yet, that's fine
        }
        return names;
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
