package server.service;

import common.model.Promotion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.db.DatabaseConfig;
import server.db.dao.PromotionDao;
import server.db.dao.PromotionDaoImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Реализация сервиса для работы с акциями с использованием БД
 */
public class PromotionServiceImpl implements PromotionService {
    private static final Logger logger = LoggerFactory.getLogger(PromotionServiceImpl.class);
    private final PromotionDao promotionDao;
    
    public PromotionServiceImpl() {
        this.promotionDao = new PromotionDaoImpl();
    }
    
    // Конструктор для тестирования с моком DAO
    public PromotionServiceImpl(PromotionDao promotionDao) {
        this.promotionDao = promotionDao;
    }
    
    @Override
    public List<Promotion> getAllPromotions() {
        logger.debug("Получение всех акций");
        try {
            return promotionDao.findAll();
        } catch (Exception e) {
            logger.error("Ошибка при получении всех акций из БД", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Promotion> getActivePromotions() {
        logger.debug("Получение активных акций");
        try {
            return promotionDao.findActive();
        } catch (Exception e) {
            logger.error("Ошибка при получении активных акций из БД", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Promotion> getPromotionsActiveOnDate(LocalDate date) {
        logger.debug("Получение акций, активных на дату: {}", date);
        if (date == null) {
            date = LocalDate.now();
        }
        try {
            return promotionDao.findActiveOnDate(date);
        } catch (Exception e) {
            logger.error("Ошибка при получении акций, активных на дату: {} из БД", date, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public Optional<Promotion> getPromotionById(Long id) {
        logger.debug("Получение акции по ID: {}", id);
        
        if (id == null) {
            return Optional.empty();
        }
        
        try {
            return promotionDao.findById(id);
        } catch (Exception e) {
            logger.error("Ошибка при получении акции по ID из БД: {}", id, e);
            return Optional.empty();
        }
    }
    
    @Override
    public Promotion createPromotion(Promotion promotion) {
        logger.debug("Создание новой акции: {}", promotion.getName());
        
        if (promotion == null) {
            logger.error("Не удалось создать акцию: объект null");
            return null;
        }
        
        try {
            Long id = promotionDao.insert(promotion);
            if (id != null && id > 0) {
                promotion.setId(id);
        return promotion;
            } else {
                logger.error("Не удалось создать акцию в БД: {}", promotion.getName());
                return null;
            }
        } catch (Exception e) {
            logger.error("Ошибка при создании акции в БД: {}", promotion.getName(), e);
            return null;
        }
    }
    
    @Override
    public Optional<Promotion> updatePromotion(Long id, Promotion promotion) {
        logger.debug("Обновление акции с ID {}: {}", id, promotion.getName());
        
        if (id == null) {
            return Optional.empty();
        }
        
        try {
            Optional<Promotion> existingPromotion = promotionDao.findById(id);
        
        if (existingPromotion.isEmpty()) {
            logger.warn("Акция с ID {} не найдена", id);
            return Optional.empty();
        }
        
            promotion.setId(id);
            boolean updated = promotionDao.update(promotion);
            
            if (updated) {
        return Optional.of(promotion);
            } else {
                logger.error("Не удалось обновить акцию с ID: {}", id);
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Ошибка при обновлении акции с ID {} в БД", id, e);
            return Optional.empty();
        }
    }
    
    @Override
    public boolean deletePromotion(Long id) {
        logger.debug("Удаление акции с ID: {}", id);
        
        if (id == null) {
            return false;
        }
        
        try {
            return promotionDao.delete(id);
        } catch (Exception e) {
            logger.error("Ошибка при удалении акции с ID {} из БД", id, e);
            return false;
        }
    }
    
    @Override
    public boolean activatePromotion(Long id) {
        logger.debug("Активация акции с ID: {}", id);
        
        if (id == null) {
            return false;
        }
        
        try {
            // Получаем акцию из БД
            Optional<Promotion> promotionOpt = promotionDao.findById(id);
            if (promotionOpt.isEmpty()) {
                logger.warn("Акция с ID {} не найдена", id);
                return false;
            }
            
            // Устанавливаем флаг активности
            Promotion promotion = promotionOpt.get();
        promotion.setActive(true);
        
            // Обновляем в БД
            return promotionDao.update(promotion);
        } catch (Exception e) {
            logger.error("Ошибка при активации акции с ID {} в БД", id, e);
            return false;
        }
    }
    
    @Override
    public boolean deactivatePromotion(Long id) {
        logger.debug("Деактивация акции с ID: {}", id);
        
        if (id == null) {
            return false;
        }
        
        try {
            // Получаем акцию из БД
            Optional<Promotion> promotionOpt = promotionDao.findById(id);
            if (promotionOpt.isEmpty()) {
                logger.warn("Акция с ID {} не найдена", id);
                return false;
            }
            
            // Устанавливаем флаг активности
            Promotion promotion = promotionOpt.get();
        promotion.setActive(false);
        
            // Обновляем в БД
            return promotionDao.update(promotion);
        } catch (Exception e) {
            logger.error("Ошибка при деактивации акции с ID {} в БД", id, e);
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getPromotionStatistics() {
        logger.debug("Получение статистики по акциям и промокодам");
        
        Map<String, Object> statistics = new HashMap<>();
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Основная статистика по акциям
            String sql = """
                SELECT 
                    COUNT(*) as total_promotions,
                    COUNT(CASE WHEN p.is_active = true AND p.start_date <= CURRENT_DATE AND p.end_date >= CURRENT_DATE THEN 1 END) as active_promotions,
                    (SELECT COUNT(*) FROM promo_codes) as total_promo_codes,
                    (SELECT COUNT(*) FROM promo_codes WHERE is_used = true) as used_promo_codes,
                    COALESCE(SUM(p.bonus_points), 0) as total_bonus_points,
                    COALESCE(SUM(p.usage_count), 0) as total_usage_count
                FROM promotions p
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    statistics.put("totalPromotions", rs.getInt("total_promotions"));
                    statistics.put("activePromotions", rs.getInt("active_promotions"));
                    statistics.put("totalPromoCodes", rs.getInt("total_promo_codes"));
                    statistics.put("usedPromoCodes", rs.getInt("used_promo_codes"));
                    statistics.put("totalBonusPoints", rs.getInt("total_bonus_points"));
                    statistics.put("totalUsageCount", rs.getInt("total_usage_count"));
                }
            }
            
            // Активность по дням (последние 7 дней)
            Map<String, Integer> dailyActivityData = new HashMap<>();
            String dailyActivityQuery = """
                SELECT 
                    DATE(cp.activation_date) as activity_date,
                    COUNT(*) as activity_count
                FROM card_promotions cp
                WHERE cp.activation_date >= CURRENT_DATE - INTERVAL '7 days'
                GROUP BY DATE(cp.activation_date)
                ORDER BY activity_date
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(dailyActivityQuery);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    String date = rs.getDate("activity_date").toString();
                    int count = rs.getInt("activity_count");
                    dailyActivityData.put(date, count);
                }
            }
            statistics.put("dailyActivityData", dailyActivityData);
            
            // Использование промокодов по типам
            Map<String, Integer> promoCodeUsageData = new HashMap<>();
            String promoCodeUsageQuery = """
                SELECT 
                    p.name as promotion_name,
                    COUNT(pc.id) as usage_count
                FROM promo_codes pc
                JOIN promotions p ON pc.promotion_id = p.id
                WHERE pc.is_used = true
                GROUP BY p.id, p.name
                ORDER BY usage_count DESC
                LIMIT 10
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(promoCodeUsageQuery);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    String promotionName = rs.getString("promotion_name");
                    int usageCount = rs.getInt("usage_count");
                    promoCodeUsageData.put(promotionName, usageCount);
                }
            }
            statistics.put("promoCodeUsageData", promoCodeUsageData);
            
            // Популярность акций
            Map<String, Integer> promotionUsageData = new HashMap<>();
            String promotionUsageQuery = """
                SELECT 
                    p.name as promotion_name,
                    COUNT(cp.id) as participants_count
                FROM promotions p
                LEFT JOIN card_promotions cp ON cp.promotion_id = p.id
                WHERE p.is_active = true
                GROUP BY p.id, p.name
                ORDER BY participants_count DESC
                LIMIT 10
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(promotionUsageQuery);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    String promotionName = rs.getString("promotion_name");
                    int participantsCount = rs.getInt("participants_count");
                    promotionUsageData.put(promotionName, participantsCount);
                }
            }
            statistics.put("promotionUsageData", promotionUsageData);
            
            logger.debug("Статистика по акциям успешно получена");
            
        } catch (SQLException e) {
            logger.error("Ошибка при получении статистики по акциям из БД", e);
            // Возвращаем пустую статистику в случае ошибки
            statistics.put("totalPromotions", 0);
            statistics.put("activePromotions", 0);
            statistics.put("totalPromoCodes", 0);
            statistics.put("usedPromoCodes", 0);
            statistics.put("totalBonusPoints", 0);
            statistics.put("totalUsageCount", 0);
            statistics.put("dailyActivityData", new HashMap<>());
            statistics.put("promoCodeUsageData", new HashMap<>());
            statistics.put("promotionUsageData", new HashMap<>());
        }
        
        return statistics;
    }
} 