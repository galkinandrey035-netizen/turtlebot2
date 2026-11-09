package ru.turtlebot.action;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import ru.turtlebot.config.BotConfig;
import ru.turtlebot.config.ConfigManager;
import ru.turtlebot.util.BotLog;
import ru.turtlebot.util.Geo;

public final class FeedHandler {
    private TurtleEntity turtle;
    private int phase, t, attempts, before;
    public boolean outOfGrass;

    public void begin(TurtleEntity target) {
        turtle = target; phase = 0; t = 0; attempts = 0; outOfGrass = false;
    }

    public StepResult tick(MinecraftClient mc) {
        BotConfig cfg = ConfigManager.get();
        ClientPlayerEntity p = mc.player;
        if (turtle == null || !turtle.isAlive() || turtle.isRemoved()) return StepResult.FAILED;
        if (turtle.isBaby()) {
            BotLog.warn("Цель оказалась детёнышем — кормление отменено");
            return StepResult.FAILED;
        }
        if (Geo.distToBox(p.getEyePos(), turtle.getBoundingBox()) > cfg.feedReach + 0.6) {
            return StepResult.NEED_APPROACH;
        }
        t++;
        switch (phase) {
            case 0 -> {
                if (!InventoryUtil.ensureSeagrassInHand(mc)) { outOfGrass = true; return StepResult.FAILED; }
                if (p.getMainHandStack().isOf(Items.SEAGRASS)) { phase = 1; t = 0; }
                else if (t > 20) return StepResult.FAILED;
            }
            case 1 -> {
                Geo.lookAt(p, turtle.getPos().add(0, turtle.getHeight() / 2.0, 0));
                before = p.getMainHandStack().getCount();
                mc.interactionManager.interactEntity(p, turtle, Hand.MAIN_HAND);
                phase = 2; t = 0;
            }
            case 2 -> {
                ItemStack s = p.getMainHandStack();
                int now = s.isOf(Items.SEAGRASS) ? s.getCount() : 0;
                if (now < before) return StepResult.DONE;
                if (t >= cfg.feedConfirmTicks) {
                    attempts++;
                    if (attempts >= cfg.feedAttemptsLimit) return StepResult.FAILED;
                    BotLog.debug("Кормление не подтверждено, повтор " + (attempts + 1) + "/" + cfg.feedAttemptsLimit);
                    phase = 1; t = 0;
                }
            }
            default -> { }
        }
        return StepResult.RUNNING;
    }
}
