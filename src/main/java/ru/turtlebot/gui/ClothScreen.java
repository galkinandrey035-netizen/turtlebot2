package ru.turtlebot.gui;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.turtlebot.config.BotConfig;
import ru.turtlebot.config.ConfigManager;

final class ClothScreen {
    private ClothScreen() {}

    static Screen build(Screen parent) {
        BotConfig c = ConfigManager.get();
        ConfigBuilder b = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("TurtleBot — настройки"))
                .setSavingRunnable(() -> { c.sanitize(); ConfigManager.save(); });
        ConfigEntryBuilder e = b.entryBuilder();

        ConfigCategory g = b.getOrCreateCategory(Text.literal("Основные"));
        g.addEntry(e.startStrField(Text.literal("Команда телепорта (без /)"), c.homeCommand)
                .setDefaultValue("home").setSaveConsumer(v -> c.homeCommand = v).build());
        g.addEntry(e.startIntField(Text.literal("Радиус поиска черепах"), c.radius)
                .setDefaultValue(24).setMin(4).setMax(64).setSaveConsumer(v -> c.radius = v).build());
        g.addEntry(e.startIntField(Text.literal("Перезарядка кормления, с"), c.feedCooldownSeconds)
                .setDefaultValue(310).setMin(10).setMax(3600).setSaveConsumer(v -> c.feedCooldownSeconds = v).build());
        g.addEntry(e.startIntField(Text.literal("Попыток кормления на черепаху"), c.feedAttemptsLimit)
                .setDefaultValue(3).setMin(1).setMax(10).setSaveConsumer(v -> c.feedAttemptsLimit = v).build());
        g.addEntry(e.startBooleanToggle(Text.literal("Сообщения в чат"), c.chatMessages)
                .setDefaultValue(true).setSaveConsumer(v -> c.chatMessages = v).build());

        ConfigCategory n = b.getOrCreateCategory(Text.literal("Навигация и защита"));
        n.addEntry(e.startIntField(Text.literal("Отступ от яиц (блоков)"), c.eggMargin)
                .setDefaultValue(1).setMin(0).setMax(3).setSaveConsumer(v -> c.eggMargin = v).build());
        n.addEntry(e.startIntField(Text.literal("Макс. узлов A*"), c.maxPathNodes)
                .setDefaultValue(6000).setMin(500).setMax(50000).setSaveConsumer(v -> c.maxPathNodes = v).build());
        n.addEntry(e.startIntField(Text.literal("Макс. спуск (блоков)"), c.maxDrop)
                .setDefaultValue(1).setMin(0).setMax(3).setSaveConsumer(v -> c.maxDrop = v).build());
        n.addEntry(e.startDoubleField(Text.literal("Дистанция кормления"), c.feedReach)
                .setDefaultValue(2.8).setMin(1.0).setMax(4.0).setSaveConsumer(v -> c.feedReach = v).build());
        n.addEntry(e.startDoubleField(Text.literal("Мин. здоровье для работы"), c.minHealth)
                .setDefaultValue(6.0).setMin(0.0).setMax(19.0).setSaveConsumer(v -> c.minHealth = v).build());

        ConfigCategory t = b.getOrCreateCategory(Text.literal("Таймауты, с"));
        t.addEntry(e.startIntField(Text.literal("Телепорт"), c.teleportTimeoutSeconds)
                .setDefaultValue(20).setMin(3).setMax(120).setSaveConsumer(v -> c.teleportTimeoutSeconds = v).build());
        t.addEntry(e.startIntField(Text.literal("Путь к сундуку"), c.pathToChestTimeoutSeconds)
                .setDefaultValue(90).setMin(10).setMax(600).setSaveConsumer(v -> c.pathToChestTimeoutSeconds = v).build());
        t.addEntry(e.startIntField(Text.literal("Подход к черепахе"), c.approachTimeoutSeconds)
                .setDefaultValue(45).setMin(10).setMax(300).setSaveConsumer(v -> c.approachTimeoutSeconds = v).build());
        t.addEntry(e.startIntField(Text.literal("Макс. ожидание таймеров"), c.maxWaitCooldownSeconds)
                .setDefaultValue(900).setMin(30).setMax(7200).setSaveConsumer(v -> c.maxWaitCooldownSeconds = v).build());

        return b.build();
    }
}
