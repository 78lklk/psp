package server.db.mapper;

import common.model.AuditLog;
import common.model.Role;
import common.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Маппер для преобразования ResultSet в объекты AuditLog
 */
public class AuditLogMapper {
    
    /**
     * Преобразует ResultSet в объект AuditLog
     * @param rs ResultSet с данными записи аудита
     * @return объект AuditLog
     * @throws SQLException при ошибке доступа к данным
     */
    public static AuditLog mapResultSetToAuditLog(ResultSet rs) throws SQLException {
        AuditLog auditLog = new AuditLog();
        
        auditLog.setId(rs.getLong("id"));
        auditLog.setActionType(rs.getString("action_type"));
        auditLog.setActionDetails(rs.getString("action_details"));
        auditLog.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        auditLog.setIpAddress(rs.getString("ip_address"));
        
        String targetEntity = rs.getString("target_entity");
        if (!rs.wasNull()) {
            auditLog.setTargetEntity(targetEntity);
        }
        
        // Для полей, которые могут быть NULL, проверяем результат на NULL
        Long targetId = rs.getLong("target_id");
        if (!rs.wasNull()) {
            auditLog.setTargetId(targetId);
        }
        
        // Извлекаем данные пользователя, если они есть
        Long userId = rs.getLong("user_id");
        if (!rs.wasNull()) {
            User user = new User();
            user.setId(userId);
            
            // Пробуем получить имя пользователя
            try {
                String username = rs.getString("username");
                if (username != null) {
                    user.setUsername(username);
                }
            } catch (SQLException e) {
                // Игнорируем ошибку, если колонка не найдена
            }
            
            // Пробуем получить email пользователя
            try {
                String email = rs.getString("email");
                if (email != null) {
                    user.setEmail(email);
                }
            } catch (SQLException e) {
                // Игнорируем ошибку, если колонка не найдена
            }
            
            // Пробуем получить роль пользователя
            try {
                Long roleId = rs.getLong("role_id");
                if (!rs.wasNull()) {
                    String roleName = rs.getString("role_name");
                    Role role = new Role(roleId, roleName);
                    user.setRole(role);
                }
            } catch (SQLException e) {
                // Игнорируем ошибку, если колонки не найдены
            }
            
            auditLog.setUser(user);
        }
        
        return auditLog;
    }
} 