package com.boat;

import com.boat.db.DatabaseManager;
import com.boat.model.Request;
import com.boat.model.User;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

public class AdvancedBoatCoordinatorBot extends TelegramLongPollingBot {
    private static final String BOT_TOKEN = "8591839154:AAFvcVX1E1jzx2aJIlrhD1wTFISpp6tCt4I";
    private static final String BOT_USERNAME = "Boat_64_Bot";
    private final DatabaseManager dbManager;

    public AdvancedBoatCoordinatorBot() {
        this.dbManager = new DatabaseManager();
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            var message = update.getMessage();
            Long chatId = message.getChatId();
            String username = message.getFrom().getUserName();

            // Добавляем пользователя в БД
            User existingUser = dbManager.getUser(chatId);
            if (existingUser == null) {
                dbManager.addUser(new User(chatId, username, false));
            }

            if (message.hasText()) {
                handleTextMessage(chatId, username, message.getText());
            } else if (message.hasLocation()) {
                handleLocation(chatId, username, message.getLocation());
            } else if (message.hasContact()) {
                handleContact(chatId, username, message.getContact());
            }
        }

        // Обработка callback запросов от inline кнопок
        if (update.hasCallbackQuery()) {
            handleCallback(update);
        }
    }

    private void handleCallback(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

        System.out.println("Callback: " + callbackData);

        // Удаляем сообщение с кнопками
        try {
            execute(new org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage(
                    String.valueOf(chatId), messageId));
        } catch (Exception e) {}

        if (callbackData.equals("my_requests")) {
            showMyRequests(chatId);
        }
        else if (callbackData.equals("exit_captain")) {
            exitCaptain(chatId);
        }
        else if (callbackData.startsWith("call_")) {
            Long userId = Long.parseLong(callbackData.replace("call_", ""));
            User user = dbManager.getUser(userId);

            if (user != null && user.getPhone() != null && !user.getPhone().isEmpty()) {
                String phone = user.getPhone();
                if (!phone.startsWith("+")) {
                    phone = "+" + phone;
                }
                sendTextMessage(chatId, "📞 НОМЕР ТЕЛЕФОНА:\n\n" + phone + "\n\n✅ Нажмите на номер, чтобы позвонить!");
            } else {
                sendTextMessage(chatId, "❌ Пользователь не указал номер телефона.");
            }
        }
        else if (callbackData.startsWith("accept_")) {
            try {
                Long requestId = Long.parseLong(callbackData.replace("accept_", ""));
                acceptBoatCall(chatId, requestId);
            } catch (NumberFormatException e) {}
        }
    }

    private ReplyKeyboardMarkup createMainKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("🚤 Вызвать лодку"));
        row.add(new KeyboardButton("⚓ Стать капитаном"));
        rows.add(row);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    // Меню капитана с inline кнопками
    private InlineKeyboardMarkup createCaptainMenuKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton myRequestsButton = new InlineKeyboardButton();
        myRequestsButton.setText("📋 МОИ ВЫЗОВЫ");
        myRequestsButton.setCallbackData("my_requests");
        row1.add(myRequestsButton);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton exitButton = new InlineKeyboardButton();
        exitButton.setText("⚓ ВЫЙТИ ИЗ РЕЖИМА КАПИТАНА");
        exitButton.setCallbackData("exit_captain");
        row2.add(exitButton);
        rows.add(row2);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private void handleTextMessage(Long chatId, String username, String text) {
        System.out.println("Обработка: " + text);

        switch (text) {
            case "/start":
                User user = dbManager.getUser(chatId);
                if (user != null && user.isCaptain()) {
                    // Показываем меню капитана
                    showCaptainMenu(chatId);
                } else {
                    showMainMenu(chatId);
                }
                break;
            case "🚤 Вызвать лодку":
                showCallButtons(chatId);
                break;
            case "⚓ Стать капитаном":
                registerCaptain(chatId, username);
                break;
            default:
                if (text.startsWith("✅ Принять вызов #")) {
                    acceptBoatCall(chatId, text);
                }
                break;
        }
    }

    private void showMainMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("🚤 Добро пожаловать!\n\n🚤 Вызвать лодку - отправьте геолокацию и телефон\n⚓ Стать капитаном - получать вызовы");
        message.setReplyMarkup(createMainKeyboard());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Показываем меню капитана
    private void showCaptainMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("👨‍✈️ МЕНЮ КАПИТАНА\n\nВыберите действие:");
        message.setReplyMarkup(createCaptainMenuKeyboard());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Показываем активные вызовы для капитана
    private void showMyRequests(Long chatId) {
        List<Request> requests = dbManager.getPendingRequests();

        if (requests.isEmpty()) {
            sendTextMessage(chatId, "📭 Нет активных вызовов.");
            showCaptainMenu(chatId);
            return;
        }

        StringBuilder response = new StringBuilder("📋 АКТИВНЫЕ ВЫЗОВЫ:\n\n");
        for (Request req : requests) {
            User user = dbManager.getUser(req.getUserChatId());
            String username = user != null && user.getUsername() != null ? user.getUsername() : "не указан";
            String phone = user != null && user.getPhone() != null ? user.getPhone() : "не указан";
            response.append(String.format("🆔 #%d\n👤 @%s\n📞 %s\n\n", req.getId(), username, phone));
        }

        sendTextMessage(chatId, response.toString());
        showCaptainMenu(chatId);
    }

    private void showCallButtons(Long chatId) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        // Первая кнопка - геолокация
        KeyboardRow row1 = new KeyboardRow();
        KeyboardButton locationButton = new KeyboardButton("📍 Отправить геолокацию");
        locationButton.setRequestLocation(true);
        row1.add(locationButton);
        rows.add(row1);

        // Вторая кнопка - телефон
        KeyboardRow row2 = new KeyboardRow();
        KeyboardButton phoneButton = new KeyboardButton("📞 Отправить номер телефона");
        phoneButton.setRequestContact(true);
        row2.add(phoneButton);
        rows.add(row2);

        keyboard.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("📍 ДЛЯ ВЫЗОВА ЛОДКИ ОТПРАВЬТЕ:\n\n1️⃣ ГЕОЛОКАЦИЮ - нажмите кнопку ниже\n2️⃣ НОМЕР ТЕЛЕФОНА - нажмите вторую кнопку\n\nПосле отправки обоих данных лодка будет вызвана!");
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleContact(Long chatId, String username, org.telegram.telegrambots.meta.api.objects.Contact contact) {
        String phone = contact.getPhoneNumber();

        User user = dbManager.getUser(chatId);
        if (user == null) {
            user = new User(chatId, username, false);
        }
        user.setPhone(phone);
        dbManager.addUser(user);

        System.out.println("✅ Телефон сохранен для " + chatId + ": " + phone);

        // Проверяем, есть ли уже локация
        if (user.getLatitude() != null && user.getLongitude() != null) {
            // Есть и локация и телефон - вызываем лодку
            createBoatCall(chatId, username, user.getLatitude(), user.getLongitude(), phone);
        } else {
            sendTextMessage(chatId, "✅ Номер телефона сохранен!\n\nТеперь отправьте геолокацию, чтобы вызвать лодку.");
            // Показываем кнопки снова
            showCallButtons(chatId);
        }
    }

    private void handleLocation(Long chatId, String username, org.telegram.telegrambots.meta.api.objects.Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        User user = dbManager.getUser(chatId);
        if (user == null) {
            user = new User(chatId, username, false);
        }
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        dbManager.addUser(user);

        System.out.println("✅ Локация сохранена для " + chatId + ": " + latitude + ", " + longitude);

        // Проверяем, есть ли уже телефон
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            // Есть и локация и телефон - вызываем лодку
            createBoatCall(chatId, username, latitude, longitude, user.getPhone());
        } else {
            sendTextMessage(chatId, "✅ Геолокация сохранена!\n\nТеперь отправьте номер телефона, чтобы вызвать лодку.");
            // Показываем кнопки снова
            showCallButtons(chatId);
        }
    }

    private void createBoatCall(Long chatId, String username, double lat, double lon, String phone) {
        // Создаём запрос
        Request request = new Request();
        request.setUserChatId(chatId);
        request.setDescription("Вызов лодки");
        dbManager.addRequest(request);

        String formattedPhone = phone;
        if (!formattedPhone.startsWith("+")) {
            formattedPhone = "+" + formattedPhone;
        }

        // Подтверждение пользователю
        String confirmText = String.format(
                "✅ ЛОДКА ВЫЗВАНА!\n\n📍 Ваша локация: %.6f, %.6f\n📞 Ваш телефон: %s\n\nКапитаны получили ваш вызов и скоро позвонят!",
                lat, lon, formattedPhone
        );

        SendMessage confirmMsg = new SendMessage();
        confirmMsg.setChatId(String.valueOf(chatId));
        confirmMsg.setText(confirmText);
        confirmMsg.setReplyMarkup(createMainKeyboard());

        try {
            execute(confirmMsg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        // Отправляем вызов всем капитанам
        List<User> captains = dbManager.getCaptains();
        System.out.println("📢 Отправка вызова #" + request.getId() + " " + captains.size() + " капитанам");

        for (User captain : captains) {
            sendCallToCaptain(captain.getChatId(), request, lat, lon, chatId, username, formattedPhone);
        }
    }

    private void sendCallToCaptain(Long captainChatId, Request request, double lat, double lon, Long userId, String username, String phone) {
        try {
            // Создаём inline кнопки для звонка и принятия вызова
            InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton callButton = new InlineKeyboardButton();
            callButton.setText("📞 ПОЗВОНИТЬ ПОЛЬЗОВАТЕЛЮ");
            callButton.setCallbackData("call_" + userId);
            row.add(callButton);

            InlineKeyboardButton acceptButton = new InlineKeyboardButton();
            acceptButton.setText("✅ ПРИНЯТЬ ВЫЗОВ #" + request.getId());
            acceptButton.setCallbackData("accept_" + request.getId());
            row.add(acceptButton);

            rows.add(row);
            inlineKeyboard.setKeyboard(rows);

            String message = String.format(
                    "🚤 НОВЫЙ ВЫЗОВ #%d!\n\n" +
                            "📍 ЛОКАЦИЯ: %.6f, %.6f\n" +
                            "👤 ПОЛЬЗОВАТЕЛЬ: @%s\n" +
                            "📞 ТЕЛЕФОН: %s",
                    request.getId(), lat, lon,
                    username != null ? username : "не указан",
                    phone
            );

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(captainChatId));
            sendMessage.setText(message);
            sendMessage.setReplyMarkup(inlineKeyboard);
            execute(sendMessage);

            // Отправляем карту с локацией
            SendLocation sendLocation = new SendLocation();
            sendLocation.setChatId(String.valueOf(captainChatId));
            sendLocation.setLatitude(lat);
            sendLocation.setLongitude(lon);
            execute(sendLocation);

            System.out.println("✅ Вызов отправлен капитану " + captainChatId);

        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки капитану " + captainChatId + ": " + e.getMessage());
        }
    }

    private void registerCaptain(Long chatId, String username) {
        User user = dbManager.getUser(chatId);
        if (user == null) {
            user = new User(chatId, username, true);
        } else {
            user.setCaptain(true);
        }
        dbManager.addUser(user);

        // После регистрации показываем меню капитана
        showCaptainMenu(chatId);
    }

    private void exitCaptain(Long chatId) {
        dbManager.removeCaptain(chatId);
        sendTextMessage(chatId, "✅ Вы вышли из режима капитана. Запросы больше приходить не будут.");
        showMainMenu(chatId);
    }

    private void acceptBoatCall(Long captainChatId, String text) {
        try {
            Long requestId = Long.parseLong(text.replace("✅ Принять вызов #", "").trim());

            Request request = getRequestById(requestId);
            if (request == null) {
                sendTextMessage(captainChatId, "❌ Вызов не найден.");
                return;
            }

            dbManager.updateRequestStatus(requestId, "ACCEPTED", captainChatId);

            User user = dbManager.getUser(request.getUserChatId());

            if (user != null && user.getPhone() != null) {
                String phone = user.getPhone();
                if (!phone.startsWith("+")) {
                    phone = "+" + phone;
                }
                sendTextMessage(captainChatId, "📞 НОМЕР ПОЛЬЗОВАТЕЛЯ: " + phone + "\n\n✅ Нажмите на номер, чтобы позвонить!");
                sendTextMessage(user.getChatId(), "✅ Капитан принял ваш вызов! Скоро свяжется.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendTextMessage(captainChatId, "❌ Ошибка.");
        }
    }

    private void acceptBoatCall(Long captainChatId, Long requestId) {
        Request request = getRequestById(requestId);
        if (request == null) {
            sendTextMessage(captainChatId, "❌ Вызов не найден.");
            return;
        }

        dbManager.updateRequestStatus(requestId, "ACCEPTED", captainChatId);

        User user = dbManager.getUser(request.getUserChatId());

        if (user != null && user.getPhone() != null) {
            String phone = user.getPhone();
            if (!phone.startsWith("+")) {
                phone = "+" + phone;
            }
            sendTextMessage(captainChatId, "📞 НОМЕР ПОЛЬЗОВАТЕЛЯ: " + phone + "\n\n✅ Нажмите на номер, чтобы позвонить!");
            sendTextMessage(user.getChatId(), "✅ Капитан принял ваш вызов! Скоро свяжется.");
        }
    }

    private Request getRequestById(Long requestId) {
        for (Request req : dbManager.getAllRequests()) {
            if (req.getId().equals(requestId)) {
                return req;
            }
        }
        return null;
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(createMainKeyboard());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}