package com.boat;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class BotApplication {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            AdvancedBoatCoordinatorBot bot = new AdvancedBoatCoordinatorBot();
            botsApi.registerBot(bot);
            System.out.println("🚤 Boat Coordinator Bot успешно запущен!");
            System.out.println("Bot username: @" + bot.getBotUsername());
            System.out.println("Бот готов к работе. Нажмите Ctrl+C для остановки.");
        } catch (TelegramApiException e) {
            System.err.println("Ошибка запуска бота: " + e.getMessage());
            e.printStackTrace();
        }
    }
}