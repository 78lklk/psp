package server.db.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.db.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Абстрактный класс DAO с общими методами
 */
public abstract class AbstractDao {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    /**
     * Закрывает ресурсы базы данных
     * @param resultSet результат запроса
     * @param statement подготовленный запрос
     * @param connection соединение с базой данных
     */
    protected void closeResources(ResultSet resultSet, PreparedStatement statement, Connection connection) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Ошибка при закрытии ресурсов", e);
        }
    }
    
    /**
     * Получить соединение с базой данных
     * @return соединение с базой данных
     * @throws SQLException если не удалось получить соединение
     */
    protected Connection getConnection() throws SQLException {
        return DatabaseConfig.getConnection();
    }
    
    /**
     * Получить и подготовить запрос
     * @param connection соединение с базой данных
     * @param sql SQL-запрос
     * @return подготовленный запрос
     * @throws SQLException если не удалось подготовить запрос
     */
    protected PreparedStatement prepareStatement(Connection connection, String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }
    
    /**
     * Получить подготовленный запрос с возвращением сгенерированных ключей
     * @param connection соединение с базой данных
     * @param sql SQL-запрос
     * @return подготовленный запрос
     * @throws SQLException если не удалось подготовить запрос
     */
    protected PreparedStatement prepareStatementWithGeneratedKeys(Connection connection, String sql) throws SQLException {
        return connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
    }
    
    /**
     * Выполнить запрос и получить сгенерированный ключ
     * @param statement подготовленный запрос
     * @return сгенерированный ключ или -1, если ключ не был сгенерирован
     * @throws SQLException если не удалось выполнить запрос или получить ключ
     */
    protected long executeUpdateAndGetGeneratedKey(PreparedStatement statement) throws SQLException {
        int affectedRows = statement.executeUpdate();
        if (affectedRows == 0) {
            throw new SQLException("Не удалось создать запись, ни одна строка не была изменена");
        }
        
        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                return generatedKeys.getLong(1);
            } else {
                return -1;
            }
        }
    }
} 