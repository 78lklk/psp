package server.db.dao;

import common.model.Promotion;
import server.db.mapper.PromotionMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Реализация DAO для работы с акциями в базе данных
 */
public class PromotionDaoImpl extends AbstractDao implements PromotionDao {
    private static final String SELECT_BY_ID = 
            "SELECT id, name, description, start_date, end_date, is_active, bonus_pct, bonus_points " +
            "FROM promotions WHERE id = ?";
    private static final String SELECT_ALL = 
            "SELECT id, name, description, start_date, end_date, is_active, bonus_pct, bonus_points " +
            "FROM promotions";
    private static final String SELECT_ACTIVE = 
            "SELECT id, name, description, start_date, end_date, is_active, bonus_pct, bonus_points " +
            "FROM promotions WHERE is_active = TRUE";
    private static final String SELECT_ACTIVE_ON_DATE = 
            "SELECT id, name, description, start_date, end_date, is_active, bonus_pct, bonus_points " +
            "FROM promotions WHERE is_active = TRUE AND " +
            "(start_date IS NULL OR start_date <= ?) AND (end_date IS NULL OR end_date >= ?)";
    private static final String INSERT = 
            "INSERT INTO promotions (name, description, start_date, end_date, is_active, bonus_pct, bonus_points) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = 
            "UPDATE promotions SET name = ?, description = ?, start_date = ?, end_date = ?, " +
            "is_active = ?, bonus_pct = ?, bonus_points = ? WHERE id = ?";
    private static final String DELETE = 
            "DELETE FROM promotions WHERE id = ?";
    
    @Override
    public Optional<Promotion> findById(Long id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_BY_ID);
            statement.setLong(1, id);
            
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                Promotion promotion = PromotionMapper.mapResultSetToPromotion(resultSet);
                return Optional.of(promotion);
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Ошибка при получении акции по ID: {}", id, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public List<Promotion> findAll() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ALL);
            
            resultSet = statement.executeQuery();
            
            List<Promotion> promotions = new ArrayList<>();
            while (resultSet.next()) {
                Promotion promotion = PromotionMapper.mapResultSetToPromotion(resultSet);
                promotions.add(promotion);
            }
            
            return promotions;
        } catch (SQLException e) {
            logger.error("Ошибка при получении всех акций", e);
            return new ArrayList<>();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public List<Promotion> findActive() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ACTIVE);
            
            resultSet = statement.executeQuery();
            
            List<Promotion> promotions = new ArrayList<>();
            while (resultSet.next()) {
                Promotion promotion = PromotionMapper.mapResultSetToPromotion(resultSet);
                promotions.add(promotion);
            }
            
            return promotions;
        } catch (SQLException e) {
            logger.error("Ошибка при получении активных акций", e);
            return new ArrayList<>();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public List<Promotion> findActiveOnDate(LocalDate date) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ACTIVE_ON_DATE);
            statement.setObject(1, date);
            statement.setObject(2, date);
            
            resultSet = statement.executeQuery();
            
            List<Promotion> promotions = new ArrayList<>();
            while (resultSet.next()) {
                Promotion promotion = PromotionMapper.mapResultSetToPromotion(resultSet);
                promotions.add(promotion);
            }
            
            return promotions;
        } catch (SQLException e) {
            logger.error("Ошибка при получении акций, активных на дату: {}", date, e);
            return new ArrayList<>();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public Long insert(Promotion promotion) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatementWithGeneratedKeys(connection, INSERT);
            
            statement.setString(1, promotion.getName());
            statement.setString(2, promotion.getDescription());
            statement.setObject(3, promotion.getStartDate());
            statement.setObject(4, promotion.getEndDate());
            statement.setBoolean(5, promotion.isActive());
            
            if (promotion.getBonusPercent() != null) {
                statement.setInt(6, promotion.getBonusPercent());
            } else {
                statement.setNull(6, java.sql.Types.INTEGER);
            }
            
            if (promotion.getBonusPoints() != null) {
                statement.setInt(7, promotion.getBonusPoints());
            } else {
                statement.setNull(7, java.sql.Types.INTEGER);
            }
            
            int affectedRows = statement.executeUpdate();
            
            if (affectedRows == 0) {
                logger.error("Не удалось создать акцию, ни одна строка не была добавлена");
                return null;
            }
            
            resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            } else {
                logger.error("Не удалось получить ID созданной акции");
                return null;
            }
        } catch (SQLException e) {
            logger.error("Ошибка при создании акции", e);
            return null;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public boolean update(Promotion promotion) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, UPDATE);
            
            statement.setString(1, promotion.getName());
            statement.setString(2, promotion.getDescription());
            statement.setObject(3, promotion.getStartDate());
            statement.setObject(4, promotion.getEndDate());
            statement.setBoolean(5, promotion.isActive());
            
            if (promotion.getBonusPercent() != null) {
                statement.setInt(6, promotion.getBonusPercent());
            } else {
                statement.setNull(6, java.sql.Types.INTEGER);
            }
            
            if (promotion.getBonusPoints() != null) {
                statement.setInt(7, promotion.getBonusPoints());
            } else {
                statement.setNull(7, java.sql.Types.INTEGER);
            }
            
            statement.setLong(8, promotion.getId());
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении акции с ID: {}", promotion.getId(), e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }
    
    @Override
    public boolean delete(Long id) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, DELETE);
            statement.setLong(1, id);
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при удалении акции с ID: {}", id, e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }
} 