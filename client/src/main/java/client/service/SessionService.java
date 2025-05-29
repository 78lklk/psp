package client.service;

import client.model.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с игровыми сессиями компьютеров
 * Работает напрямую с БД через SQL
 */
public class SessionService {
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    public static final String DB_URL = "jdbc:postgresql://localhost:5432/loyalty_db";
    public static final String DB_USER = "postgres";
    public static final String DB_PASSWORD = "27932102300"; // пароль для подключения к БД
    
    private final String authToken; // не используется, но нужен для совместимости
    
    public SessionService(String authToken) {
        this.authToken = authToken;
    }
    
    /**
     * Получает соединение с БД
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
    
    /**
     * Получает список всех сессий
     */
    public List<Session> getAllSessions() {
        List<Session> sessions = new ArrayList<>();
        
        String sql = "SELECT s.id, s.card_id, s.computer_number, s.start_time, s.end_time, " +
                     "s.minutes, s.points_earned, s.status, c.number AS card_number, c.user_id " +
                     "FROM sessions s " +
                     "LEFT JOIN cards c ON s.card_id = c.id " +
                     "ORDER BY s.start_time DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Session session = new Session();
                
                session.setId(rs.getLong("id"));
                session.setCardId(rs.getLong("card_id"));
                session.setComputerName("ПК #" + rs.getInt("computer_number"));
                
                Timestamp startTime = rs.getTimestamp("start_time");
                if (startTime != null) {
                    session.setStartTime(startTime.toLocalDateTime());
                }
                
                Timestamp endTime = rs.getTimestamp("end_time");
                if (endTime != null) {
                    session.setEndTime(endTime.toLocalDateTime());
                }
                
                session.setMinutes(rs.getInt("minutes"));
                session.setPoints(rs.getInt("points_earned"));
                session.setActive(rs.getString("status") == null || rs.getString("status").equals("ACTIVE"));
                session.setCardNumber(rs.getString("card_number"));
                
                // Получаем имя пользователя отдельным запросом, если есть user_id
                Long userId = rs.getLong("user_id");
                if (!rs.wasNull() && userId > 0) {
                    try (PreparedStatement userStmt = conn.prepareStatement(
                            "SELECT login, full_name FROM users WHERE id = ?")) {
                        userStmt.setLong(1, userId);
                        try (ResultSet userRs = userStmt.executeQuery()) {
                            if (userRs.next()) {
                                // Используем full_name если доступно, иначе login
                                String fullName = userRs.getString("full_name");
                                if (fullName != null && !fullName.trim().isEmpty()) {
                                    session.setClientName(fullName);
                                } else {
                                    session.setClientName(userRs.getString("login"));
                                }
                            }
                        }
                    }
                }
                
                sessions.add(session);
            }
            
            logger.debug("Загружено {} сессий", sessions.size());
        } catch (SQLException e) {
            logger.error("Ошибка при получении списка сессий", e);
        }
        
        return sessions;
    }
    
    /**
     * Получает все активные сессии
     */
    public List<Session> getActiveSessions() {
        List<Session> sessions = new ArrayList<>();
        
        String sql = "SELECT s.id, s.card_id, s.computer_number, s.start_time, s.end_time, " +
                     "s.minutes, s.points_earned, s.status, c.number AS card_number, c.user_id " +
                     "FROM sessions s " +
                     "LEFT JOIN cards c ON s.card_id = c.id " +
                     "WHERE s.status = 'ACTIVE' " +
                     "ORDER BY s.start_time DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Session session = new Session();
                
                session.setId(rs.getLong("id"));
                session.setCardId(rs.getLong("card_id"));
                session.setComputerName("ПК #" + rs.getInt("computer_number"));
                
                Timestamp startTime = rs.getTimestamp("start_time");
                if (startTime != null) {
                    session.setStartTime(startTime.toLocalDateTime());
                }
                
                Timestamp endTime = rs.getTimestamp("end_time");
                if (endTime != null) {
                    session.setEndTime(endTime.toLocalDateTime());
                }
                
                session.setMinutes(rs.getInt("minutes"));
                session.setPoints(rs.getInt("points_earned"));
                session.setActive(rs.getString("status") == null || rs.getString("status").equals("ACTIVE"));
                session.setCardNumber(rs.getString("card_number"));
                
                // Получаем имя пользователя через дополнительный запрос
                Long userId = rs.getLong("user_id");
                if (!rs.wasNull() && userId > 0) {
                    try (PreparedStatement userStmt = conn.prepareStatement(
                            "SELECT login, full_name FROM users WHERE id = ?")) {
                        userStmt.setLong(1, userId);
                        try (ResultSet userRs = userStmt.executeQuery()) {
                            if (userRs.next()) {
                                // Используем full_name если доступно, иначе login
                                String fullName = userRs.getString("full_name");
                                if (fullName != null && !fullName.trim().isEmpty()) {
                                    session.setClientName(fullName);
                                } else {
                                    session.setClientName(userRs.getString("login"));
                                }
                            }
                        }
                    }
                }
                
                sessions.add(session);
            }
            
            logger.debug("Загружено {} активных сессий", sessions.size());
        } catch (SQLException e) {
            logger.error("Ошибка при получении списка активных сессий", e);
        }
        
        return sessions;
    }
    
    /**
     * Получает сессию по ID
     */
    public Optional<Session> getSessionById(Long sessionId) {
        String sql = "SELECT s.id, s.card_id, s.computer_number, s.start_time, s.end_time, " +
                     "s.minutes, s.points_earned, s.status, c.number AS card_number, c.user_id " +
                     "FROM sessions s " +
                     "LEFT JOIN cards c ON s.card_id = c.id " +
                     "WHERE s.id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, sessionId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Session session = new Session();
                
                    session.setId(rs.getLong("id"));
                    session.setCardId(rs.getLong("card_id"));
                    session.setComputerName("ПК #" + rs.getInt("computer_number"));
                    
                    Timestamp startTime = rs.getTimestamp("start_time");
                    if (startTime != null) {
                        session.setStartTime(startTime.toLocalDateTime());
                    }
                    
                    Timestamp endTime = rs.getTimestamp("end_time");
                    if (endTime != null) {
                        session.setEndTime(endTime.toLocalDateTime());
                    }
                    
                    session.setMinutes(rs.getInt("minutes"));
                    session.setPoints(rs.getInt("points_earned"));
                    session.setActive(rs.getString("status") == null || rs.getString("status").equals("ACTIVE"));
                    session.setCardNumber(rs.getString("card_number"));
                    
                    // Получаем имя пользователя отдельным запросом, если есть user_id
                    Long userId = rs.getLong("user_id");
                    if (!rs.wasNull() && userId > 0) {
                        try (PreparedStatement userStmt = conn.prepareStatement(
                                "SELECT login, full_name FROM users WHERE id = ?")) {
                            userStmt.setLong(1, userId);
                            try (ResultSet userRs = userStmt.executeQuery()) {
                                if (userRs.next()) {
                                    // Используем full_name если доступно, иначе login
                                    String fullName = userRs.getString("full_name");
                                    if (fullName != null && !fullName.trim().isEmpty()) {
                                        session.setClientName(fullName);
                                    } else {
                                        session.setClientName(userRs.getString("login"));
                                    }
                                }
                            }
                        }
                    }
                    
                    return Optional.of(session);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Ошибка при получении сессии по ID {}", sessionId, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Получает сессии по ID карты и периоду
     */
    public List<Session> getSessionsByCardIdAndPeriod(Long cardId, LocalDateTime fromDate, LocalDateTime toDate) {
        List<Session> sessions = new ArrayList<>();
        
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT s.id, s.card_id, s.computer_number, s.start_time, s.end_time, " +
                "s.minutes, s.points_earned, s.status, c.number AS card_number, c.user_id " +
                "FROM sessions s " +
                "LEFT JOIN cards c ON s.card_id = c.id " +
                "WHERE s.card_id = ?");
        
        if (fromDate != null) {
            sqlBuilder.append(" AND s.start_time >= ?");
        }
        
        if (toDate != null) {
            sqlBuilder.append(" AND s.start_time <= ?");
        }
        
        sqlBuilder.append(" ORDER BY s.start_time DESC");
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
            
            int paramIndex = 1;
            
            stmt.setLong(paramIndex++, cardId);
            
            if (fromDate != null) {
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(fromDate));
            }
            
            if (toDate != null) {
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(toDate));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Session session = new Session();
                
                    session.setId(rs.getLong("id"));
                    session.setCardId(rs.getLong("card_id"));
                    session.setComputerName("ПК #" + rs.getInt("computer_number"));
                    
                    Timestamp startTime = rs.getTimestamp("start_time");
                    if (startTime != null) {
                        session.setStartTime(startTime.toLocalDateTime());
                    }
                    
                    Timestamp endTime = rs.getTimestamp("end_time");
                    if (endTime != null) {
                        session.setEndTime(endTime.toLocalDateTime());
                    }
                    
                    session.setMinutes(rs.getInt("minutes"));
                    session.setPoints(rs.getInt("points_earned"));
                    session.setActive(rs.getString("status") == null || rs.getString("status").equals("ACTIVE"));
                    session.setCardNumber(rs.getString("card_number"));
                    
                    // Получаем имя пользователя отдельным запросом, если есть user_id
                    Long userId = rs.getLong("user_id");
                    if (!rs.wasNull() && userId > 0) {
                        try (PreparedStatement userStmt = conn.prepareStatement(
                                "SELECT login, full_name FROM users WHERE id = ?")) {
                            userStmt.setLong(1, userId);
                            try (ResultSet userRs = userStmt.executeQuery()) {
                                if (userRs.next()) {
                                    // Используем full_name если доступно, иначе login
                                    String fullName = userRs.getString("full_name");
                                    if (fullName != null && !fullName.trim().isEmpty()) {
                                        session.setClientName(fullName);
                                    } else {
                                        session.setClientName(userRs.getString("login"));
                                    }
                                }
                            }
                        }
                    }
                    
                    sessions.add(session);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Ошибка при получении сессий по ID карты {} и периоду", cardId, e);
        }
        
        return sessions;
    }
    
    /**
     * Создает новую сессию - максимально простой метод
     */
    public Optional<Session> createSession(Long cardId, Integer minutes, String computerName) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO sessions (card_id, computer_number, start_time, minutes, status) " +
                 "VALUES (?, ?, ?, ?, 'ACTIVE') RETURNING id", Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, cardId);
            stmt.setString(2, computerName);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(4, minutes);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Long sessionId = generatedKeys.getLong(1);
                        logger.info("Создана новая сессия с ID {} для карты {} на компьютере {}", 
                                sessionId, cardId, computerName);
                        
                        // Создаем объект сессии вручную вместо повторного запроса к БД
                        Session session = new Session();
                        session.setId(sessionId);
                        session.setCardId(cardId);
                        session.setComputerName(computerName);
                        session.setStartTime(LocalDateTime.now());
                        session.setMinutes(minutes);
                        session.setPoints(0);
                        session.setActive(true);
                        
                        return Optional.of(session);
                    }
                }
            }
            
            logger.error("Не удалось создать сессию - запрос не вернул ID");
                                return Optional.empty();
            
        } catch (SQLException e) {
            logger.error("Ошибка при создании сессии: {}", e.getMessage(), e);
                            return Optional.empty();
        }
    }
    
    /**
     * Завершает сессию
     */
    public Optional<Session> finishSession(Long sessionId) {
        String sql = "UPDATE sessions SET status = 'FINISHED', end_time = ?, points_earned = ? " +
                     "WHERE id = ? AND status = 'ACTIVE' " +
                     "RETURNING id";
        
        try (Connection conn = getConnection()) {
            
            // Получаем текущую сессию
            Optional<Session> currentSessionOpt = getSessionById(sessionId);
            if (!currentSessionOpt.isPresent() || !currentSessionOpt.get().isActive()) {
                logger.warn("Сессия с ID {} не найдена или уже завершена", sessionId);
                            return Optional.empty();
                        }
            
            Session currentSession = currentSessionOpt.get();
            
            // Вычисляем количество баллов (10% от количества минут)
            LocalDateTime now = LocalDateTime.now();
            int elapsedMinutes = currentSession.getMinutes() - currentSession.getRemainingMinutes(now);
            int points = Math.max(elapsedMinutes / 10, 1);  // Минимум 1 балл
            
            // Завершаем сессию
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.valueOf(now));
                stmt.setInt(2, points);
                stmt.setLong(3, sessionId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        logger.info("Сессия с ID {} успешно завершена. Начислено {} баллов", sessionId, points);
                        
                        // Получаем обновленную сессию
                        return getSessionById(sessionId);
                    }
                }
            }
            
        } catch (SQLException e) {
            logger.error("Ошибка при завершении сессии", e);
        }
        
                        return Optional.empty();
    }
    
    /**
     * Преобразует ResultSet в объект Session
     */
    private Session mapResultSetToSession(ResultSet rs) throws SQLException {
        Session session = new Session();
        
        session.setId(rs.getLong("id"));
        session.setCardId(rs.getLong("card_id"));
        session.setComputerName("ПК #" + rs.getInt("computer_number"));
        
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            session.setStartTime(startTime.toLocalDateTime());
        }
        
        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            session.setEndTime(endTime.toLocalDateTime());
        }
        
        session.setMinutes(rs.getInt("minutes"));
        session.setPoints(rs.getInt("points_earned"));
        session.setActive(rs.getString("status") == null || rs.getString("status").equals("ACTIVE"));
        session.setCardNumber(rs.getString("card_number"));
        session.setClientName(rs.getString("client_name"));
        
        return session;
    }
} 