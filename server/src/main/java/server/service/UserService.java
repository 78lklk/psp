package server.service;

import common.dto.AuthRequest;
import common.dto.AuthResponse;
import common.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Интерфейс сервиса для работы с пользователями
 */
public interface UserService {
    /**
     * Аутентификация пользователя
     * @param authRequest запрос аутентификации
     * @return результат аутентификации
     */
    AuthResponse authenticate(AuthRequest authRequest);
    
    /**
     * Создание нового пользователя
     * @param user пользователь
     * @param password пароль в открытом виде
     * @return созданный пользователь
     */
    User createUser(User user, String password);
    
    /**
     * Создание нового пользователя
     * @param user пользователь с паролем
     * @return созданный пользователь
     */
    User createUser(User user);
    
    /**
     * Изменение пароля пользователя
     * @param userId идентификатор пользователя
     * @param oldPassword старый пароль
     * @param newPassword новый пароль
     * @return true, если пароль успешно изменен
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * Получение пользователя по идентификатору
     * @param id идентификатор пользователя
     * @return пользователь или пустой Optional, если не найден
     */
    Optional<User> getUserById(Long id);
    
    /**
     * Получение пользователя по логину
     * @param login логин пользователя
     * @return пользователь или пустой Optional, если не найден
     */
    Optional<User> getUserByLogin(String login);
    
    /**
     * Получение пользователя по роли
     * @param roleName название роли (например, "ADMIN", "MANAGER", "STAFF")
     * @return первый найденный пользователь с указанной ролью или пустой Optional, если не найден
     */
    Optional<User> getUserByRole(String roleName);
    
    /**
     * Получение всех пользователей
     * @return список всех пользователей
     */
    List<User> getAllUsers();
    
    /**
     * Обновление пользователя
     * @param user пользователь
     * @return true, если пользователь успешно обновлен
     */
    boolean updateUser(User user);
    
    /**
     * Удаление пользователя
     * @param id идентификатор пользователя
     * @return true, если пользователь успешно удален
     */
    boolean deleteUser(Long id);
} 