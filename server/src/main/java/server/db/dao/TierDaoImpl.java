package server.db.dao;

import common.model.Tier;
import server.db.mapper.TierMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Реализация DAO для работы с уровнями лояльности
 */
public class TierDaoImpl extends AbstractDao implements TierDao {
    private static final String INSERT_TIER = 
            "INSERT INTO tiers (name, min_points, discount_pct) VALUES (?, ?, ?)";
    private static final String UPDATE_TIER = 
            "UPDATE tiers SET name = ?, min_points = ?, discount_pct = ? WHERE id = ?";
    private static final String DELETE_TIER = 
            "DELETE FROM tiers WHERE id = ?";
    private static final String SELECT_TIER_BY_ID = 
            "SELECT id, name, min_points, discount_pct FROM tiers WHERE id = ?";
    private static final String SELECT_TIER_BY_NAME = 
            "SELECT id, name, min_points, discount_pct FROM tiers WHERE name = ?";
    private static final String SELECT_ALL_TIERS = 
            "SELECT id, name, min_points, discount_pct FROM tiers ORDER BY min_points";
    private static final String SELECT_TIER_FOR_POINTS = 
            "SELECT id, name, min_points, discount_pct FROM tiers " +
            "WHERE min_points <= ? ORDER BY min_points DESC LIMIT 1";

    @Override
    public Optional<Tier> findById(Long id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_TIER_BY_ID);
            statement.setLong(1, id);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return Optional.of(TierMapper.map(resultSet));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Ошибка при поиске уровня по id: {}", id, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public Optional<Tier> findByName(String name) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_TIER_BY_NAME);
            statement.setString(1, name);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return Optional.of(TierMapper.map(resultSet));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Ошибка при поиске уровня по имени: {}", name, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public Optional<Tier> findTierForPoints(int points) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_TIER_FOR_POINTS);
            statement.setInt(1, points);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return Optional.of(TierMapper.map(resultSet));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Ошибка при поиске уровня для баллов: {}", points, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public List<Tier> findAll() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Tier> tiers = new ArrayList<>();
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ALL_TIERS);
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                tiers.add(TierMapper.map(resultSet));
            }
            
            return tiers;
        } catch (SQLException e) {
            logger.error("Ошибка при получении всех уровней", e);
            return tiers;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public Tier save(Tier tier) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatementWithGeneratedKeys(connection, INSERT_TIER);
            statement.setString(1, tier.getName());
            statement.setInt(2, tier.getMinPoints() != null ? tier.getMinPoints() : 0);
            
            // Calculate a discount percentage from the bonus multiplier (if available)
            int discountPct = 0;
            if (tier.getBonusMultiplier() != null) {
                discountPct = (int) ((tier.getBonusMultiplier() - 1.0) * 100);
            }
            statement.setInt(3, discountPct);
            
            long id = executeUpdateAndGetGeneratedKey(statement);
            // Since we're now using level instead of ID, we can set the level to the ID for simplicity
            // This is a workaround for backward compatibility
            if (tier.getLevel() == null) {
                tier.setLevel((int) id);
            }
            
            return tier;
        } catch (SQLException e) {
            logger.error("Ошибка при сохранении уровня: {}", tier.getName(), e);
            return tier;
        } finally {
            closeResources(null, statement, connection);
        }
    }

    @Override
    public boolean update(Tier tier) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, UPDATE_TIER);
            statement.setString(1, tier.getName());
            statement.setInt(2, tier.getMinPoints() != null ? tier.getMinPoints() : 0);
            
            // Calculate a discount percentage from the bonus multiplier (if available)
            int discountPct = 0;
            if (tier.getBonusMultiplier() != null) {
                discountPct = (int) ((tier.getBonusMultiplier() - 1.0) * 100);
            }
            statement.setInt(3, discountPct);
            
            // Use level as ID for database operations
            statement.setLong(4, tier.getLevel().longValue());
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении уровня: {}", tier.getLevel(), e);
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
            statement = prepareStatement(connection, DELETE_TIER);
            statement.setLong(1, id);
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при удалении уровня: {}", id, e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }
} 