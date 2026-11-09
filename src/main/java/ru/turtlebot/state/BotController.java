package ru.turtlebot.state;

import net.minecraft.block.BarrelBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.turtlebot.action.*;
import ru.turtlebot.config.BotConfig;
import ru.turtlebot.config.ConfigManager;
import ru.turtlebot.data.TurtleRegistry;
import ru.turtlebot.nav.Navigator;
import ru.turtlebot.util.BotLog;
import ru.turtlebot.util.Geo;

import java.util.ArrayList;
import java.util.List;

public final class BotController {
    private final TurtleRegistry registry;
    private final Navigator nav = new Navigator();
    private final ChestHandler chest = new ChestHandler();
    private final FeedHandler feed = new FeedHandler();

    private BotState state = BotState.IDLE;
    private boolean paused;
    private long tickNow, enterTick, stageTick = -1;
    private int retries, totalFed, restocks;
    private boolean chestDrained;
    private TurtleEntity target;
    private long waitStartMs;
    private Vec3d homeStartPos = Vec3d.ZERO;
    private Object homeStartPlayer;
    private int adults, babies, ready;

    public BotController(TurtleRegistry registry) { this.registry = registry; }

    private static BotConfig cfg() { return ConfigManager.get(); }
    private static BlockPos chestPos() { return new BlockPos(cfg().chestX, cfg().chestY, cfg().chestZ); }

    public boolean isActive() {
        return state != BotState.IDLE && state != BotState.FINISHED && state != BotState.ERROR;
    }

    public String start() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (isActive()) return "Бот уже запущен";
        if (mc.player == null || mc.world == null) return "Вы не в мире";
        if (!cfg().chestSet) return "Сундук не задан. Используйте /turtlebot chest set";
        retries = 0; totalFed = 0; restocks = 0;
        chestDrained = false; paused = false; target = null; waitStartMs = 0;
        BotLog.say("Бот запущен");
        setState(BotState.TELEPORT_HOME);
        return null;
    }

    public void stop(String reason) {
        boolean was = isActive() || paused;
        cleanup();
        state = BotState.IDLE;
        paused = false;
        registry.save();
        if (was) BotLog.say("Бот остановлен: " + reason);
    }

    public void pause() {
        if (!isActive() || paused) return;
        paused = true;
        nav.cancel(MinecraftClient.getInstance());
        BotLog.say("Пауза");
    }

    public void resume() {
        if (!isActive() || !paused) return;
        paused = false;
        BotLog.say("Работа продолжена");
    }

    private void finish(String msg) {
        cleanup();
        state = BotState.FINISHED;
        registry.save();
        BotLog.say("Работа завершена: " + msg + ". Покормлено за сессию: " + totalFed);
    }

    private void fail(String msg) {
        cleanup();
        state = BotState.ERROR;
        registry.save();
        BotLog.error("Остановка из-за ошибки: " + msg);
    }

    private void cleanup() {
        MinecraftClient mc = MinecraftClient.getInstance();
        nav.cancel(mc);
        if (mc.player != null && mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
            mc.player.closeHandledScreen();
        }
    }

    public List<String> status() {
        List<String> l = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        l.add("Состояние: " + state.title + (paused ? " (на паузе)" : ""));
        if (isActive()) l.add("В состоянии: " + Math.max(0, (tickNow - enterTick) / 20) + " с");
        if (mc.player != null) l.add("Трава в инвентаре: " + InventoryUtil.countSeagrass(mc.player));
        l.add(cfg().chestSet
                ? "Сундук: " + cfg().chestX + " " + cfg().chestY + " " + cfg().chestZ + " (" + cfg().chestDimension + ")"
                : "Сундук: не задан");
        l.add("Радиус поиска: " + cfg().radius);
        l.add("Черепах в базе: " + registry.size() + ", готовы к кормлению: " + registry.readyCount(System.currentTimeMillis()));
        l.add("Покормлено за сессию: " + totalFed + ", походов за травой: " + restocks);
        return l;
    }

    private void setState(BotState next) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean loop = next == BotState.SCAN_TURTLES || next == BotState.APPROACH_TURTLE
                || next == BotState.FEED_TURTLE || next == BotState.WAIT_COOLDOWN;
        String msg = "Состояние: " + state.title + " → " + next.title;
        if (loop) BotLog.debug(msg); else BotLog.info(msg);

        state = next;
        enterTick = tickNow;
        stageTick = -1;
        switch (next) {
            case PATH_TO_CHEST -> {
                Vec3d c = Vec3d.ofCenter(chestPos());
                double r = cfg().chestReach;
                nav.setGoal(eye -> eye.squaredDistanceTo(c) <= r * r, () -> c, r);
            }
            case OPEN_CHEST -> { nav.cancel(mc); chest.begin(); }
            case TAKE_GRASS, CLOSE_CHEST -> { nav.cancel(mc); chest.next(); }
            case APPROACH_TURTLE -> setTurtleGoal();
            default -> nav.cancel(mc);
        }
    }

    private void setTurtleGoal() {
        final TurtleEntity t = target;
        final double reach = cfg().feedReach;
        nav.setGoal(eye -> Geo.distToBox(eye, t.getBoundingBox()) <= reach,
                () -> t.getPos().add(0, t.getHeight() / 2.0, 0), reach + 0.4);
    }

    private boolean timedOut(int seconds) { return tickNow - enterTick > seconds * 20L; }

    private void stageFail(String what) {
        retries++;
        if (retries > cfg().stageRetries) { fail(what + " (попытки исчерпаны)"); return; }
        BotLog.warn("Сбой этапа: " + what + ". Повтор " + retries + "/" + cfg().stageRetries);
        cleanup();
        setState(BotState.TELEPORT_HOME);
    }

    private void turtleFail(String why) {
        if (target != null) {
            registry.markFail(target.getUuid(), System.currentTimeMillis());
            BotLog.warnThrottled("tf" + target.getUuid(),
                    "Черепаха " + shortId(target) + " пропущена: " + why, 15_000);
        }
        target = null;
        setState(BotState.SCAN_TURTLES);
    }

    private static String shortId(TurtleEntity t) { return t.getUuid().toString().substring(0, 8); }

    private boolean dimensionOk(MinecraftClient mc) {
        String d = cfg().chestDimension;
        return d.isEmpty() || d.equals(mc.world.getRegistryKey().getValue().toString());
    }

    private boolean chestLoaded(MinecraftClient mc) {
        BlockPos c = chestPos();
        return mc.world.getChunkManager().isChunkLoaded(c.getX() >> 4, c.getZ() >> 4);
    }

    public void tick(MinecraftClient mc) {
        tickNow++;
        if (tickNow % 600 == 0) registry.saveIfDirty();
        if (!isActive()) return;
        if (mc.player == null || mc.world == null) { stop("потеряно соединение с миром"); return; }
        if (paused) { enterTick++; if (stageTick >= 0) stageTick++; return; }
        if (!mc.player.isAlive() || mc.player.getHealth() < cfg().minHealth) {
            fail("здоровье ниже порога (" + cfg().minHealth + ") или игрок погиб");
            return;
        }
        mc.options.attackKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        try {
            switch (state) {
                case TELEPORT_HOME -> tickTeleportHome(mc);
                case WAIT_TELEPORT -> tickWaitTeleport(mc);
                case PATH_TO_CHEST -> tickPathToChest(mc);
                case OPEN_CHEST -> tickOpen(mc);
                case TAKE_GRASS -> tickTake(mc);
                case CLOSE_CHEST -> tickClose(mc);
                case SCAN_TURTLES -> tickScan(mc);
                case APPROACH_TURTLE -> tickApproach(mc);
                case FEED_TURTLE -> tickFeed(mc);
                case WAIT_COOLDOWN -> tickWait(mc);
                default -> { }
            }
        } catch (Exception e) {
            BotLog.error("Внутренняя ошибка: " + e, e);
            fail("внутренняя ошибка (см. latest.log)");
        }
    }

    private void tickTeleportHome(MinecraftClient mc) {
        String cmd = cfg().homeCommand.trim();
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        if (mc.getNetworkHandler() == null) { fail("нет сетевого соединения"); return; }
        homeStartPos = mc.player.getPos();
        homeStartPlayer = mc.player;
        mc.getNetworkHandler().sendChatCommand(cmd);
        BotLog.info("Отправлена команда /" + cmd);
        setState(BotState.WAIT_TELEPORT);
    }

    private void tickWaitTeleport(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        boolean moved = p != homeStartPlayer || p.getPos().squaredDistanceTo(homeStartPos) > 4.0;
        if (moved && stageTick < 0) stageTick = tickNow;
        if (moved && tickNow - stageTick >= cfg().afterTeleportDelayTicks && chestLoaded(mc)) {
            BotLog.info("Телепорт выполнен");
            setState(BotState.PATH_TO_CHEST);
            return;
        }
        if (!moved && timedOut(cfg().teleportTimeoutSeconds)) {
            boolean near = dimensionOk(mc) && chestLoaded(mc)
                    && p.getPos().distanceTo(Vec3d.ofCenter(chestPos())) <= cfg().maxPathRange;
            if (near) {
                BotLog.info("Позиция не изменилась — считаю, что бот уже дома");
                setState(BotState.PATH_TO_CHEST);
            } else {
                stageFail("телепорт /" + cfg().homeCommand + " не сработал");
            }
            return;
        }
        if (timedOut(cfg().teleportTimeoutSeconds + 15)) stageFail("таймаут загрузки мира после телепорта");
    }

    private void tickPathToChest(MinecraftClient mc) {
        if (timedOut(cfg().pathToChestTimeoutSeconds)) { stageFail("таймаут пути к сундуку"); return; }
        switch (nav.tick(mc, tickNow)) {
            case ARRIVED -> setState(BotState.OPEN_CHEST);
            case FAILED -> stageFail("не найден путь к сундуку");
            default -> { }
        }
    }

    private void tickOpen(MinecraftClient mc) {
        if (stageTick < 0) {
            stageTick = tickNow;
            if (!dimensionOk(mc)) { fail("сундук задан в другом измерении (" + cfg().chestDimension + ")"); return; }
            var b = mc.world.getBlockState(chestPos()).getBlock();
            if (!(b instanceof ChestBlock || b instanceof BarrelBlock)) {
                fail("по координатам сундука нет контейнера"); return;
            }
        }
        StepResult r = chest.open(mc, chestPos());
        if (r == StepResult.DONE) setState(BotState.TAKE_GRASS);
        else if (r == StepResult.FAILED || timedOut(cfg().openChestTimeoutSeconds)) stageFail("не удалось открыть сундук");
    }

    private void tickTake(MinecraftClient mc) {
        StepResult r = chest.take(mc);
        if (r == StepResult.DONE) {
            chestDrained = chest.drained;
            BotLog.info("Взято травы: " + chest.taken + ", в инвентаре: "
                    + InventoryUtil.countSeagrass(mc.player)
                    + (chestDrained ? ". В сундуке травы больше нет" : ". В сундуке осталось: " + chest.remaining));
            setState(BotState.CLOSE_CHEST);
        } else if (r == StepResult.FAILED || timedOut(cfg().takeGrassTimeoutSeconds)) {
            stageFail("не удалось забрать траву");
        }
    }

    private void tickClose(MinecraftClient mc) {
        StepResult r = chest.close(mc);
        if (r == StepResult.DONE || timedOut(cfg().closeChestTimeoutSeconds)) {
            if (r != StepResult.DONE) mc.setScreen(null);
            retries = 0;
            waitStartMs = 0;
            restocks++;
            if (InventoryUtil.countSeagrass(mc.player) == 0) finish("сундук пуст, травы нет");
            else setState(BotState.SCAN_TURTLES);
        }
    }

    private void tickScan(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        ClientPlayerEntity p = mc.player;
        double r = cfg().radius;
        List<TurtleEntity> list = mc.world.getEntitiesByClass(TurtleEntity.class,
                p.getBoundingBox().expand(r), t -> t.isAlive() && t.distanceTo(p) <= r);

        adults = 0; babies = 0; ready = 0;
        TurtleEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (TurtleEntity t : list) {
            boolean baby = t.isBaby();
            registry.seen(t.getUuid(), baby, now);
            if (baby) { babies++; continue; }
            adults++;
            if (t.hasEgg()) continue;
            if (registry.canFeed(t.getUuid(), now)) {
                ready++;
                double d = t.squaredDistanceTo(p);
                if (d < bestD) { bestD = d; best = t; }
            }
        }
        BotLog.infoThrottled("scan", "Черепахи рядом: взрослых " + adults + ", детёнышей " + babies
                + ", готовы к кормлению " + ready, 30_000);

        int grass = InventoryUtil.countSeagrass(p);
        if (best != null) {
            if (grass > 0) { target = best; setState(BotState.APPROACH_TURTLE); return; }
            if (chestDrained) { finish("трава закончилась, сундук пуст"); return; }
            BotLog.info("Трава закончилась — возвращаюсь за новой");
            setState(BotState.TELEPORT_HOME);
            return;
        }
        if (grass == 0) {
            if (chestDrained) { finish("трава закончилась, сундук пуст"); return; }
            BotLog.info("Нет травы и некого кормить — возвращаюсь за травой");
            setState(BotState.TELEPORT_HOME);
            return;
        }
        if (waitStartMs == 0) waitStartMs = now;
        if (now - waitStartMs > cfg().maxWaitCooldownSeconds * 1000L) {
            finish("за " + cfg().maxWaitCooldownSeconds + " с не нашлось черепах для кормления");
            return;
        }
        if (adults == 0) BotLog.warnThrottled("noadult", "Взрослых черепах в радиусе " + cfg().radius + " нет, жду", 60_000);
        else BotLog.infoThrottled("waitcd", "Все взрослые черепахи на перезарядке, жду", 60_000);
        setState(BotState.WAIT_COOLDOWN);
    }

    private void tickWait(MinecraftClient mc) {
        if (tickNow - enterTick >= 100) setState(BotState.SCAN_TURTLES);
    }

    private void tickApproach(MinecraftClient mc) {
        if (target == null || !target.isAlive() || target.isRemoved() || target.isBaby()) {
            target = null;
            setState(BotState.SCAN_TURTLES);
            return;
        }
        if (timedOut(cfg().approachTimeoutSeconds)) { turtleFail("таймаут подхода"); return; }
        switch (nav.tick(mc, tickNow)) {
            case ARRIVED -> { feed.begin(target); setState(BotState.FEED_TURTLE); }
            case FAILED -> turtleFail("нет пути к черепахе");
            default -> { }
        }
    }

    private void tickFeed(MinecraftClient mc) {
        if (timedOut(cfg().feedTimeoutSeconds)) { turtleFail("таймаут кормления"); return; }
        StepResult r = feed.tick(mc);
        switch (r) {
            case NEED_APPROACH -> setState(BotState.APPROACH_TURTLE);
            case DONE -> {
                registry.markFed(target.getUuid(), System.currentTimeMillis());
                totalFed++;
                waitStartMs = 0;
                BotLog.info("Покормлена черепаха " + shortId(target) + " (всего за сессию: " + totalFed
                        + "), следующее кормление через " + cfg().feedCooldownSeconds + " с");
                target = null;
                setState(BotState.SCAN_TURTLES);
            }
            case FAILED -> {
                if (feed.outOfGrass) { target = null; setState(BotState.SCAN_TURTLES); }
                else turtleFail("не удалось покормить за " + cfg().feedAttemptsLimit + " попыток");
            }
            default -> { }
        }
    }
}
