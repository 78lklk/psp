package server.util;

import server.db.DatabaseConfig;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Утилита для генерации дополнительных тестовых данных
 */
public class DataGenerator {
    
    private static final Random random = new Random();
    
    // Массивы для генерации случайных данных
    private static final String[] FIRST_NAMES = {
        "Александр", "Мария", "Дмитрий", "Елена", "Сергей", "Анна", "Павел", "Ольга",
        "Игорь", "Наталья", "Роман", "Светлана", "Андрей", "Екатерина", "Максим", "Юлия",
        "Денис", "Ирина", "Владимир", "Алина", "Алексей", "Татьяна", "Михаил", "Виктория"
    };
    
    private static final String[] LAST_NAMES = {
        "Иванов", "Петрова", "Сидоров", "Козлова", "Смирнов", "Попова", "Волков", "Соколова",
        "Лебедев", "Морозова", "Новиков", "Федорова", "Орлов", "Михайлова", "Семенов", "Николаева",
        "Богданов", "Макарова", "Дмитриев", "Степанова", "Егоров", "Белова", "Матвеев", "Захарова"
    };
    
    private static final String[] GAME_TYPES = {
        "CS", "Dota", "LoL", "WoW", "PUBG", "Valorant", "Overwatch", "Apex", "Fortnite", "Minecraft"
    };
    
    public static void main(String[] args) {
        System.out.println("Начинаю генерацию дополнительных тестовых данных...");
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Проверяем текущее количество данных
            printCurrentStats(conn);
            
            // Генерируем дополнительные данные
            generateUsers(conn, 50);
            generateCards(conn);
            generatePromotions(conn, 15);
            generatePromoCodes(conn, 25);
            generateSessions(conn, 300);
            
            // Показываем новую статистику
            System.out.println("\n=== ПОСЛЕ ГЕНЕРАЦИИ ===");
            printCurrentStats(conn);
            
            System.out.println("\nГенерация данных завершена успешно!");
            
        } catch (SQLException e) {
            System.err.println("Ошибка при генерации данных: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void printCurrentStats(Connection conn) throws SQLException {
        String[] tables = {"users", "cards", "sessions", "transactions", "promotions", "promo_codes"};
        
        System.out.println("=== СТАТИСТИКА БАЗЫ ДАННЫХ ===");
        for (String table : tables) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM " + table);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println(table + ": " + rs.getInt(1) + " записей");
                }
            }
        }
        
        // Статистика за последний месяц
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM sessions WHERE start_time >= CURRENT_DATE - INTERVAL '30 days'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                System.out.println("Сессии за последний месяц: " + rs.getInt(1));
            }
        }
    }
    
    private static void generateUsers(Connection conn, int count) throws SQLException {
        System.out.println("Генерирую " + count + " дополнительных пользователей...");
        
        String sql = "INSERT INTO users (login, password, role_id, full_name, email, phone, registration_date, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                String gameType = GAME_TYPES[random.nextInt(GAME_TYPES.length)].toLowerCase();
                
                String login = firstName.toLowerCase() + "_" + gameType + "_" + (1000 + i);
                String fullName = firstName + " " + lastName;
                String email = login + "@email.ru";
                String phone = "+7-9" + String.format("%02d", 10 + random.nextInt(90)) + 
                              "-" + String.format("%03d", 100 + random.nextInt(900)) + 
                              "-" + String.format("%02d", 10 + random.nextInt(90)) + 
                              "-" + String.format("%02d", 10 + random.nextInt(90));
                
                // Регистрация в течение последних 12 месяцев
                LocalDateTime regDate = LocalDateTime.now().minusDays(random.nextInt(365));
                
                ps.setString(1, login);
                ps.setString(2, "password123");
                ps.setInt(3, 4); // CLIENT role
                ps.setString(4, fullName);
                ps.setString(5, email);
                ps.setString(6, phone);
                ps.setTimestamp(7, Timestamp.valueOf(regDate));
                ps.setBoolean(8, true);
                
                ps.addBatch();
                
                if (i % 10 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
        }
        
        System.out.println("Добавлено " + count + " пользователей");
    }
    
    private static void generateCards(Connection conn) throws SQLException {
        System.out.println("Создаю карты для новых пользователей...");
        
        // Получаем пользователей без карт
        String selectSql = "SELECT id, registration_date FROM users WHERE role_id = 4 AND id NOT IN (SELECT user_id FROM cards)";
        String insertSql = "INSERT INTO cards (number, user_id, tier_id, points, total_spent, total_sessions, issue_date, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement selectPs = conn.prepareStatement(selectSql);
             PreparedStatement insertPs = conn.prepareStatement(insertSql);
             ResultSet rs = selectPs.executeQuery()) {
            
            while (rs.next()) {
                int userId = rs.getInt("id");
                Timestamp regDate = rs.getTimestamp("registration_date");
                
                // Генерируем номер карты
                String cardNumber = "LC" + String.format("%03d", userId);
                
                // Случайные данные карты
                int tierId = 1 + random.nextInt(4); // 1-4
                int points = random.nextInt(2000);
                double totalSpent = 1000 + random.nextDouble() * 30000;
                int totalSessions = random.nextInt(200);
                
                insertPs.setString(1, cardNumber);
                insertPs.setInt(2, userId);
                insertPs.setInt(3, tierId);
                insertPs.setInt(4, points);
                insertPs.setDouble(5, totalSpent);
                insertPs.setInt(6, totalSessions);
                insertPs.setTimestamp(7, regDate);
                insertPs.setBoolean(8, true);
                
                insertPs.addBatch();
            }
            
            insertPs.executeBatch();
            System.out.println("Карты созданы для новых пользователей");
        }
    }
    
    private static void generatePromotions(Connection conn, int count) throws SQLException {
        System.out.println("Генерирую " + count + " дополнительных акций...");
        
        String[] promoNames = {
            "Кибер-понедельник", "Турбо-вторник", "Уютная среда", "Четверг геймера", 
            "Пятничная буря", "Субботняя атака", "Воскресный релакс", "Полуночный марафон",
            "Утренний кофе", "Дневной перерыв", "Вечерний драйв", "Ночная смена",
            "Командная игра", "Соло режим", "Турнирная лихорадка"
        };
        
        String sql = "INSERT INTO promotions (name, description, start_date, end_date, bonus_pct, bonus_points, is_active, created_by, max_uses, min_points_required, usage_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                String name = promoNames[i % promoNames.length];
                if (i >= promoNames.length) {
                    name += " " + (i / promoNames.length + 1);
                }
                
                LocalDateTime startDate = LocalDateTime.now().minusDays(random.nextInt(180));
                LocalDateTime endDate = startDate.plusDays(30 + random.nextInt(120));
                
                int bonusPct = random.nextInt(100);
                int bonusPoints = random.nextInt(300);
                boolean isActive = random.nextBoolean();
                Integer maxUses = random.nextBoolean() ? null : Integer.valueOf(50 + random.nextInt(500));
                int minPoints = random.nextInt(200);
                int usageCount = random.nextInt(100);
                
                ps.setString(1, name);
                ps.setString(2, "Автоматически сгенерированная акция: " + name);
                ps.setDate(3, Date.valueOf(startDate.toLocalDate()));
                ps.setDate(4, Date.valueOf(endDate.toLocalDate()));
                ps.setInt(5, bonusPct);
                ps.setInt(6, bonusPoints);
                ps.setBoolean(7, isActive);
                ps.setInt(8, 1); // admin
                
                if (maxUses != null) {
                    ps.setInt(9, maxUses);
                } else {
                    ps.setNull(9, Types.INTEGER);
                }
                
                ps.setInt(10, minPoints);
                ps.setInt(11, usageCount);
                
                ps.addBatch();
            }
            
            ps.executeBatch();
            System.out.println("Добавлено " + count + " акций");
        }
    }
    
    private static void generatePromoCodes(Connection conn, int count) throws SQLException {
        System.out.println("Генерирую " + count + " дополнительных промокодов...");
        
        String[] codeNames = {
            "MEGA", "SUPER", "ULTRA", "HYPER", "TURBO", "POWER", "SPEED", "FORCE",
            "BLAST", "STORM", "FLAME", "FROST", "SHADOW", "LIGHT", "DARK", "FIRE",
            "ICE", "WIND", "EARTH", "WATER", "ENERGY", "MAGIC", "CYBER", "QUANTUM"
        };
        
        String sql = "INSERT INTO promo_codes (code, promotion_id, is_used, used_by, used_date, expiry_date, created_by, bonus_points, discount_percent, uses_limit, uses_count, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                String code = codeNames[i % codeNames.length] + random.nextInt(1000);
                boolean isUsed = random.nextBoolean();
                LocalDateTime expiryDate = LocalDateTime.now().plusDays(30 + random.nextInt(365));
                
                int bonusPoints = random.nextInt(500);
                double discountPercent = random.nextDouble() * 50;
                int usesLimit = 1 + random.nextInt(100);
                int usesCount = isUsed ? random.nextInt(usesLimit) : 0;
                
                ps.setString(1, code);
                ps.setNull(2, Types.INTEGER); // Без привязки к акции
                ps.setBoolean(3, isUsed);
                ps.setNull(4, Types.INTEGER); // used_by
                ps.setNull(5, Types.TIMESTAMP); // used_date
                ps.setDate(6, Date.valueOf(expiryDate.toLocalDate()));
                ps.setInt(7, 1); // created_by admin
                ps.setInt(8, bonusPoints);
                ps.setDouble(9, discountPercent);
                ps.setInt(10, usesLimit);
                ps.setInt(11, usesCount);
                ps.setBoolean(12, true);
                
                ps.addBatch();
            }
            
            ps.executeBatch();
            System.out.println("Добавлено " + count + " промокодов");
        }
    }
    
    private static void generateSessions(Connection conn, int count) throws SQLException {
        System.out.println("Генерирую " + count + " дополнительных сессий...");
        
        // Получаем все карты
        String selectCardsSql = "SELECT id, user_id FROM cards WHERE is_active = true";
        String insertSessionSql = "INSERT INTO sessions (card_id, user_id, start_time, end_time, minutes, points_earned, price, computer_number, status, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertTransactionSql = "INSERT INTO transactions (card_id, type, points, timestamp, description, operator_id, session_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement selectPs = conn.prepareStatement(selectCardsSql);
             PreparedStatement sessionPs = conn.prepareStatement(insertSessionSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement transactionPs = conn.prepareStatement(insertTransactionSql);
             ResultSet cardsRs = selectPs.executeQuery()) {
            
            // Собираем карты в массив
            var cards = new java.util.ArrayList<int[]>();
            while (cardsRs.next()) {
                cards.add(new int[]{cardsRs.getInt("id"), cardsRs.getInt("user_id")});
            }
            
            for (int i = 0; i < count; i++) {
                int[] card = cards.get(random.nextInt(cards.size()));
                int cardId = card[0];
                int userId = card[1];
                
                // Генерируем случайную сессию за последние 90 дней
                LocalDateTime startTime = LocalDateTime.now().minusDays(random.nextInt(90));
                int minutes = 60 + random.nextInt(240); // 1-4 часа
                LocalDateTime endTime = startTime.plusMinutes(minutes);
                
                int pointsEarned = minutes / 10; // 10 минут = 1 балл
                double price = minutes * 3.0; // 3 рубля за минуту
                int computerNumber = 1 + random.nextInt(10);
                int operatorId = 3 + random.nextInt(3); // staff1, staff2, staff3
                
                sessionPs.setInt(1, cardId);
                sessionPs.setInt(2, userId);
                sessionPs.setTimestamp(3, Timestamp.valueOf(startTime));
                sessionPs.setTimestamp(4, Timestamp.valueOf(endTime));
                sessionPs.setInt(5, minutes);
                sessionPs.setInt(6, pointsEarned);
                sessionPs.setDouble(7, price);
                sessionPs.setInt(8, computerNumber);
                sessionPs.setString(9, "COMPLETED");
                sessionPs.setInt(10, operatorId);
                
                sessionPs.executeUpdate();
                
                // Получаем ID сессии
                try (ResultSet generatedKeys = sessionPs.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int sessionId = generatedKeys.getInt(1);
                        
                        // Создаем транзакцию на заработанные баллы
                        transactionPs.setInt(1, cardId);
                        transactionPs.setString(2, "EARN");
                        transactionPs.setInt(3, pointsEarned);
                        transactionPs.setTimestamp(4, Timestamp.valueOf(endTime));
                        transactionPs.setString(5, "Заработано за сессию");
                        transactionPs.setInt(6, operatorId);
                        transactionPs.setInt(7, sessionId);
                        
                        transactionPs.addBatch();
                        
                        // Иногда добавляем бонусные баллы
                        if (random.nextDouble() < 0.3) { // 30% шанс
                            int bonusPoints = random.nextInt(50) + 10;
                            transactionPs.setInt(1, cardId);
                            transactionPs.setString(2, "BONUS");
                            transactionPs.setInt(3, bonusPoints);
                            transactionPs.setTimestamp(4, Timestamp.valueOf(endTime));
                            transactionPs.setString(5, "Случайный бонус");
                            transactionPs.setInt(6, operatorId);
                            transactionPs.setInt(7, sessionId);
                            
                            transactionPs.addBatch();
                        }
                    }
                }
                
                if (i % 50 == 0) {
                    transactionPs.executeBatch();
                    System.out.println("Обработано " + i + " сессий...");
                }
            }
            
            transactionPs.executeBatch();
            System.out.println("Добавлено " + count + " сессий и соответствующих транзакций");
        }
    }
} 