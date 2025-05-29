package server.db.dao;

import common.model.Role;

import java.util.Optional;

/**
 * DAO для работы с ролями
 */
public interface RoleDao extends Dao<Role, Long> {
    /**
     * Найти роль по названию
     * @param name название роли
     * @return роль или пустой Optional, если не найдена
     */
    Optional<Role> findByName(String name);
} 