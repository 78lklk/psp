package server.db.dao;

import common.model.AuditLog;
import server.db.mapper.AuditLogMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация DAO для работы с записями аудита в базе данных
 */
public class AuditLogDaoImpl extends AbstractDao implements AuditLogDao {
    private static final String SELECT_ALL = 
            "SELECT a.id, a.user_id, a.action_type, a.action_details, a.timestamp, a.ip_address, " +
            "a.target_entity, a.target_id, u.login as username, u.email, u.role_id, r.name as role_name " +
            "FROM audit_log a " +
            "LEFT JOIN users u ON a.user_id = u.id " +
            "LEFT JOIN roles r ON u.role_id = r.id " +
            "ORDER BY a.timestamp DESC";
    
    private static final String SELECT_BY_PERIOD = 
            "SELECT a.id, a.user_id, a.action_type, a.action_details, a.timestamp, a.ip_address, " +
            "a.target_entity, a.target_id, u.login as username, u.email, u.role_id, r.name as role_name " +
            "FROM audit_log a " +
            "LEFT JOIN users u ON a.user_id = u.id " +
            "LEFT JOIN roles r ON u.role_id = r.id " +
            "WHERE a.timestamp BETWEEN ? AND ? " +
            "ORDER BY a.timestamp DESC";
    
    private static final String SELECT_BY_USER_ID = 
            "SELECT a.id, a.user_id, a.action_type, a.action_details, a.timestamp, a.ip_address, " +
            "a.target_entity, a.target_id, u.login as username, u.email, u.role_id, r.name as role_name " +
            "FROM audit_log a " +
            "LEFT JOIN users u ON a.user_id = u.id " +
            "LEFT JOIN roles r ON u.role_id = r.id " +
            "WHERE a.user_id = ? " +
            "ORDER BY a.timestamp DESC";
    
    private static final String SELECT_BY_ACTION_TYPE = 
            "SELECT a.id, a.user_id, a.action_type, a.action_details, a.timestamp, a.ip_address, " +
            "a.target_entity, a.target_id, u.login as username, u.email, u.role_id, r.name as role_name " +
            "FROM audit_log a " +
            "LEFT JOIN users u ON a.user_id = u.id " +
            "LEFT JOIN roles r ON u.role_id = r.id " +
            "WHERE a.action_type = ? " +
            "ORDER BY a.timestamp DESC";
    
    private static final String INSERT = 
            "INSERT INTO audit_log (user_id, action_type, action_details, timestamp, ip_address, target_entity, target_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    
    @Override
    public List<AuditLog> findAll() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ALL);
            
            resultSet = statement.executeQuery();
            
            List<AuditLog> logs = new ArrayList<>();
            while (resultSet.next()) {
                logs.add(AuditLogMapper.mapResultSetToAuditLog(resultSet));
            }
            
            return logs;
        } catch (SQLException e) {
            logger.error("Ошибка при получении всех записей аудита", e);
            return new ArrayList<>();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public List<AuditLog> findByPeriod(LocalDateTime from, LocalDateTime to) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_BY_PERIOD);
            statement.setTimestamp(1, Timestamp.valueOf(from));
            statement.setTimestamp(2, Timestamp.valueOf(to));
            
            resultSet = statement.executeQuery();
            
            List<AuditLog> logs = new ArrayList<>();
            while (resultSet.next()) {
                logs.add(AuditLogMapper.mapResultSetToAuditLog(resultSet));
            }
            
            return logs;
        } catch (SQLException e) {
            logger.error("Ошибка при получении записей аудита за период", e);
            return new ArrayList<>();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public List<AuditLog> findByUserId(Long userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_BY_USER_ID);
            statement.setLong(1, userId);
            
            resultSet = statement.executeQuery();
            
            List<AuditLog> logs = new ArrayList<>();
            while (resultSet.next()) {
                logs.add(AuditLogMapper.mapResultSetToAuditLog(resultSet));
            }
            
            return logs;
        } catch (SQLException e) {
            logger.error("Ошибка при получении записей аудита для пользователя", e);
            return new ArrayList<>();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public List<AuditLog> findByActionType(String actionType) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_BY_ACTION_TYPE);
            statement.setString(1, actionType);
            
            resultSet = statement.executeQuery();
            
            List<AuditLog> logs = new ArrayList<>();
            while (resultSet.next()) {
                logs.add(AuditLogMapper.mapResultSetToAuditLog(resultSet));
            }
            
            return logs;
        } catch (SQLException e) {
            logger.error("Ошибка при получении записей аудита по типу действия", e);
            return new ArrayList<>();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public Long insert(AuditLog auditLog) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatementWithGeneratedKeys(connection, INSERT);
            
            if (auditLog.getUser() != null) {
                statement.setLong(1, auditLog.getUser().getId());
            } else {
                statement.setNull(1, java.sql.Types.BIGINT);
            }
            
            statement.setString(2, auditLog.getActionType());
            statement.setString(3, auditLog.getActionDetails());
            statement.setTimestamp(4, java.sql.Timestamp.valueOf(auditLog.getTimestamp()));
            statement.setString(5, auditLog.getIpAddress());
            
            if (auditLog.getTargetEntity() != null) {
                statement.setString(6, auditLog.getTargetEntity());
            } else {
                statement.setNull(6, java.sql.Types.VARCHAR);
            }
            
            if (auditLog.getTargetId() != null) {
                statement.setLong(7, auditLog.getTargetId());
            } else {
                statement.setNull(7, java.sql.Types.BIGINT);
            }
            
            int affectedRows = statement.executeUpdate();
            
            if (affectedRows == 0) {
                logger.error("Не удалось создать запись аудита, ни одна строка не была добавлена");
                return null;
            }
            
            resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            } else {
                logger.error("Не удалось получить ID созданной записи аудита");
                return null;
            }
        } catch (SQLException e) {
            logger.error("Ошибка при создании записи аудита", e);
            return null;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
} 