package ru.turtlebot.data;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.turtlebot.config.BotConfig;
import ru.turtlebot.config.ConfigManager;
import ru.turtlebot.util.BotLog;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
public final class TurtleRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final class Data { Map<String, TurtleRecord> turtles = new LinkedHashMap<>(); }
    private Data data = new Data();
    private boolean dirty;
    private Path file() { return ConfigManager.DIR.resolve("turtles.json"); }
    public synchronized void load() {
        Path f = file();
        try {
            Files.createDirectories(ConfigManager.DIR);
            if (!Files.exists(f)) { data = new Data(); return; }
            Data d = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Data.class);
            if (d == null || d.turtles == null) throw new IllegalStateException("пустые данные");
            data = d;
            prune();
            BotLog.info("Загружено черепах из базы: " + data.turtles.size());
        } catch (Exception e) {
            BotLog.error("База черепах повреждена (" + e.getMessage() + "), создана новая");
            try {
                Files.move(f, ConfigManager.DIR.resolve("turtles.json.broken-" + System.currentTimeMillis()),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) { }
            data = new Data();
        }
    }
    public synchronized void save() {
        try {
            Files.createDirectories(ConfigManager.DIR);
            Path tmp = ConfigManager.DIR.resolve("turtles.json.tmp");
            Files.writeString(tmp, GSON.toJson(data), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file(), StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (Exception e) {
            BotLog.error("Не удалось сохранить базу черепах: " + e.getMessage());
        }
    }
    public synchronized void saveIfDirty() { if (dirty) save(); }
    public synchronized void clear() { data = new Data(); save(); }
    public synchronized void seen(UUID id, boolean baby, long now) {
        TurtleRecord r = data.turtles.get(id.toString());
        if (r == null) {
            r = new TurtleRecord();
            r.uuid = id.toString();
            r.firstSeen = now;
            data.turtles.put(r.uuid, r);
            dirty = true;
        }
        if (r.baby != baby) { r.baby = baby; dirty = true; }
        r.lastSeen = now;
    }
    public synchronized boolean canFeed(UUID id, long now) {
        TurtleRecord r = data.turtles.get(id.toString());
        if (r == null) return true;
        return !r.baby && now >= r.nextFeedAt && now >= r.retryAfter;
    }
    public synchronized long eligibleAt(UUID id) {
        TurtleRecord r = data.turtles.get(id.toString());
        return r == null ? 0 : Math.max(r.nextFeedAt, r.retryAfter);
    }
    public synchronized void markFed(UUID id, long now) {
        TurtleRecord r = data.turtles.get(id.toString());
        if (r == null) { seen(id, false, now); r = data.turtles.get(id.toString()); }
        r.lastFed = now;
        r.nextFeedAt = now + ConfigManager.get().feedCooldownSeconds * 1000L;
        r.retryAfter = 0;
        r.failCount = 0;
        r.feedCount++;
        dirty = true;
        save();
    }
    public synchronized void markFail(UUID id, long now) {
        TurtleRecord r = data.turtles.get(id.toString());
        if (r == null) { seen(id, false, now); r = data.turtles.get(id.toString()); }
        r.failCount++;
        r.retryAfter = now + ConfigManager.get().failBackoffSeconds * 1000L;
        dirty = true;
    }
    public synchronized int size() { return data.turtles.size(); }
    public synchronized int readyCount(long now) {
        int n = 0;
        for (TurtleRecord r : data.turtles.values())
            if (!r.baby && now >= r.nextFeedAt && now >= r.retryAfter) n++;
        return n;
    }
    private void prune() {
        BotConfig c = ConfigManager.get();
        long limit = System.currentTimeMillis() - c.turtleForgetDays * 86_400_000L;
        if (data.turtles.values().removeIf(r -> r.lastSeen > 0 && r.lastSeen < limit)) dirty = true;
    }
}
