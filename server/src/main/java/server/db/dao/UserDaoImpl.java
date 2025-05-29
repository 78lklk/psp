package server.db.dao;

import common.model.User;
import server.db.mapper.UserMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Реализация DAO для работы с пользователями
 */
public class UserDaoImpl extends AbstractDao implements UserDao {
    private static final String INSERT_USER = 
            "INSERT INTO users (login, password, role_id, full_name, email, phone) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_USER = 
            "UPDATE users SET login = ?, password = ?, role_id = ?, full_name = ?, email = ?, phone = ? WHERE id = ?";
    private static final String DELETE_USER = "DELETE FROM users WHERE id = ?";
    private static final String SELECT_USER_BY_ID = 
            "SELECT u.id, u.login, u.password, u.role_id, r.name as role_name, u.full_name, u.email, u.phone, u.registration_date " +
            "FROM users u JOIN roles r ON u.role_id = r.id WHERE u.id = ?";
    private static final String SELECT_USER_BY_LOGIN = 
            "SELECT u.id, u.login, u.password, u.role_id, r.name as role_name, u.full_name, u.email, u.phone, u.registration_date " +
            "FROM users u JOIN roles r ON u.role_id = r.id WHERE u.login = ?";
    private static final String SELECT_USER_BY_ROLE = 
            "SELECT u.id, u.login, u.password, u.role_id, r.name as role_name, u.full_name, u.email, u.phone, u.registration_date " +
            "FROM users u JOIN roles r ON u.role_id = r.id WHERE r.name = ? LIMIT 1";
    private static final String SELECT_ALL_USERS = 
            "SELECT u.id, u.login, u.password, u.role_id, r.name as role_name, u.full_name, u.email, u.phone, u.registration_date " +
            "FROM users u JOIN roles r ON u.role_id = r.id";
    private static final String AUTHENTICATE_USER = 
            "SELECT u.id, u.login, u.password, u.role_id, r.name as role_name, u.full_name, u.email, u.phone, u.registration_date " +
            "FROM users u JOIN roles r ON u.role_id = r.id " +
            "WHERE LOWER(u.login) = LOWER(?) AND u.password = ?";

    @Override
    public Optional<User> findById(Long id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_USER_BY_ID);
            statement.setLong(1, id);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return Optional.of(UserMapper.map(resultSet));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error finding user by id: {}", id, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public Optional<User> findByLogin(String login) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = connection.prepareStatement(
                "SELECT u.id, u.login, u.password, u.role_id, r.name as role_name, u.full_name, u.email, u.phone, u.registration_date " +
                "FROM users u JOIN roles r ON u.role_id = r.id WHERE LOWER(u.login) = LOWER(?)");
            statement.setString(1, login);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return Optional.of(UserMapper.map(resultSet));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error finding user by login: {}", login, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public Optional<User> findByRole(String roleName) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_USER_BY_ROLE);
            statement.setString(1, roleName);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return Optional.of(UserMapper.map(resultSet));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error finding user by role: {}", roleName, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public Optional<String> getSaltByLogin(String login) {
        // Method no longer used with plaintext passwords
        return Optional.empty();
    }

    @Override
    public Optional<User> authenticate(String login, String password) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            // Get the user details first
            String sql = "SELECT u.id, u.login, u.password, u.role_id, r.name as role_name, u.full_name, u.email, u.phone, u.registration_date " +
                "FROM users u JOIN roles r ON u.role_id = r.id " +
                "WHERE LOWER(u.login) = LOWER(?)";
            
            logger.debug("Executing SQL for user lookup: {}", sql);
            
            statement = connection.prepareStatement(sql);
            statement.setString(1, login);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                String storedPassword = resultSet.getString("password");
                String roleName = resultSet.getString("role_name");
                
                // Secure password validation - only check stored password
                boolean isPasswordValid = password.equals(storedPassword);
                
                if (isPasswordValid) {
                    User user = UserMapper.map(resultSet);
                    logger.debug("User found: login={}, id={}", user.getLogin(), user.getId());
                    return Optional.of(user);
                }
            }
            
            logger.debug("No user found for login={} and provided password", login);
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error during authentication for user {}: {}", login, e.getMessage(), e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public List<User> findAll() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<User> users = new ArrayList<>();
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ALL_USERS);
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                users.add(UserMapper.map(resultSet));
            }
            
            return users;
        } catch (SQLException e) {
            logger.error("Error retrieving all users", e);
            return users;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public User save(User user) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatementWithGeneratedKeys(connection, INSERT_USER);
            statement.setString(1, user.getLogin());
            statement.setString(2, user.getPassword());
            
            long roleId = 3L; // Default to Staff role (3)
            if (user.getRole() != null) {
                roleId = user.getRole().getId() != null ? user.getRole().getId() : 3L;
            }
            statement.setLong(3, roleId);
            
            // Construct full name from firstName and lastName
            String fullName = "";
            if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
                fullName = user.getFirstName();
                if (user.getLastName() != null && !user.getLastName().isEmpty()) {
                    fullName += " " + user.getLastName();
                }
            } else if (user.getLastName() != null && !user.getLastName().isEmpty()) {
                fullName = user.getLastName();
            } else if (user.getFullName() != null) {
                fullName = user.getFullName();
            }
            
            statement.setString(4, fullName);
            statement.setString(5, user.getEmail() != null ? user.getEmail() : "");
            statement.setString(6, user.getPhone() != null ? user.getPhone() : "");
            
            long id = executeUpdateAndGetGeneratedKey(statement);
            if (id > 0) {
                user.setId(id);
                logger.info("User successfully created with ID: {}", id);
            } else {
                logger.error("Failed to get generated ID for user: {}", user.getLogin());
            }
            
            return user;
        } catch (SQLException e) {
            logger.error("Error saving user: {}", user.getLogin(), e);
            return user;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public boolean update(User user) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            
            // Check if user exists
            PreparedStatement checkUserStmt = connection.prepareStatement("SELECT * FROM users WHERE id = ?");
            checkUserStmt.setLong(1, user.getId());
            ResultSet userRs = checkUserStmt.executeQuery();
            
            if (!userRs.next()) {
                logger.error("User with ID {} does not exist", user.getId());
                userRs.close();
                checkUserStmt.close();
                return false;
            }
            
            // Get existing user data
            String existingLogin = userRs.getString("login");
            String existingPassword = userRs.getString("password");
            userRs.close();
            checkUserStmt.close();
            
            // If login is being changed, check if new login is already in use
            if (!existingLogin.equals(user.getLogin())) {
                PreparedStatement checkLoginStmt = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE login = ? AND id != ?");
                checkLoginStmt.setString(1, user.getLogin());
                checkLoginStmt.setLong(2, user.getId());
                ResultSet loginRs = checkLoginStmt.executeQuery();
                if (loginRs.next() && loginRs.getInt(1) > 0) {
                    logger.error("Login {} is already in use by another user", user.getLogin());
                    loginRs.close();
                    checkLoginStmt.close();
                    return false;
                }
                loginRs.close();
                checkLoginStmt.close();
            }
            
            // If password is null, use existing password
            if (user.getPassword() == null) {
                user.setPassword(existingPassword);
            }
            
            statement = prepareStatement(connection, UPDATE_USER);
            statement.setString(1, user.getLogin());
            statement.setString(2, user.getPassword());
            
            long roleId = 1L;
            if (user.getRole() != null) {
                roleId = user.getRole().getId() != null ? user.getRole().getId() : 1L;
            }
            statement.setLong(3, roleId);
            
            // Construct full name from firstName and lastName
            String fullName = "";
            if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
                fullName = user.getFirstName();
                if (user.getLastName() != null && !user.getLastName().isEmpty()) {
                    fullName += " " + user.getLastName();
                }
            } else if (user.getLastName() != null && !user.getLastName().isEmpty()) {
                fullName = user.getLastName();
            } else if (user.getFullName() != null) {
                fullName = user.getFullName();
            }
            
            statement.setString(4, fullName);
            statement.setString(5, user.getEmail() != null ? user.getEmail() : "");
            statement.setString(6, user.getPhone() != null ? user.getPhone() : "");
            statement.setLong(7, user.getId());
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error updating user: {}", user.getId(), e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        Connection connection = null;
        PreparedStatement deleteCardsStatement = null;
        PreparedStatement deleteUserStatement = null;
        
        try {
            connection = getConnection();
            connection.setAutoCommit(false);
            
            // First, delete all cards associated with this user
            deleteCardsStatement = connection.prepareStatement("DELETE FROM cards WHERE user_id = ?");
            deleteCardsStatement.setLong(1, id);
            int cardsDeleted = deleteCardsStatement.executeUpdate();
            logger.debug("Deleted {} cards associated with user ID: {}", cardsDeleted, id);
            
            // Then, delete the user
            deleteUserStatement = connection.prepareStatement(DELETE_USER);
            deleteUserStatement.setLong(1, id);
            int result = deleteUserStatement.executeUpdate();
            
            connection.commit();
            
            if (result > 0) {
                logger.info("User with ID {} successfully deleted", id);
                return true;
            } else {
                logger.warn("No user found with ID {}", id);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error deleting user: {}", id, e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            return false;
        } finally {
            if (deleteCardsStatement != null) {
                try {
                    deleteCardsStatement.close();
                } catch (SQLException e) {
                    logger.error("Error closing statement", e);
                }
            }
            if (deleteUserStatement != null) {
                try {
                    deleteUserStatement.close();
                } catch (SQLException e) {
                    logger.error("Error closing statement", e);
                }
            }
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Error closing connection", e);
                }
            }
        }
    }

    public Optional<User> findByUsername(String username) {
        return findByLogin(username);
    }
} 