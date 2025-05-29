package server.db.dao;

import common.model.Card;
import server.db.mapper.CardMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Реализация DAO для работы с картами лояльности
 */
public class CardDaoImpl extends AbstractDao implements CardDao {
    private static final String INSERT_CARD = 
            "INSERT INTO cards (number, user_id, tier_id, points) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_CARD = 
            "UPDATE cards SET number = ?, user_id = ?, tier_id = ?, points = ? WHERE id = ?";
    private static final String DELETE_CARD = 
            "DELETE FROM cards WHERE id = ?";
    private static final String SELECT_CARD_BY_ID = 
            "SELECT c.id, c.number, c.points, " +
            "c.user_id, u.login, " +
            "c.tier_id, t.name as tier_name, t.min_points, t.discount_pct " +
            "FROM cards c " +
            "LEFT JOIN users u ON c.user_id = u.id " +
            "LEFT JOIN tiers t ON c.tier_id = t.id " +
            "WHERE c.id = ?";
    private static final String SELECT_CARD_BY_NUMBER = 
            "SELECT c.id, c.number, c.points, " +
            "c.user_id, u.login, " +
            "c.tier_id, t.name as tier_name, t.min_points, t.discount_pct " +
            "FROM cards c " +
            "LEFT JOIN users u ON c.user_id = u.id " +
            "LEFT JOIN tiers t ON c.tier_id = t.id " +
            "WHERE c.number = ?";
    private static final String SELECT_CARDS_BY_USER_ID = 
            "SELECT c.id, c.number, c.points, " +
            "c.user_id, u.login, " +
            "c.tier_id, t.name as tier_name, t.min_points, t.discount_pct " +
            "FROM cards c " +
            "LEFT JOIN users u ON c.user_id = u.id " +
            "LEFT JOIN tiers t ON c.tier_id = t.id " +
            "WHERE c.user_id = ?";
    private static final String SELECT_ALL_CARDS = 
            "SELECT c.id, c.number, c.points, " +
            "c.user_id, u.login, " +
            "c.tier_id, t.name as tier_name, t.min_points, t.discount_pct " +
            "FROM cards c " +
            "LEFT JOIN users u ON c.user_id = u.id " +
            "LEFT JOIN tiers t ON c.tier_id = t.id";
    private static final String UPDATE_CARD_POINTS = 
            "UPDATE cards SET points = ? WHERE id = ?";
    private static final String UPDATE_CARD_TIER = 
            "UPDATE cards SET tier_id = ? WHERE id = ?";

    @Override
    public Optional<Card> findById(Long id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_CARD_BY_ID);
            statement.setLong(1, id);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return Optional.of(CardMapper.map(resultSet));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Ошибка при поиске карты по id: {}", id, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public Optional<Card> findByNumber(String number) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_CARD_BY_NUMBER);
            statement.setString(1, number);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return Optional.of(CardMapper.map(resultSet));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Ошибка при поиске карты по номеру: {}", number, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public List<Card> findByUserId(Long userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Card> cards = new ArrayList<>();
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_CARDS_BY_USER_ID);
            statement.setLong(1, userId);
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                cards.add(CardMapper.map(resultSet));
            }
            
            return cards;
        } catch (SQLException e) {
            logger.error("Ошибка при поиске карт пользователя: {}", userId, e);
            return cards;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public List<Card> findAll() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Card> cards = new ArrayList<>();
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ALL_CARDS);
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                cards.add(CardMapper.map(resultSet));
            }
            
            return cards;
        } catch (SQLException e) {
            logger.error("Ошибка при получении всех карт", e);
            return cards;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public Card save(Card card) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatementWithGeneratedKeys(connection, INSERT_CARD);
            statement.setString(1, card.getCardNumber());
            statement.setLong(2, card.getUserId());
            
            // Get tier ID from the tier level or default to level 1
            int tierLevel = card.getLevel() != null ? card.getLevel() : 1;
            
            // We need to resolve the tier ID based on level - using tier 1 as default
            Long tierId = (long)tierLevel; // Simple mapping for now, should be replaced with actual tier lookup
            statement.setLong(3, tierId);
            statement.setInt(4, card.getPoints() != null ? card.getPoints() : 0);
            
            long id = executeUpdateAndGetGeneratedKey(statement);
            card.setId(id);
            
            return card;
        } catch (SQLException e) {
            logger.error("Ошибка при сохранении карты: {}", card.getCardNumber(), e);
            return card;
        } finally {
            closeResources(null, statement, connection);
        }
    }

    @Override
    public boolean update(Card card) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, UPDATE_CARD);
            statement.setString(1, card.getCardNumber());
            statement.setLong(2, card.getUserId());
            
            // Get tier ID from the tier level or default to level 1
            int tierLevel = card.getLevel() != null ? card.getLevel() : 1;
            
            // We need to resolve the tier ID based on level
            Long tierId = (long)tierLevel; // Simple mapping for now, should be replaced with actual tier lookup
            statement.setLong(3, tierId);
            statement.setInt(4, card.getPoints() != null ? card.getPoints() : 0);
            statement.setLong(5, card.getId());
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении карты: {}", card.getId(), e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }

    @Override
    public boolean updatePoints(Long cardId, int points) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, UPDATE_CARD_POINTS);
            statement.setInt(1, points);
            statement.setLong(2, cardId);
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении баллов карты: {}", cardId, e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }

    @Override
    public boolean updateTier(Long cardId, Long tierId) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, UPDATE_CARD_TIER);
            statement.setLong(1, tierId);
            statement.setLong(2, cardId);
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении уровня карты: {}", cardId, e);
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
            statement = prepareStatement(connection, DELETE_CARD);
            statement.setLong(1, id);
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при удалении карты: {}", id, e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }
} 