package ru.turtlebot.util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.turtlebot.config.ConfigManager;
import java.util.HashMap;
import java.util.Map;
public final class BotLog {
    private static final Logger LOG = LoggerFactory.getLogger("TurtleBot");
    private static final Map<String, Long> LAST = new HashMap<>();
    private BotLog() {}
    public static void debug(String m) { LOG.debug(m); }
    public static void info(String m) { LOG.info(m); }
    public static void say(String m) { LOG.info(m); chat("§a" + m, false); }
    public static void warn(String m) { LOG.warn(m); chat("§e" + m, false); }
    public static void error(String m) { LOG.error(m); chat("§c" + m, false); }
    public static void error(String m, Throwable t) { LOG.error(m, t); chat("§c" + m, false); }
    public static void reply(String m) { chat(m, true); }
    public static void infoThrottled(String key, String m, long ms) { if (allow(key, ms)) info(m); }
    public static void warnThrottled(String key, String m, long ms) { if (allow(key, ms)) warn(m); }
    private static boolean allow(String key, long ms) {
        long now = System.currentTimeMillis();
        Long last = LAST.get(key);
        if (last != null && now - last < ms) return false;
        LAST.put(key, now);
        return true;
    }
    private static void chat(String m, boolean force) {
        if (!force && !ConfigManager.get().chatMessages) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.player.sendMessage(Text.literal("§6[TurtleBot]§r " + m), false);
        });
    }
}
