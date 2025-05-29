package server.service;

import common.dto.AuthRequest;
import common.dto.AuthResponse;
import common.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.db.dao.UserDao;
import server.db.dao.UserDaoImpl;
import server.util.TokenGenerator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Реализация сервиса для работы с пользователями
 */
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserDao userDao;
    
    public UserServiceImpl() {
        this.userDao = new UserDaoImpl();
    }
    
    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public AuthResponse authenticate(AuthRequest authRequest) {
        logger.debug("Попытка аутентификации пользователя: {}", authRequest.getLogin());
        
        try {
            // Debug: List all users in the database
            List<User> allUsers = userDao.findAll();
            logger.debug("Всего пользователей в базе данных: {}", allUsers.size());
            for (User user : allUsers) {
                logger.debug("Пользователь в БД: login={}, role={}, id={}", 
                             user.getLogin(), user.getRole().getName(), user.getId());
            }
            
            Optional<User> userOpt = userDao.authenticate(authRequest.getLogin(), authRequest.getPassword());
            if (userOpt.isEmpty()) {
                logger.debug("Неверный логин или пароль для пользователя: {}", authRequest.getLogin());
                return AuthResponse.error("Неверный логин или пароль");
            }
            
            User user = userOpt.get();
            logger.info("Пользователь успешно аутентифицирован: {}", user.getLogin());
            
            String token = TokenGenerator.generateToken();
            
            return AuthResponse.success(token, user);
        } catch (Exception e) {
            logger.error("Ошибка при аутентификации: {}", e.getMessage(), e);
            return AuthResponse.error("Ошибка аутентификации");
        }
    }
    
    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    @Override
    public User createUser(User user, String password) {
        logger.debug("Creating new user with separate password: {}", user.getLogin());
        
        // Set password and delegate to the other method
        user.setPassword(password);
        return createUser(user);
    }

    @Override
    public User createUser(User user) {
        logger.debug("Creating new user from complete user object: {}", user.getLogin());
        
        try {
            // Check if user with this login already exists
            Optional<User> existingUser = userDao.findByLogin(user.getLogin());
            if (existingUser.isPresent()) {
                logger.debug("User with login {} already exists", user.getLogin());
                throw new IllegalArgumentException("User with this login already exists");
            }
            
            // Password should already be set in the user object
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                logger.error("Password is not set for user: {}", user.getLogin());
                throw new IllegalArgumentException("Password is required");
            }
            
            // Ensure role is set
            if (user.getRole() == null) {
                logger.warn("Role not set for user: {}, defaulting to Staff", user.getLogin());
                common.model.Role defaultRole = new common.model.Role();
                defaultRole.setId(3L); // Assuming 3 is STAFF role
                defaultRole.setName("STAFF");
                user.setRole(defaultRole);
            }
            
            User createdUser = userDao.save(user);
            logger.info("Created new user: {}", createdUser.getLogin());
            
            return createdUser;
        } catch (Exception e) {
            logger.error("Error creating user: {}", user.getLogin(), e);
            throw new RuntimeException("Error creating user: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        logger.debug("Изменение пароля для пользователя с id: {}", userId);
        
        try {
            Optional<User> userOpt = userDao.findById(userId);
            if (userOpt.isEmpty()) {
                logger.debug("Пользователь с id {} не найден", userId);
                return false;
            }
            
            User user = userOpt.get();
            
            // Проверяем старый пароль
            if (!oldPassword.equals(user.getPassword())) {
                logger.debug("Неверный старый пароль для пользователя: {}", user.getLogin());
                return false;
            }
            
            // Устанавливаем новый пароль
            user.setPassword(newPassword);
            
            boolean updated = userDao.update(user);
            if (updated) {
                logger.info("Пароль изменен для пользователя: {}", user.getLogin());
            } else {
                logger.error("Не удалось обновить пароль для пользователя: {}", user.getLogin());
            }
            
            return updated;
        } catch (Exception e) {
            logger.error("Ошибка при изменении пароля для пользователя с id: {}", userId, e);
            return false;
        }
    }

    @Override
    public Optional<User> getUserById(Long id) {
        logger.debug("Получение пользователя по id: {}", id);
        return userDao.findById(id);
    }

    @Override
    public Optional<User> getUserByLogin(String login) {
        logger.debug("Получение пользователя по логину: {}", login);
        return userDao.findByLogin(login);
    }

    @Override
    public Optional<User> getUserByRole(String roleName) {
        logger.debug("Получение пользователя по роли: {}", roleName);
        return userDao.findByRole(roleName);
    }

    @Override
    public List<User> getAllUsers() {
        logger.debug("Получение всех пользователей");
        return userDao.findAll();
    }

    @Override
    public boolean updateUser(User user) {
        logger.debug("Обновление пользователя: {}", user.getId());
        try {
            boolean updated = userDao.update(user);
            if (updated) {
                logger.info("Пользователь обновлен: {}", user.getLogin());
            } else {
                logger.error("Не удалось обновить пользователя: {}", user.getLogin());
            }
            
            return updated;
        } catch (Exception e) {
            logger.error("Ошибка при обновлении пользователя: {}", user.getLogin(), e);
            return false;
        }
    }

    @Override
    public boolean deleteUser(Long id) {
        logger.debug("Удаление пользователя с id: {}", id);
        try {
            boolean deleted = userDao.deleteById(id);
            if (deleted) {
                logger.info("Пользователь удален: {}", id);
            } else {
                logger.error("Не удалось удалить пользователя: {}", id);
            }
            
            return deleted;
        } catch (Exception e) {
            logger.error("Ошибка при удалении пользователя с id: {}", id, e);
            return false;
        }
    }
} 