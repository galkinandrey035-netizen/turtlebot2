package ru.turtlebot.nav;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

public final class Keys {
    private Keys() {}

    public static void walk(MinecraftClient mc, boolean forward) {
        GameOptions o = mc.options;
        o.forwardKey.setPressed(forward);
        o.backKey.setPressed(false);
        o.leftKey.setPressed(false);
        o.rightKey.setPressed(false);
        o.jumpKey.setPressed(false);
        o.sprintKey.setPressed(false);
        if (mc.player != null) mc.player.setSprinting(false);
    }

    public static void release(MinecraftClient mc) {
        GameOptions o = mc.options;
        o.forwardKey.setPressed(false);
        o.backKey.setPressed(false);
        o.leftKey.setPressed(false);
        o.rightKey.setPressed(false);
        o.jumpKey.setPressed(false);
        o.sprintKey.setPressed(false);
    }
}
