package server.db.dao;

import common.model.User;

import java.util.Optional;

/**
 * DAO для работы с пользователями
 */
public interface UserDao extends Dao<User, Long> {
    /**
     * Найти пользователя по логину
     * @param login логин пользователя
     * @return пользователь или пустой Optional, если не найден
     */
    Optional<User> findByLogin(String login);
    
    /**
     * Найти пользователя по роли
     * @param roleName имя роли
     * @return первый найденный пользователь с указанной ролью или пустой Optional, если не найден
     */
    Optional<User> findByRole(String roleName);
    
    /**
     * Получить соль пользователя по логину
     * @param login логин пользователя
     * @return соль или пустой Optional, если пользователь не найден
     */
    Optional<String> getSaltByLogin(String login);
    
    /**
     * Аутентификация пользователя
     * @param login логин пользователя
     * @param passwordHash хеш пароля
     * @return пользователь или пустой Optional, если аутентификация не удалась
     */
    Optional<User> authenticate(String login, String passwordHash);
} 