package server.db.mapper;

import common.model.Card;
import common.model.Session;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Маппер для преобразования результатов запроса в объекты Session
 */
public class SessionMapper {
    /**
     * Преобразует результат запроса в объект Session
     * @param rs результат запроса
     * @return объект Session
     * @throws SQLException если произошла ошибка при получении данных
     */
    public static Session map(ResultSet rs) throws SQLException {
        Session session = new Session();
        session.setId(rs.getLong("id"));
        
        // Set card ID
        session.setCardId(rs.getLong("card_id"));
        
        // Set user ID if available
        try {
            session.setUserId(rs.getLong("user_id"));
        } catch (SQLException e) {
            // Column not found, ignore
        }
        
        // Преобразуем timestamp в LocalDateTime
        session.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        if (rs.getTimestamp("end_time") != null) {
            session.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        }
        
        session.setMinutes(rs.getInt("minutes"));
        
        // Handle points_earned field
        try {
            session.setPoints(rs.getInt("points_earned"));
        } catch (SQLException e) {
            // Try legacy points field
            try {
                session.setPoints(rs.getInt("points"));
            } catch (SQLException e2) {
                session.setPoints(0);
            }
        }
        
        // Set computer info if available
        if (hasColumn(rs, "computer_info")) {
            session.setComputerInfo(rs.getString("computer_info"));
        }
        
        // Set status if available
        if (hasColumn(rs, "status")) {
            session.setStatus(rs.getString("status"));
        }
        
        // Set notes if available
        if (hasColumn(rs, "notes")) {
            session.setNotes(rs.getString("notes"));
        }
        
        // Создаем объект Card, если есть данные о карте
        if (hasColumn(rs, "card_id")) {
            Card card = new Card();
            card.setId(rs.getLong("card_id"));
            
            // Если есть дополнительные данные о карте
            if (hasColumn(rs, "card_number")) {
                card.setCardNumber(rs.getString("card_number"));
            }
            
            session.setCard(card);
        }
        
        return session;
    }
    
    /**
     * Проверяет, содержит ли результат запроса указанный столбец
     * @param rs результат запроса
     * @param columnName имя столбца
     * @return true, если столбец существует
     * @throws SQLException если произошла ошибка при получении метаданных
     */
    private static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        int columns = metaData.getColumnCount();
        for (int i = 1; i <= columns; i++) {
            if (columnName.equals(metaData.getColumnName(i))) {
                return true;
            }
        }
        return false;
    }
} 