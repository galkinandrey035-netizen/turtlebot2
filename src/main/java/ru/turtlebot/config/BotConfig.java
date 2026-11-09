package ru.turtlebot.config;

public final class BotConfig {
    public String homeCommand = "home";
    public boolean chestSet = false;
    public String chestDimension = "";
    public int chestX = 0, chestY = 0, chestZ = 0;
    public int radius = 24;
    public int feedCooldownSeconds = 310;
    public int failBackoffSeconds = 90;
    public int feedAttemptsLimit = 3;
    public double feedReach = 2.8;
    public double chestReach = 3.5;
    public int feedConfirmTicks = 15;
    public int chestClickDelayTicks = 3;
    public int chestOpenWaitTicks = 6;
    public int afterTeleportDelayTicks = 30;
    public int teleportTimeoutSeconds = 20;
    public int pathToChestTimeoutSeconds = 90;
    public int openChestTimeoutSeconds = 8;
    public int takeGrassTimeoutSeconds = 15;
    public int closeChestTimeoutSeconds = 5;
    public int approachTimeoutSeconds = 45;
    public int feedTimeoutSeconds = 20;
    public int maxWaitCooldownSeconds = 900;
    public int stageRetries = 2;
    public int maxPathNodes = 6000;
    public int maxPathRange = 64;
    public int maxDrop = 1;
    public int eggMargin = 1;
    public int maxReplans = 6;
    public double minHealth = 6.0;
    public boolean chatMessages = true;
    public int turtleForgetDays = 7;

    public void sanitize() {
        if (homeCommand == null || homeCommand.isBlank()) homeCommand = "home";
        if (chestDimension == null) chestDimension = "";
        radius = clamp(radius, 4, 64);
        feedCooldownSeconds = clamp(feedCooldownSeconds, 10, 3600);
        failBackoffSeconds = clamp(failBackoffSeconds, 5, 3600);
        feedAttemptsLimit = clamp(feedAttemptsLimit, 1, 10);
        feedReach = clamp(feedReach, 1.0, 4.0);
        chestReach = clamp(chestReach, 1.5, 4.5);
        feedConfirmTicks = clamp(feedConfirmTicks, 5, 60);
        chestClickDelayTicks = clamp(chestClickDelayTicks, 1, 20);
        chestOpenWaitTicks = clamp(chestOpenWaitTicks, 2, 40);
        afterTeleportDelayTicks = clamp(afterTeleportDelayTicks, 0, 200);
        teleportTimeoutSeconds = clamp(teleportTimeoutSeconds, 3, 120);
        pathToChestTimeoutSeconds = clamp(pathToChestTimeoutSeconds, 10, 600);
        openChestTimeoutSeconds = clamp(openChestTimeoutSeconds, 3, 60);
        takeGrassTimeoutSeconds = clamp(takeGrassTimeoutSeconds, 5, 120);
        closeChestTimeoutSeconds = clamp(closeChestTimeoutSeconds, 2, 60);
        approachTimeoutSeconds = clamp(approachTimeoutSeconds, 10, 300);
        feedTimeoutSeconds = clamp(feedTimeoutSeconds, 5, 120);
        maxWaitCooldownSeconds = clamp(maxWaitCooldownSeconds, 30, 7200);
        stageRetries = clamp(stageRetries, 0, 10);
        maxPathNodes = clamp(maxPathNodes, 500, 50000);
        maxPathRange = clamp(maxPathRange, 8, 128);
        maxDrop = clamp(maxDrop, 0, 3);
        eggMargin = clamp(eggMargin, 0, 3);
        maxReplans = clamp(maxReplans, 1, 30);
        minHealth = clamp(minHealth, 0.0, 19.0);
        turtleForgetDays = clamp(turtleForgetDays, 1, 365);
    }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v)) return lo;
        return Math.max(lo, Math.min(hi, v));
    }
}
