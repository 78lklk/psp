package server.db.dao;

import common.model.PromoCode;
import server.db.mapper.PromoCodeMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Реализация DAO для работы с промокодами в базе данных
 */
public class PromoCodeDaoImpl extends AbstractDao implements PromoCodeDao {
    private static final String SELECT_BY_ID = 
            "SELECT id, code, promotion_id, is_used, used_by, used_date, expiry_date, created_by " +
            "FROM promo_codes WHERE id = ?";
    private static final String SELECT_BY_CODE = 
            "SELECT id, code, promotion_id, is_used, used_by, used_date, expiry_date, created_by " +
            "FROM promo_codes WHERE code = ?";
    private static final String SELECT_ALL = 
            "SELECT id, code, promotion_id, is_used, used_by, used_date, expiry_date, created_by " +
            "FROM promo_codes";
    private static final String SELECT_ACTIVE = 
            "SELECT id, code, promotion_id, is_used, used_by, used_date, expiry_date, created_by " +
            "FROM promo_codes WHERE is_used = FALSE AND (expiry_date IS NULL OR expiry_date >= CURRENT_DATE)";
    private static final String INSERT = 
            "INSERT INTO promo_codes (code, promotion_id, is_used, used_by, used_date, expiry_date, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = 
            "UPDATE promo_codes SET code = ?, promotion_id = ?, is_used = ?, used_by = ?, " +
            "used_date = ?, expiry_date = ?, created_by = ? WHERE id = ?";
    private static final String DELETE = 
            "DELETE FROM promo_codes WHERE id = ?";
    
    @Override
    public Optional<PromoCode> findById(Long id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_BY_ID);
            statement.setLong(1, id);
            
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                PromoCode promoCode = PromoCodeMapper.mapResultSetToPromoCode(resultSet);
                return Optional.of(promoCode);
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Ошибка при получении промокода по ID: {}", id, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public Optional<PromoCode> findByCode(String code) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_BY_CODE);
            statement.setString(1, code);
            
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                PromoCode promoCode = PromoCodeMapper.mapResultSetToPromoCode(resultSet);
                return Optional.of(promoCode);
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Ошибка при получении промокода по коду: {}", code, e);
            return Optional.empty();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public List<PromoCode> findAll() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ALL);
            
            resultSet = statement.executeQuery();
            
            List<PromoCode> promoCodes = new ArrayList<>();
            while (resultSet.next()) {
                PromoCode promoCode = PromoCodeMapper.mapResultSetToPromoCode(resultSet);
                promoCodes.add(promoCode);
            }
            
            return promoCodes;
        } catch (SQLException e) {
            logger.error("Ошибка при получении всех промокодов", e);
            return new ArrayList<>();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public List<PromoCode> findActive() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, SELECT_ACTIVE);
            
            resultSet = statement.executeQuery();
            
            List<PromoCode> promoCodes = new ArrayList<>();
            while (resultSet.next()) {
                PromoCode promoCode = PromoCodeMapper.mapResultSetToPromoCode(resultSet);
                promoCodes.add(promoCode);
            }
            
            return promoCodes;
        } catch (SQLException e) {
            logger.error("Ошибка при получении активных промокодов", e);
            return new ArrayList<>();
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public Long insert(PromoCode promoCode) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = prepareStatementWithGeneratedKeys(connection, INSERT);
            
            statement.setString(1, promoCode.getCode());
            
            // Обрабатываем promotion_id как nullable поле
            if (promoCode.getPromotionId() != null && promoCode.getPromotionId() > 0) {
                statement.setLong(2, promoCode.getPromotionId());
            } else {
                statement.setNull(2, java.sql.Types.INTEGER);
            }
            
            statement.setBoolean(3, promoCode.isUsed());
            
            // used_by - это ID карты (integer), не строка
            if (promoCode.getUsedBy() != null && !promoCode.getUsedBy().isEmpty()) {
                try {
                    statement.setLong(4, Long.parseLong(promoCode.getUsedBy()));
                } catch (NumberFormatException e) {
                    statement.setNull(4, java.sql.Types.INTEGER);
                }
            } else {
                statement.setNull(4, java.sql.Types.INTEGER);
            }
            
            setNullableLocalDate(statement, 5, promoCode.getUsedDate());
            setNullableLocalDate(statement, 6, promoCode.getExpiryDate());
            
            // created_by - это ID пользователя (integer), не строка  
            if (promoCode.getCreatedBy() != null && !promoCode.getCreatedBy().isEmpty()) {
                try {
                    statement.setLong(7, Long.parseLong(promoCode.getCreatedBy()));
                } catch (NumberFormatException e) {
                    // Если это не число, попробуем найти пользователя по имени "admin"
                    if ("admin".equals(promoCode.getCreatedBy()) || "system".equals(promoCode.getCreatedBy())) {
                        statement.setLong(7, 1L); // ID администратора
                    } else {
                        statement.setNull(7, java.sql.Types.INTEGER);
                    }
                }
            } else {
                statement.setNull(7, java.sql.Types.INTEGER);
            }
            
            int affectedRows = statement.executeUpdate();
            
            if (affectedRows == 0) {
                logger.error("Не удалось создать промокод, ни одна строка не была добавлена");
                return null;
            }
            
            resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                return resultSet.getLong(1);
            } else {
                logger.error("Не удалось получить ID созданного промокода");
                return null;
            }
        } catch (SQLException e) {
            logger.error("Ошибка при создании промокода", e);
            return null;
        } finally {
            closeResources(resultSet, statement, connection);
        }
    }
    
    @Override
    public boolean update(PromoCode promoCode) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = prepareStatement(connection, UPDATE);
            
            statement.setString(1, promoCode.getCode());
            
            // Обрабатываем promotion_id как nullable поле
            if (promoCode.getPromotionId() != null && promoCode.getPromotionId() > 0) {
                statement.setLong(2, promoCode.getPromotionId());
            } else {
                statement.setNull(2, java.sql.Types.INTEGER);
            }
            
            statement.setBoolean(3, promoCode.isUsed());
            
            // used_by - это ID карты (integer), не строка
            if (promoCode.getUsedBy() != null && !promoCode.getUsedBy().isEmpty()) {
                try {
                    statement.setLong(4, Long.parseLong(promoCode.getUsedBy()));
                } catch (NumberFormatException e) {
                    statement.setNull(4, java.sql.Types.INTEGER);
                }
            } else {
                statement.setNull(4, java.sql.Types.INTEGER);
            }
            
            setNullableLocalDate(statement, 5, promoCode.getUsedDate());
            setNullableLocalDate(statement, 6, promoCode.getExpiryDate());
            
            // created_by - это ID пользователя (integer), не строка  
            if (promoCode.getCreatedBy() != null && !promoCode.getCreatedBy().isEmpty()) {
                try {
                    statement.setLong(7, Long.parseLong(promoCode.getCreatedBy()));
                } catch (NumberFormatException e) {
                    // Если это не число, попробуем найти пользователя по имени "admin"
                    if ("admin".equals(promoCode.getCreatedBy()) || "system".equals(promoCode.getCreatedBy())) {
                        statement.setLong(7, 1L); // ID администратора
                    } else {
                        statement.setNull(7, java.sql.Types.INTEGER);
                    }
                }
            } else {
                statement.setNull(7, java.sql.Types.INTEGER);
            }
            
            statement.setLong(8, promoCode.getId());
            
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении промокода с ID: {}", promoCode.getId(), e);
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
            logger.error("Ошибка при удалении промокода с ID: {}", id, e);
            return false;
        } finally {
            closeResources(null, statement, connection);
        }
    }
    
    // Вспомогательные методы для работы с NULL-значениями
    private void setNullableLocalDate(PreparedStatement statement, int index, java.time.LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DATE);
        } else {
            statement.setDate(index, java.sql.Date.valueOf(value));
        }
    }
} 