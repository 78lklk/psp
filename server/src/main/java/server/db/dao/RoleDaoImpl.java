package server.db.dao;

import common.model.Role;
import server.db.mapper.RoleMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Реализация DAO для работы с ролями
 */
public class RoleDaoImpl extends AbstractDao implements RoleDao {
    private static final String INSERT_ROLE = "INSERT INTO roles (name) VALUES (?)";
    private static final String UPDATE_ROLE = "UPDATE roles SET name = ? WHERE id = ?";
    private static final String DELETE_ROLE = "DELETE FROM roles WHERE id = ?";
    private static final String SELECT_ROLE_BY_ID = "SELECT id, name FROM roles WHERE id = ?";
    private static final String SELECT_ROLE_BY_NAME = "SELECT id, name FROM roles WHERE name = ?";
    private static final String SELECT_ALL_ROLES = "SELECT id, name FROM roles";

    @Override
    public Optional<Role> findById(Long id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ROLE_BY_ID);
            statement.setLong(1, id);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return Optional.of(RoleMapper.map(resultSet));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Ошибка при поиске роли по id: {}", id, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public Optional<Role> findByName(String name) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ROLE_BY_NAME);
            statement.setString(1, name);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return Optional.of(RoleMapper.map(resultSet));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Ошибка при поиске роли по имени: {}", name, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public List<Role> findAll() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Role> roles = new ArrayList<>();
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ALL_ROLES);
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                roles.add(RoleMapper.map(resultSet));
            }
            
            return roles;
        } catch (SQLException e) {
            logger.error("Ошибка при получении всех ролей", e);
            return roles;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public Role save(Role role) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatementWithGeneratedKeys(connection, INSERT_ROLE);
            statement.setString(1, role.getName());
            
            long id = executeUpdateAndGetGeneratedKey(statement);
            role.setId(id);
            
            return role;
        } catch (SQLException e) {
            logger.error("Ошибка при сохранении роли: {}", role.getName(), e);
            return role;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public boolean update(Role role) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, UPDATE_ROLE);
            statement.setString(1, role.getName());
            statement.setLong(2, role.getId());
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении роли: {}", role.getId(), e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, DELETE_ROLE);
            statement.setLong(1, id);
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при удалении роли: {}", id, e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }
} 