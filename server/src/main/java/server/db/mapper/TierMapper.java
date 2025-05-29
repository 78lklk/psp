package server.db.mapper;

import common.model.Tier;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Маппер для преобразования результатов запроса в объекты Tier
 */
public class TierMapper {
    /**
     * Преобразует результат запроса в объект Tier
     * @param rs результат запроса
     * @return объект Tier
     * @throws SQLException если произошла ошибка при получении данных
     */
    public static Tier map(ResultSet rs) throws SQLException {
        // Create Tier with level and name
        int level = 1; // Default level
        if (hasColumn(rs, "level")) {
            level = rs.getInt("level");
        } else if (hasColumn(rs, "id")) {
            // Use ID as level if no level column is available
            level = (int) rs.getLong("id");
        }
        
        String name = rs.getString("name");
        Tier tier = new Tier(level, name);
        
        // Set additional properties if available
        if (hasColumn(rs, "min_points")) {
            tier.setMinPoints(rs.getInt("min_points"));
        }
        
        if (hasColumn(rs, "max_points")) {
            tier.setMaxPoints(rs.getInt("max_points"));
        } else if (hasColumn(rs, "min_points")) {
            // If we have min_points but not max_points, we could derive it
            // This is just a placeholder example
            tier.setMaxPoints(tier.getMinPoints() + 500);
        }
        
        if (hasColumn(rs, "bonus_multiplier")) {
            tier.setBonusMultiplier(rs.getDouble("bonus_multiplier"));
        } else if (hasColumn(rs, "discount_pct")) {
            // Convert discount percentage to bonus multiplier if needed
            double discount = rs.getInt("discount_pct") / 100.0;
            tier.setBonusMultiplier(1.0 + discount);
        }
        
        return tier;
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