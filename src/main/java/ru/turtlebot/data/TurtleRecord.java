package ru.turtlebot.data;
public final class TurtleRecord {
    public String uuid = "";
    public boolean baby = false;
    public long firstSeen = 0;
    public long lastSeen = 0;
    public long lastFed = 0;
    public long nextFeedAt = 0;
    public long retryAfter = 0;
    public int feedCount = 0;
    public int failCount = 0;
}
