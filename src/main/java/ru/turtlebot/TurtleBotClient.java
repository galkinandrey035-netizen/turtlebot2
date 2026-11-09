package ru.turtlebot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.util.ActionResult;
import ru.turtlebot.command.BotCommands;
import ru.turtlebot.config.ConfigManager;
import ru.turtlebot.data.TurtleRegistry;
import ru.turtlebot.state.BotController;
import ru.turtlebot.util.BotLog;

public final class TurtleBotClient implements ClientModInitializer {
    private static BotController controller;
    private static TurtleRegistry registry;
    public static BotController controller() { return controller; }
    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        registry = new TurtleRegistry();
        registry.load();
        controller = new BotController(registry);
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, access) -> BotCommands.register(dispatcher, controller, registry));
        ClientTickEvents.END_CLIENT_TICK.register(controller::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> controller.stop("отключение от сервера"));
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> {
            controller.stop("закрытие клиента");
            registry.save();
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) ->
                world.isClient && controller.isActive() ? ActionResult.FAIL : ActionResult.PASS);
        BotLog.info("TurtleBot загружен. Команда: /turtlebot (или /tb)");
    }
}
