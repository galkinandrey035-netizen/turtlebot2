package ru.turtlebot.action;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ru.turtlebot.config.BotConfig;
import ru.turtlebot.config.ConfigManager;
import ru.turtlebot.util.BotLog;
import ru.turtlebot.util.Geo;

import java.util.ArrayDeque;

public final class ChestHandler {
    private int t, attempts, phase, invBefore;
    private final ArrayDeque<Integer> queue = new ArrayDeque<>();

    public int taken;
    public boolean drained;
    public int remaining;

    public void begin() { next(); taken = 0; drained = false; remaining = 0; attempts = 0; }

    public void next() { t = 0; phase = 0; queue.clear(); }

    private static boolean isOpen(ClientPlayerEntity p) {
        return p.currentScreenHandler instanceof GenericContainerScreenHandler;
    }

    public StepResult open(MinecraftClient mc, BlockPos chest) {
        ClientPlayerEntity p = mc.player;
        if (isOpen(p)) return StepResult.DONE;
        if (t % 25 == 0) {
            if (attempts >= 3) return StepResult.FAILED;
            attempts++;
            Vec3d c = Vec3d.ofCenter(chest);
            Geo.lookAt(p, c);
            Vec3d d = p.getEyePos().subtract(c);
            Direction face = Direction.getFacing(d.x, d.y, d.z);
            Vec3d hit = c.add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
            mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hit, face, chest, false));
            BotLog.debug("Открытие сундука, попытка " + attempts);
        }
        t++;
        return StepResult.RUNNING;
    }

    public StepResult take(MinecraftClient mc) {
        BotConfig cfg = ConfigManager.get();
        ClientPlayerEntity p = mc.player;
        if (!(p.currentScreenHandler instanceof GenericContainerScreenHandler h)) return StepResult.FAILED;
        int cSlots = h.getRows() * 9;
        t++;
        switch (phase) {
            case 0 -> {
                if (t < cfg.chestOpenWaitTicks) return StepResult.RUNNING;
                invBefore = InventoryUtil.countSeagrass(p);
                for (int i = 0; i < cSlots; i++)
                    if (h.getSlot(i).getStack().isOf(Items.SEAGRASS)) queue.add(i);
                if (queue.isEmpty()) {
                    taken = 0; drained = true; remaining = 0;
                    return StepResult.DONE;
                }
                phase = 1; t = 0;
            }
            case 1 -> {
                if (t >= cfg.chestClickDelayTicks) {
                    t = 0;
                    Integer slot = queue.poll();
                    if (slot != null) {
                        mc.interactionManager.clickSlot(h.syncId, slot, 0, SlotActionType.QUICK_MOVE, p);
                    }
                    if (queue.isEmpty()) { phase = 2; t = 0; }
                }
            }
            case 2 -> {
                if (t < 10) return StepResult.RUNNING;
                remaining = 0;
                for (int i = 0; i < cSlots; i++) {
                    var s = h.getSlot(i).getStack();
                    if (s.isOf(Items.SEAGRASS)) remaining += s.getCount();
                }
                taken = InventoryUtil.countSeagrass(p) - invBefore;
                drained = remaining == 0;
                if (remaining > 0) {
                    BotLog.warn("В сундуке осталась трава (" + remaining + ") — возможно, нет места в инвентаре");
                }
                return StepResult.DONE;
            }
            default -> { }
        }
        return StepResult.RUNNING;
    }

    public StepResult close(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (t == 0 && isOpen(p)) p.closeHandledScreen();
        t++;
        if (!isOpen(p) && mc.currentScreen == null) return StepResult.DONE;
        if (t > 20) {
            BotLog.warn("Сундук не закрылся штатно, закрываю принудительно");
            mc.setScreen(null);
            return StepResult.DONE;
        }
        return StepResult.RUNNING;
    }
}
