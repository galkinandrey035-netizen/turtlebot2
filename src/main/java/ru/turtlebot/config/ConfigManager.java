package ru.turtlebot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import ru.turtlebot.util.BotLog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class ConfigManager {
    public static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("turtlebot");
    private static final Path FILE = DIR.resolve("config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static BotConfig cfg = new BotConfig();
    private ConfigManager() {}
    public static BotConfig get() { return cfg; }
    public static synchronized void load() {
        try {
            Files.createDirectories(DIR);
            if (!Files.exists(FILE)) {
                cfg = new BotConfig();
                save();
                BotLog.info("Создан конфиг по умолчанию: " + FILE);
                return;
            }
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            BotConfig loaded = GSON.fromJson(json, BotConfig.class);
            if (loaded == null) throw new JsonParseException("пустой файл");
            loaded.sanitize();
            cfg = loaded;
            save();
            BotLog.info("Конфиг загружен");
        } catch (Exception e) {
            BotLog.error("Конфиг повреждён (" + e.getMessage() + "), загружены значения по умолчанию");
            try {
                Files.move(FILE, DIR.resolve("config.json.broken-" + System.currentTimeMillis()),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) { }
            cfg = new BotConfig();
            save();
        }
    }
    public static synchronized void save() {
        try {
            Files.createDirectories(DIR);
            Path tmp = DIR.resolve("config.json.tmp");
            Files.writeString(tmp, GSON.toJson(cfg), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            BotLog.error("Не удалось сохранить конфиг: " + e.getMessage());
        }
    }
}
