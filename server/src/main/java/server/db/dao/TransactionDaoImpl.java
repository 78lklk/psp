package server.db.dao;

import common.model.Transaction;
import server.db.mapper.TransactionMapper;

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
 * Реализация DAO для работы с транзакциями
 */
public class TransactionDaoImpl extends AbstractDao implements TransactionDao {
    private static final String INSERT_TRANSACTION = 
            "INSERT INTO transactions (card_id, type, points, timestamp, description) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_TRANSACTION = 
            "UPDATE transactions SET card_id = ?, type = ?, points = ?, timestamp = ?, description = ? WHERE id = ?";
    private static final String DELETE_TRANSACTION = 
            "DELETE FROM transactions WHERE id = ?";
    private static final String SELECT_TRANSACTION_BY_ID = 
            "SELECT t.id, t.card_id, t.type, t.points, t.timestamp, t.description, " +
            "c.number as card_number " +
            "FROM transactions t " +
            "JOIN cards c ON t.card_id = c.id " +
            "WHERE t.id = ?";
    private static final String SELECT_TRANSACTIONS_BY_CARD = 
            "SELECT t.id, t.card_id, t.type, t.points, t.timestamp, t.description, " +
            "c.number as card_number " +
            "FROM transactions t " +
            "JOIN cards c ON t.card_id = c.id " +
            "WHERE t.card_id = ? " +
            "ORDER BY t.timestamp DESC";
    private static final String SELECT_TRANSACTIONS_BY_CARD_AND_PERIOD = 
            "SELECT t.id, t.card_id, t.type, t.points, t.timestamp, t.description, " +
            "c.number as card_number " +
            "FROM transactions t " +
            "JOIN cards c ON t.card_id = c.id " +
            "WHERE t.card_id = ? AND t.timestamp >= ? AND t.timestamp <= ? " +
            "ORDER BY t.timestamp DESC";
    private static final String SELECT_TRANSACTIONS_BY_TYPE = 
            "SELECT t.id, t.card_id, t.type, t.points, t.timestamp, t.description, " +
            "c.number as card_number " +
            "FROM transactions t " +
            "JOIN cards c ON t.card_id = c.id " +
            "WHERE t.type = ? " +
            "ORDER BY t.timestamp DESC";
    private static final String SELECT_ALL_TRANSACTIONS = 
            "SELECT t.id, t.card_id, t.type, t.points, t.timestamp, t.description, " +
            "c.number as card_number " +
            "FROM transactions t " +
            "JOIN cards c ON t.card_id = c.id " +
            "ORDER BY t.timestamp DESC";

    @Override
    public Optional<Transaction> findById(Long id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_TRANSACTION_BY_ID);
            statement.setLong(1, id);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return Optional.of(TransactionMapper.map(resultSet));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Ошибка при поиске транзакции по id: {}", id, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public List<Transaction> findByCardId(Long cardId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Transaction> transactions = new ArrayList<>();
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_TRANSACTIONS_BY_CARD);
            statement.setLong(1, cardId);
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                transactions.add(TransactionMapper.map(resultSet));
            }
            
            return transactions;
        } catch (SQLException e) {
            logger.error("Ошибка при поиске транзакций для карты: {}", cardId, e);
            return transactions;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public List<Transaction> findByCardIdAndPeriod(Long cardId, LocalDateTime from, LocalDateTime to) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Transaction> transactions = new ArrayList<>();
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_TRANSACTIONS_BY_CARD_AND_PERIOD);
            statement.setLong(1, cardId);
            statement.setTimestamp(2, Timestamp.valueOf(from));
            statement.setTimestamp(3, Timestamp.valueOf(to));
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                transactions.add(TransactionMapper.map(resultSet));
            }
            
            return transactions;
        } catch (SQLException e) {
            logger.error("Ошибка при поиске транзакций для карты {} в период с {} по {}", cardId, from, to, e);
            return transactions;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public List<Transaction> findByType(Transaction.Type type) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Transaction> transactions = new ArrayList<>();
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_TRANSACTIONS_BY_TYPE);
            statement.setString(1, type.name());
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                transactions.add(TransactionMapper.map(resultSet));
            }
            
            return transactions;
        } catch (SQLException e) {
            logger.error("Ошибка при поиске транзакций по типу: {}", type, e);
            return transactions;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public List<Transaction> findAll() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Transaction> transactions = new ArrayList<>();
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ALL_TRANSACTIONS);
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                transactions.add(TransactionMapper.map(resultSet));
            }
            
            return transactions;
        } catch (SQLException e) {
            logger.error("Ошибка при получении всех транзакций", e);
            return transactions;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public Transaction save(Transaction transaction) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatementWithGeneratedKeys(connection, INSERT_TRANSACTION);
            statement.setLong(1, transaction.getCard().getId());
            statement.setString(2, transaction.getType().name());
            statement.setInt(3, transaction.getPoints());
            statement.setTimestamp(4, Timestamp.valueOf(transaction.getTimestamp()));
            statement.setString(5, transaction.getDescription());
            
            long id = executeUpdateAndGetGeneratedKey(statement);
            transaction.setId(id);
            
            return transaction;
        } catch (SQLException e) {
            logger.error("Ошибка при сохранении транзакции для карты: {}", transaction.getCard().getId(), e);
            return transaction;
        } finally {
            closeResources(null, statement, connection);
        }
    }

    @Override
    public boolean update(Transaction transaction) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, UPDATE_TRANSACTION);
            statement.setLong(1, transaction.getCard().getId());
            statement.setString(2, transaction.getType().name());
            statement.setInt(3, transaction.getPoints());
            statement.setTimestamp(4, Timestamp.valueOf(transaction.getTimestamp()));
            statement.setString(5, transaction.getDescription());
            statement.setLong(6, transaction.getId());
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении транзакции: {}", transaction.getId(), e);
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
            statement = prepareStatement(connection, DELETE_TRANSACTION);
            statement.setLong(1, id);
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при удалении транзакции: {}", id, e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }
} 