package server.db.mapper;

import common.model.PromoCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Маппер для преобразования ResultSet в объекты PromoCode и обратно
 */
public class PromoCodeMapper {
    private static final Logger logger = LoggerFactory.getLogger(PromoCodeMapper.class);
    
    /**
     * Преобразует ResultSet в объект PromoCode
     * @param rs ResultSet с данными промокода
     * @return объект PromoCode
     * @throws SQLException при ошибке доступа к данным
     */
    public static PromoCode mapResultSetToPromoCode(ResultSet rs) throws SQLException {
        PromoCode promoCode = new PromoCode();
        
        promoCode.setId(rs.getLong("id"));
        promoCode.setCode(rs.getString("code"));
        promoCode.setPromotionId(rs.getLong("promotion_id"));
        
        // Пробуем разные варианты имени колонки для флага использования
        try {
            promoCode.setUsed(rs.getBoolean("is_used"));
        } catch (SQLException e) {
            try {
                promoCode.setUsed(rs.getBoolean("used"));
            } catch (SQLException e2) {
                logger.error("Не удалось найти колонку для флага использования промокода", e2);
                // Устанавливаем значение по умолчанию
                promoCode.setUsed(false);
        }
        }
        
        // Для полей, которые могут быть NULL
        try {
            long usedBy = rs.getLong("used_by");
        if (!rs.wasNull()) {
                promoCode.setUsedBy(String.valueOf(usedBy));
            }
        } catch (SQLException e) {
            logger.debug("Колонка used_by не найдена", e);
        }
        
        try {
            java.sql.Timestamp usedDate = rs.getTimestamp("used_date");
            if (usedDate != null) {
                promoCode.setUsedDate(usedDate.toLocalDateTime().toLocalDate());
        }
        } catch (SQLException e) {
            logger.debug("Колонка used_date не найдена", e);
        }
        
        try {
            java.sql.Timestamp expiryDate = rs.getTimestamp("expiry_date");
            if (expiryDate != null) {
                promoCode.setExpiryDate(expiryDate.toLocalDateTime().toLocalDate());
        }
        } catch (SQLException e) {
            logger.debug("Колонка expiry_date не найдена", e);
        }
        
        try {
            long createdBy = rs.getLong("created_by");
        if (!rs.wasNull()) {
                promoCode.setCreatedBy(String.valueOf(createdBy));
        }
        } catch (SQLException e) {
            logger.debug("Колонка created_by не найдена", e);
        }
        
        return promoCode;
    }
} 