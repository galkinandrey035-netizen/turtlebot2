package ru.turtlebot.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import ru.turtlebot.config.BotConfig;
import ru.turtlebot.config.ConfigManager;
import ru.turtlebot.data.TurtleRegistry;
import ru.turtlebot.state.BotController;
import ru.turtlebot.util.BotLog;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class BotCommands {
    private BotCommands() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> d, BotController bot, TurtleRegistry reg) {
        d.register(build("turtlebot", bot, reg));
        d.register(build("tb", bot, reg));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> build(String name, BotController bot, TurtleRegistry reg) {
        return literal(name)
                .executes(c -> { help(); return 1; })
                .then(literal("start").executes(c -> {
                    String err = bot.start();
                    if (err != null) BotLog.reply("§c" + err);
                    return 1;
                }))
                .then(literal("stop").executes(c -> { bot.stop("остановлено командой"); BotLog.reply("Бот остановлен"); return 1; }))
                .then(literal("pause").executes(c -> { bot.pause(); return 1; }))
                .then(literal("resume").executes(c -> { bot.resume(); return 1; }))
                .then(literal("status").executes(c -> { bot.status().forEach(BotLog::reply); return 1; }))
                .then(literal("reload").executes(c -> {
                    ConfigManager.load();
                    reg.load();
                    BotLog.reply("Конфиг и база черепах перезагружены");
                    return 1;
                }))
                .then(literal("clear").executes(c -> {
                    reg.clear();
                    BotLog.reply("База черепах очищена");
                    return 1;
                }))
                .then(literal("chest")
                        .then(literal("set")
                                .executes(BotCommands::setChestLook)
                                .then(argument("x", IntegerArgumentType.integer())
                                        .then(argument("y", IntegerArgumentType.integer())
                                                .then(argument("z", IntegerArgumentType.integer())
                                                        .executes(c -> setChest(MinecraftClient.getInstance(),
                                                                new BlockPos(IntegerArgumentType.getInteger(c, "x"),
                                                                        IntegerArgumentType.getInteger(c, "y"),
                                                                        IntegerArgumentType.getInteger(c, "z"))))))))
                        .then(literal("show").executes(c -> {
                            BotConfig cfg = ConfigManager.get();
                            BotLog.reply(cfg.chestSet
                                    ? "Сундук: " + cfg.chestX + " " + cfg.chestY + " " + cfg.chestZ + " (" + cfg.chestDimension + ")"
                                    : "Сундук не задан");
                            return 1;
                        })))
                .then(literal("radius")
                        .executes(c -> { BotLog.reply("Радиус поиска: " + ConfigManager.get().radius); return 1; })
                        .then(argument("n", IntegerArgumentType.integer(4, 64)).executes(c -> {
                            ConfigManager.get().radius = IntegerArgumentType.getInteger(c, "n");
                            ConfigManager.save();
                            BotLog.reply("Радиус поиска: " + ConfigManager.get().radius);
                            return 1;
                        })));
    }

    private static void help() {
        BotLog.reply("§6/turtlebot§r start | stop | pause | resume | status | reload | clear");
        BotLog.reply("§6/turtlebot§r chest set [x y z] | chest show | radius [4-64]");
    }

    private static int setChestLook(CommandContext<FabricClientCommandSource> c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            return setChest(mc, hit.getBlockPos());
        }
        BotLog.reply("§cНаведите прицел на сундук или укажите координаты: chest set x y z");
        return 0;
    }

    private static int setChest(MinecraftClient mc, BlockPos pos) {
        if (mc.world == null) return 0;
        var b = mc.world.getBlockState(pos).getBlock();
        if (!(b instanceof ChestBlock || b instanceof BarrelBlock)) {
            BotLog.reply("§cПо координатам " + pos.toShortString() + " нет сундука или бочки");
            return 0;
        }
        BotConfig cfg = ConfigManager.get();
        cfg.chestSet = true;
        cfg.chestX = pos.getX(); cfg.chestY = pos.getY(); cfg.chestZ = pos.getZ();
        cfg.chestDimension = mc.world.getRegistryKey().getValue().toString();
        ConfigManager.save();
        BotLog.reply("Сундук задан: " + pos.toShortString() + " (" + cfg.chestDimension + ")");
        return 1;
    }
}
