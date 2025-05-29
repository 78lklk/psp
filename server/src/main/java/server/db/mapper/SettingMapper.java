package server.db.mapper;

import common.model.Setting;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Маппер для преобразования данных из ResultSet в объект Setting
 */
public class SettingMapper {
    
    /**
     * Преобразует данные из ResultSet в объект Setting
     * @param resultSet результат SQL-запроса
     * @return объект Setting
     * @throws SQLException если произошла ошибка при чтении данных
     */
    public static Setting map(ResultSet resultSet) throws SQLException {
        Setting setting = new Setting();
        
        // Обязательные поля
        setting.setKey(resultSet.getString("key"));
        setting.setValue(resultSet.getString("value"));
        
        // Опциональные поля
        String description = resultSet.getString("description");
        if (description != null) {
            setting.setDescription(description);
        }
        
        // Дата последнего обновления
        java.sql.Timestamp lastUpdatedTimestamp = resultSet.getTimestamp("last_updated");
        if (lastUpdatedTimestamp != null) {
            setting.setLastUpdated(lastUpdatedTimestamp.toLocalDateTime());
        } else {
            setting.setLastUpdated(LocalDateTime.now());
        }
        
        // Кто обновил
        String updatedBy = resultSet.getString("updated_by");
        if (updatedBy != null) {
            setting.setUpdatedBy(updatedBy);
        }
        
        return setting;
    }
} 