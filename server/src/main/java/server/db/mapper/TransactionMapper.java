package server.db.mapper;

import common.model.Card;
import common.model.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Маппер для преобразования результатов запроса в объекты Transaction
 */
public class TransactionMapper {
    /**
     * Преобразует результат запроса в объект Transaction
     * @param rs результат запроса
     * @return объект Transaction
     * @throws SQLException если произошла ошибка при получении данных
     */
    public static Transaction map(ResultSet rs) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(rs.getLong("id"));
        
        // Преобразуем тип транзакции из строки в enum
        String typeStr = rs.getString("type");
        transaction.setType(Transaction.Type.valueOf(typeStr));
        
        transaction.setPoints(rs.getInt("points"));
        transaction.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        
        // Опциональное описание
        if (hasColumn(rs, "description")) {
            transaction.setDescription(rs.getString("description"));
        }
        
        // Создаем объект Card, если есть данные о карте
        if (hasColumn(rs, "card_id")) {
            Card card = new Card();
            card.setId(rs.getLong("card_id"));
            
            // Если есть дополнительные данные о карте
            if (hasColumn(rs, "card_number")) {
                card.setCardNumber(rs.getString("card_number"));
            }
            
            transaction.setCard(card);
        }
        
        return transaction;
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