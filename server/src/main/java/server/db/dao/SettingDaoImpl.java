package server.db.dao;

import common.model.Setting;
import server.db.mapper.SettingMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Реализация DAO для работы с настройками в базе данных
 */
public class SettingDaoImpl extends AbstractDao implements SettingDao {
    private static final String INSERT_SETTING = 
            "INSERT INTO settings (key, value, description, last_updated) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_SETTING = 
            "UPDATE settings SET value = ?, description = ?, last_updated = ? WHERE key = ?";
    private static final String DELETE_SETTING = 
            "DELETE FROM settings WHERE key = ?";
    private static final String SELECT_SETTING_BY_KEY = 
            "SELECT id, key, value, description, last_updated, updated_by FROM settings WHERE key = ?";
    private static final String SELECT_ALL_SETTINGS = 
            "SELECT id, key, value, description, last_updated, updated_by FROM settings";
    
    @Override
    public List<Setting> findAll() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Setting> settings = new ArrayList<>();
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ALL_SETTINGS);
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                settings.add(SettingMapper.map(resultSet));
            }
            
            return settings;
        } catch (SQLException e) {
            logger.error("Ошибка при получении всех настроек", e);
            return settings;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public Optional<Setting> findByKey(String key) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_SETTING_BY_KEY);
            statement.setString(1, key);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return Optional.of(SettingMapper.map(resultSet));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Ошибка при поиске настройки по ключу: {}", key, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public boolean save(Setting setting) {
        if (setting == null || setting.getKey() == null || setting.getKey().isEmpty()) {
            return false;
        }
        
        // Проверяем, существует ли настройка
        Optional<Setting> existingSetting = findByKey(setting.getKey());
        
        if (existingSetting.isPresent()) {
            return update(setting);
        } else {
            return insert(setting);
        }
    }
    
    /**
     * Вставляет новую настройку в базу данных
     * @param setting настройка для вставки
     * @return true если операция выполнена успешно
     */
    private boolean insert(Setting setting) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, INSERT_SETTING);
            
            statement.setString(1, setting.getKey());
            statement.setString(2, setting.getValue());
            statement.setString(3, setting.getDescription());
            statement.setTimestamp(4, java.sql.Timestamp.valueOf(setting.getLastUpdated()));
            
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при вставке настройки: {}", setting.getKey(), e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }
    
    /**
     * Обновляет существующую настройку в базе данных
     * @param setting настройка для обновления
     * @return true если операция выполнена успешно
     */
    private boolean update(Setting setting) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, UPDATE_SETTING);
            
            statement.setString(1, setting.getValue());
            statement.setString(2, setting.getDescription());
            statement.setTimestamp(3, java.sql.Timestamp.valueOf(setting.getLastUpdated()));
            statement.setString(4, setting.getKey());
            
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении настройки: {}", setting.getKey(), e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }
    
    @Override
    public boolean delete(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, DELETE_SETTING);
            statement.setString(1, key);
            
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при удалении настройки: {}", key, e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }
} 