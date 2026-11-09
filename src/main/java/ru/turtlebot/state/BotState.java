package ru.turtlebot.state;

public enum BotState {
    IDLE("Ожидание"),
    TELEPORT_HOME("Телепорт домой"),
    WAIT_TELEPORT("Ожидание телепорта"),
    PATH_TO_CHEST("Путь к сундуку"),
    OPEN_CHEST("Открытие сундука"),
    TAKE_GRASS("Забор травы"),
    CLOSE_CHEST("Закрытие сундука"),
    SCAN_TURTLES("Поиск черепах"),
    APPROACH_TURTLE("Подход к черепахе"),
    FEED_TURTLE("Кормление"),
    WAIT_COOLDOWN("Ожидание таймеров"),
    FINISHED("Завершено"),
    ERROR("Ошибка");

    public final String title;
    BotState(String title) { this.title = title; }
}
