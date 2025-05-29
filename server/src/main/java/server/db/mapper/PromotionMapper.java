package server.db.mapper;

import common.model.Promotion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Маппер для преобразования результатов запросов в объекты акций
 */
public class PromotionMapper {
    private static final Logger logger = LoggerFactory.getLogger(PromotionMapper.class);
    
    /**
     * Преобразует результат запроса в объект акции
     * @param resultSet результат запроса
     * @return объект акции
     * @throws SQLException если произошла ошибка при работе с результатом запроса
     */
    public static Promotion mapResultSetToPromotion(ResultSet resultSet) throws SQLException {
        Promotion promotion = new Promotion();
        
        try {
            promotion.setId(resultSet.getLong("id"));
            promotion.setName(resultSet.getString("name"));
            promotion.setDescription(resultSet.getString("description"));
            
            // Преобразование дат (могут быть NULL)
            java.sql.Date startDate = resultSet.getDate("start_date");
            if (startDate != null) {
                promotion.setStartDate(startDate.toLocalDate());
            }
            
            java.sql.Date endDate = resultSet.getDate("end_date");
            if (endDate != null) {
                promotion.setEndDate(endDate.toLocalDate());
            }
            
            // Проверяем, есть ли поле is_active или active
            boolean isActive;
            try {
                isActive = resultSet.getBoolean("is_active");
            } catch (SQLException e) {
                try {
                    isActive = resultSet.getBoolean("active");
                } catch (SQLException e2) {
                    // Если ни одно поле не найдено, по умолчанию true
                    logger.warn("Ни одно из полей активности (is_active, active) не найдено, устанавливаем значение по умолчанию: true");
                    isActive = true;
                }
            }
            promotion.setActive(isActive);
            
            // Обрабатываем bonus_pct или bonus_percent
            Integer bonusPercent = null;
            try {
                bonusPercent = resultSet.getInt("bonus_pct");
                if (resultSet.wasNull()) {
                    bonusPercent = null;
                }
            } catch (SQLException e) {
                try {
                    bonusPercent = resultSet.getInt("bonus_percent");
                    if (resultSet.wasNull()) {
                        bonusPercent = null;
                    }
                } catch (SQLException e2) {
                    // Если ни одно поле не найдено, оставляем null
                    logger.warn("Ни одно из полей процента бонуса (bonus_pct, bonus_percent) не найдено");
                }
            }
            promotion.setBonusPercent(bonusPercent);
            
            // Обрабатываем bonus_points
            try {
                int bonusPoints = resultSet.getInt("bonus_points");
                if (!resultSet.wasNull()) {
                    promotion.setBonusPoints(bonusPoints);
                }
            } catch (SQLException e) {
                logger.warn("Поле bonus_points не найдено");
            }
            
        } catch (SQLException e) {
            logger.error("Ошибка при маппинге акции из ResultSet", e);
            throw e;
        }
        
        return promotion;
    }
} 