package ru.turtlebot.nav;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.turtlebot.config.BotConfig;
import ru.turtlebot.config.ConfigManager;
import ru.turtlebot.util.BotLog;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class Navigator {
    public enum Status { MOVING, ARRIVED, FAILED }

    private final PathFollower follower = new PathFollower();
    private Predicate<Vec3d> eyeOk = v -> true;
    private Supplier<Vec3d> targetSup = () -> Vec3d.ZERO;
    private double hReach;
    private int replans;
    private long lastPlan = -100;
    private Vec3d plannedTarget;

    public void setGoal(Predicate<Vec3d> eyeOk, Supplier<Vec3d> target, double hReach) {
        this.eyeOk = eyeOk;
        this.targetSup = target;
        this.hReach = hReach;
        replans = 0;
        lastPlan = -100;
        plannedTarget = null;
        follower.clear();
    }

    public void cancel(MinecraftClient mc) {
        follower.clear();
        Keys.release(mc);
    }

    public Status tick(MinecraftClient mc, long now) {
        BotConfig cfg = ConfigManager.get();
        ClientPlayerEntity p = mc.player;

        if (eyeOk.test(p.getEyePos())) {
            cancel(mc);
            return Status.ARRIVED;
        }
        if (replans > cfg.maxReplans) { cancel(mc); return Status.FAILED; }

        Vec3d tgt = targetSup.get();
        boolean need = !follower.hasPath()
                || (plannedTarget != null && tgt.squaredDistanceTo(plannedTarget) > 4.0);
        if (need) {
            if (now - lastPlan < 10) { Keys.release(mc); return Status.MOVING; }
            lastPlan = now;
            Walkability w = new Walkability(mc.world, cfg);
            List<BlockPos> path = AStar.search(p.getBlockPos(), p.getEyePos(), eyeOk, tgt, hReach, w, cfg);
            if (path == null) {
                replans++;
                follower.clear();
                Keys.release(mc);
                BotLog.warnThrottled("nopath", "Путь не найден (попытка " + replans + "/" + cfg.maxReplans + ")", 10_000);
                return Status.MOVING;
            }
            follower.set(path);
            plannedTarget = tgt;
        }

        switch (follower.tick(mc)) {
            case RUNNING -> { }
            case FINISHED -> follower.clear();
            case STUCK, BLOCKED -> {
                follower.clear();
                replans++;
                BotLog.debug("Перепланирование маршрута (" + replans + ")");
            }
        }
        return Status.MOVING;
    }
}
