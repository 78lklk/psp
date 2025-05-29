package server.db.dao;

import common.model.Session;
import server.db.mapper.SessionMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Реализация DAO для работы с игровыми сессиями
 */
public class SessionDaoImpl extends AbstractDao implements SessionDao {
    private static final String UPDATE_SESSION = 
            "UPDATE sessions SET card_id = ?, start_time = ?, end_time = ?, minutes = ?, points_earned = ? WHERE id = ?";
    private static final String DELETE_SESSION = 
            "DELETE FROM sessions WHERE id = ?";
    private static final String SELECT_SESSION_BY_ID = 
            "SELECT s.id, s.card_id, s.user_id, s.start_time, s.end_time, s.minutes, s.points_earned, " +
            "c.number as card_number " +
            "FROM sessions s " +
            "JOIN cards c ON s.card_id = c.id " +
            "WHERE s.id = ?";
    private static final String SELECT_ACTIVE_SESSIONS_BY_CARD = 
            "SELECT s.id, s.card_id, s.user_id, s.start_time, s.end_time, s.minutes, s.points_earned, " +
            "c.number as card_number " +
            "FROM sessions s " +
            "JOIN cards c ON s.card_id = c.id " +
            "WHERE s.card_id = ? AND s.end_time IS NULL";
    private static final String SELECT_SESSIONS_BY_CARD_AND_PERIOD = 
            "SELECT s.id, s.card_id, s.user_id, s.start_time, s.end_time, s.minutes, s.points_earned, " +
            "c.number as card_number " +
            "FROM sessions s " +
            "JOIN cards c ON s.card_id = c.id " +
            "WHERE s.card_id = ? AND s.start_time >= ? AND (s.end_time <= ? OR s.end_time IS NULL)";
    private static final String SELECT_ALL_SESSIONS = 
            "SELECT s.id, s.card_id, s.user_id, s.start_time, s.end_time, s.minutes, s.points_earned, " +
            "c.number as card_number " +
            "FROM sessions s " +
            "JOIN cards c ON s.card_id = c.id";
    private static final String FINISH_SESSION = 
            "UPDATE sessions SET end_time = ?, points_earned = ? WHERE id = ?";

    @Override
    public Optional<Session> findById(Long id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_SESSION_BY_ID);
            statement.setLong(1, id);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return Optional.of(SessionMapper.map(resultSet));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Ошибка при поиске сессии по id: {}", id, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public List<Session> findActiveSessionsByCardId(Long cardId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Session> sessions = new ArrayList<>();
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ACTIVE_SESSIONS_BY_CARD);
            statement.setLong(1, cardId);
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                sessions.add(SessionMapper.map(resultSet));
            }
            
            return sessions;
        } catch (SQLException e) {
            logger.error("Ошибка при поиске активных сессий для карты: {}", cardId, e);
            return sessions;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public List<Session> findSessionsByCardIdAndPeriod(Long cardId, LocalDateTime from, LocalDateTime to) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Session> sessions = new ArrayList<>();
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_SESSIONS_BY_CARD_AND_PERIOD);
            statement.setLong(1, cardId);
            statement.setTimestamp(2, Timestamp.valueOf(from));
            statement.setTimestamp(3, Timestamp.valueOf(to));
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                sessions.add(SessionMapper.map(resultSet));
            }
            
            return sessions;
        } catch (SQLException e) {
            logger.error("Ошибка при поиске сессий для карты {} в период с {} по {}", cardId, from, to, e);
            return sessions;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public boolean finishSession(Long sessionId, LocalDateTime endTime, int points) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, FINISH_SESSION);
            statement.setTimestamp(1, Timestamp.valueOf(endTime));
            statement.setInt(2, points);
            statement.setLong(3, sessionId);
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при завершении сессии: {}", sessionId, e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }

    @Override
    public List<Session> findAll() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Session> sessions = new ArrayList<>();
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ALL_SESSIONS);
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                sessions.add(SessionMapper.map(resultSet));
            }
            
            return sessions;
        } catch (SQLException e) {
            logger.error("Ошибка при получении всех сессий", e);
            return sessions;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public Session save(Session session) {
        Connection connection = null;
        PreparedStatement statement = null;
        PreparedStatement selectStatement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            
            // Сначала получаем user_id по card_id
            selectStatement = prepareStatement(connection, "SELECT user_id FROM cards WHERE id = ?");
            selectStatement.setLong(1, session.getCard().getId());
            resultSet = selectStatement.executeQuery();
            
            if (!resultSet.next()) {
                throw new SQLException("Карта с ID " + session.getCard().getId() + " не найдена");
            }
            
            long userId = resultSet.getLong("user_id");
            closeResources(resultSet, selectStatement, null);
            
            // Теперь создаем сессию
            String insertSql = "INSERT INTO sessions (card_id, user_id, start_time, minutes, points_earned, computer_number, status) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')";
            statement = prepareStatementWithGeneratedKeys(connection, insertSql);
            statement.setLong(1, session.getCard().getId());
            statement.setLong(2, userId);
            statement.setTimestamp(3, Timestamp.valueOf(session.getStartTime()));
            statement.setInt(4, session.getMinutes());
            statement.setInt(5, session.getPoints() == null ? 0 : session.getPoints());
            statement.setInt(6, session.getComputerNumber() != null ? session.getComputerNumber() : 1);
            
            long id = executeUpdateAndGetGeneratedKey(statement);
            session.setId(id);
            
            return session;
        } catch (SQLException e) {
            logger.error("Ошибка при сохранении сессии для карты: {}", session.getCard().getId(), e);
            return session;
        } finally {
            closeResources(null, statement, connection);
        }
    }

    @Override
    public boolean update(Session session) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, UPDATE_SESSION);
            statement.setLong(1, session.getCard().getId());
            statement.setTimestamp(2, Timestamp.valueOf(session.getStartTime()));
            
            if (session.getEndTime() != null) {
                statement.setTimestamp(3, Timestamp.valueOf(session.getEndTime()));
            } else {
                statement.setNull(3, java.sql.Types.TIMESTAMP);
            }
            
            statement.setInt(4, session.getMinutes());
            statement.setInt(5, session.getPoints() == null ? 0 : session.getPoints());
            statement.setLong(6, session.getId());
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении сессии: {}", session.getId(), e);
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
            statement = prepareStatement(connection, DELETE_SESSION);
            statement.setLong(1, id);
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при удалении сессии: {}", id, e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }
} 