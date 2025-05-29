package server.db.mapper;

import common.model.Role;
import common.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Маппер для преобразования результатов запроса в объекты User
 */
public class UserMapper {
    /**
     * Преобразует результат запроса в объект User
     * @param rs результат запроса
     * @return объект User
     * @throws SQLException если произошла ошибка при получении данных
     */
    public static User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        
        // Map login to username
        user.setUsername(rs.getString("login"));
        
        // Password handling
        if (hasColumn(rs, "password")) {
            user.setPassword(rs.getString("password"));
        }
        
        // Handle role
        if (hasColumn(rs, "role_id") && hasColumn(rs, "role_name")) {
            Role role = new Role(rs.getLong("role_id"), rs.getString("role_name"));
            user.setRole(role);
        }
        
        // Map additional fields that now match the DB
        if (hasColumn(rs, "email")) {
            user.setEmail(rs.getString("email"));
        }
        
        if (hasColumn(rs, "full_name")) {
            user.setFullName(rs.getString("full_name"));
        }
        
        if (hasColumn(rs, "phone")) {
            user.setPhone(rs.getString("phone"));
        }
        
        // Active status - default to true if not in DB
        if (hasColumn(rs, "is_active")) {
            user.setActive(rs.getBoolean("is_active"));
        }
        
        // Set creation date from registration_date if available
        if (hasColumn(rs, "registration_date")) {
            user.setCreatedAt(rs.getTimestamp("registration_date").toLocalDateTime());
        } else if (hasColumn(rs, "created_at")) {
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        } else {
            user.setCreatedAt(LocalDateTime.now());
        }
        
        // Set last login date if available
        if (hasColumn(rs, "last_login")) {
            user.setLastLogin(rs.getTimestamp("last_login").toLocalDateTime());
        }
        
        return user;
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