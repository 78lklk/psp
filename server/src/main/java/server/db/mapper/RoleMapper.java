package server.db.mapper;

import common.model.Role;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Маппер для преобразования результатов запроса в объекты Role
 */
public class RoleMapper {
    /**
     * Преобразует результат запроса в объект Role
     * @param rs результат запроса
     * @return объект Role
     * @throws SQLException если произошла ошибка при получении данных
     */
    public static Role map(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setId(rs.getLong("id"));
        role.setName(rs.getString("name"));
        return role;
    }
} 