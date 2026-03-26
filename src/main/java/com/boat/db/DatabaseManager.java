package com.boat.db;

import com.boat.model.Request;
import com.boat.model.User;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private Connection connection;

    public DatabaseManager() {
        try {
            connect();
            createTables();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка инициализации БД: " + e.getMessage(), e);
        }
    }

    private void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:boat_bot.db");
        connection.setAutoCommit(true);
    }

    private void createTables() throws SQLException {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                chat_id INTEGER PRIMARY KEY,
                username TEXT,
                phone TEXT,
                is_captain INTEGER DEFAULT 0,
                latitude REAL DEFAULT 0,
                longitude REAL DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createRequestsTable = """
            CREATE TABLE IF NOT EXISTS requests (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_chat_id INTEGER NOT NULL,
                description TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                status TEXT DEFAULT 'PENDING',
                captain_chat_id INTEGER,
                completed_at DATETIME,
                FOREIGN KEY (user_chat_id) REFERENCES users(chat_id)
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createRequestsTable);

            // Добавляем колонку phone, если её нет
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN phone TEXT");
                System.out.println("✅ Колонка phone добавлена");
            } catch (SQLException e) {
                if (!e.getMessage().contains("duplicate column")) {
                    // Игнорируем
                }
            }

            // Добавляем колонку completed_at в requests, если её нет
            try {
                stmt.execute("ALTER TABLE requests ADD COLUMN completed_at DATETIME");
                System.out.println("✅ Колонка completed_at добавлена");
            } catch (SQLException e) {
                if (!e.getMessage().contains("duplicate column")) {
                    // Игнорируем
                }
            }
        }
    }

    public void addUser(User user) {
        String sql = "INSERT OR REPLACE INTO users (chat_id, username, phone, is_captain, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, user.getChatId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPhone());
            pstmt.setInt(4, user.isCaptain() ? 1 : 0);
            pstmt.setDouble(5, user.getLatitude() != null ? user.getLatitude() : 0.0);
            pstmt.setDouble(6, user.getLongitude() != null ? user.getLongitude() : 0.0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения пользователя: " + e.getMessage(), e);
        }
    }

    public User getUser(Long chatId) {
        String sql = "SELECT * FROM users WHERE chat_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, chatId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                        rs.getLong("chat_id"),
                        rs.getString("username"),
                        rs.getInt("is_captain") == 1
                );
                try {
                    user.setPhone(rs.getString("phone"));
                } catch (SQLException e) {
                    user.setPhone(null);
                }
                user.setLatitude(rs.getDouble("latitude"));
                user.setLongitude(rs.getDouble("longitude"));
                return user;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения пользователя: " + e.getMessage(), e);
        }
        return null;
    }

    public List<User> getCaptains() {
        List<User> captains = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE is_captain = 1";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User(
                        rs.getLong("chat_id"),
                        rs.getString("username"),
                        true
                );
                try {
                    user.setPhone(rs.getString("phone"));
                } catch (SQLException e) {
                    user.setPhone(null);
                }
                user.setLatitude(rs.getDouble("latitude"));
                user.setLongitude(rs.getDouble("longitude"));
                captains.add(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения капитанов: " + e.getMessage(), e);
        }
        return captains;
    }

    public void addRequest(Request request) {
        String sql = "INSERT INTO requests (user_chat_id, description) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, request.getUserChatId());
            pstmt.setString(2, request.getDescription());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    request.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения запроса: " + e.getMessage(), e);
        }
    }

    public List<Request> getPendingRequests() {
        List<Request> requests = new ArrayList<>();
        String sql = "SELECT * FROM requests WHERE status = 'PENDING' ORDER BY created_at DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Request request = new Request();
                request.setId(rs.getLong("id"));
                request.setUserChatId(rs.getLong("user_chat_id"));
                request.setDescription(rs.getString("description"));
                request.setStatus(rs.getString("status"));

                try {
                    String dateStr = rs.getString("created_at");
                    if (dateStr != null && !dateStr.isEmpty()) {
                        request.setCreatedAt(parseDateTime(dateStr));
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка парсинга даты: " + e.getMessage());
                }

                if (rs.getObject("captain_chat_id") != null) {
                    request.setCaptainChatId(rs.getLong("captain_chat_id"));
                }
                requests.add(request);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения запросов: " + e.getMessage(), e);
        }
        return requests;
    }

    public List<Request> getAllRequests() {
        List<Request> requests = new ArrayList<>();
        String sql = "SELECT * FROM requests ORDER BY created_at DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Request request = new Request();
                request.setId(rs.getLong("id"));
                request.setUserChatId(rs.getLong("user_chat_id"));
                request.setDescription(rs.getString("description"));
                request.setStatus(rs.getString("status"));

                try {
                    String dateStr = rs.getString("created_at");
                    if (dateStr != null && !dateStr.isEmpty()) {
                        request.setCreatedAt(parseDateTime(dateStr));
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка парсинга даты: " + e.getMessage());
                }

                if (rs.getObject("captain_chat_id") != null) {
                    request.setCaptainChatId(rs.getLong("captain_chat_id"));
                }
                requests.add(request);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения запросов: " + e.getMessage(), e);
        }
        return requests;
    }

    private LocalDateTime parseDateTime(String dateStr) {
        try {
            if (dateStr.contains(" ")) {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else if (dateStr.contains("T")) {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (Exception e) {
            System.err.println("Ошибка парсинга даты '" + dateStr + "': " + e.getMessage());
        }
        return LocalDateTime.now();
    }

    public void updateRequestStatus(Long requestId, String status, Long captainChatId) {
        String sql = "UPDATE requests SET status = ?, captain_chat_id = ?, completed_at = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setLong(2, captainChatId);
            if (status.equals("COMPLETED")) {
                pstmt.setString(3, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            pstmt.setLong(4, requestId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления статуса запроса: " + e.getMessage(), e);
        }
    }

    public void removeCaptain(Long chatId) {
        String sql = "UPDATE users SET is_captain = 0 WHERE chat_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, chatId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления капитана: " + e.getMessage(), e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Ошибка закрытия соединения: " + e.getMessage());
        }
    }
}