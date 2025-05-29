package server.service;

import common.model.PromoCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.db.dao.PromoCodeDao;
import server.db.dao.PromoCodeDaoImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для работы с промокодами с хранением в базе данных
 */
public class PromoCodeServiceImpl implements PromoCodeService {
    private static final Logger logger = LoggerFactory.getLogger(PromoCodeServiceImpl.class);
    private final PromoCodeDao promoCodeDao;
    
    public PromoCodeServiceImpl() {
        this.promoCodeDao = new PromoCodeDaoImpl();
    }
    
    @Override
    public List<PromoCode> getAllPromoCodes() {
        logger.debug("Получение всех промокодов");
        try {
            return promoCodeDao.findAll();
        } catch (Exception e) {
            logger.error("Ошибка при получении всех промокодов из БД", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<PromoCode> getActivePromoCodes() {
        logger.debug("Получение активных промокодов");
        
        LocalDate now = LocalDate.now();
        
        try {
            return promoCodeDao.findActive();
        } catch (Exception e) {
            logger.error("Ошибка при получении активных промокодов из БД", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public Optional<PromoCode> getPromoCodeById(Long id) {
        logger.debug("Получение промокода по ID: {}", id);
        
        if (id == null) {
            return Optional.empty();
        }
        
        try {
            return promoCodeDao.findById(id);
        } catch (Exception e) {
            logger.error("Ошибка при получении промокода по ID из БД: {}", id, e);
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<PromoCode> getPromoCodeByCode(String code) {
        logger.debug("Получение промокода по коду: {}", code);
        
        if (code == null || code.isEmpty()) {
            return Optional.empty();
        }
        
        try {
            return promoCodeDao.findByCode(code);
        } catch (Exception e) {
            logger.error("Ошибка при получении промокода по коду из БД: {}", code, e);
            return Optional.empty();
        }
    }
    
    @Override
    public PromoCode createPromoCode(PromoCode promoCode) {
        logger.debug("Создание нового промокода: {}", promoCode.getCode());
        
        // Устанавливаем значения по умолчанию для обязательных полей
        if (promoCode.getPromotionId() == null) {
            // Промокод без привязки к акции - устанавливаем специальное значение
            promoCode.setPromotionId(0L); // 0 = независимый промокод
        }
        
        if (promoCode.getUsesCount() == null) {
            promoCode.setUsesCount(0);
        }
        
        if (promoCode.getCreatedBy() == null) {
            promoCode.setCreatedBy("admin");
        }
        
        // Устанавливаем isUsed в false для новых промокодов
        promoCode.setUsed(false);
        promoCode.setUsedBy(null);
        promoCode.setUsedDate(null);
        
        try {
            Long id = promoCodeDao.insert(promoCode);
            if (id != null && id > 0) {
                promoCode.setId(id);
                return promoCode;
            } else {
                logger.error("Не удалось создать промокод в БД: {}", promoCode.getCode());
                return null;
            }
        } catch (Exception e) {
            logger.error("Ошибка при создании промокода в БД: {}", promoCode.getCode(), e);
            return null;
        }
    }
    
    @Override
    public Optional<PromoCode> updatePromoCode(Long id, PromoCode promoCode) {
        logger.debug("Обновление промокода с ID: {}", id);
        
        if (id == null || promoCode == null) {
            return Optional.empty();
        }
        
        try {
            // Проверяем, существует ли промокод
            Optional<PromoCode> existingPromoCodeOpt = getPromoCodeById(id);
            if (existingPromoCodeOpt.isEmpty()) {
                logger.warn("Промокод с ID {} не найден для обновления", id);
                return Optional.empty();
            }
            
            PromoCode existingPromoCode = existingPromoCodeOpt.get();
            
            // Устанавливаем ID в обновляемом объекте
            promoCode.setId(id);
            
            // Сохраняем обязательные поля из существующего промокода, если они не заданы
            if (promoCode.getPromotionId() == null) {
                promoCode.setPromotionId(existingPromoCode.getPromotionId());
            }
            
            if (promoCode.getCreatedBy() == null) {
                promoCode.setCreatedBy(existingPromoCode.getCreatedBy());
            }
            
            if (promoCode.getUsesCount() == null) {
                promoCode.setUsesCount(existingPromoCode.getUsesCount());
            }
            
            // Обновляем промокод
            boolean updated = promoCodeDao.update(promoCode);
            
            if (updated) {
                return Optional.of(promoCode);
            } else {
                logger.warn("Не удалось обновить промокод с ID: {}", id);
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Ошибка при обновлении промокода с ID: {}", id, e);
            return Optional.empty();
        }
    }
    
    @Override
    public boolean deletePromoCode(Long id) {
        logger.debug("Удаление промокода с ID: {}", id);
        
        if (id == null) {
            return false;
        }
        
        try {
            return promoCodeDao.delete(id);
        } catch (Exception e) {
            logger.error("Ошибка при удалении промокода с ID: {}", id, e);
            return false;
        }
    }
    
    @Override
    public boolean activatePromoCode(Long id) {
        logger.debug("Активация промокода с ID: {}", id);
        
        Optional<PromoCode> promoCodeOptional = getPromoCodeById(id);
        
        if (promoCodeOptional.isEmpty()) {
            logger.warn("Промокод с ID {} не найден", id);
            return false;
        }
        
        try {
            PromoCode promoCode = promoCodeOptional.get();
            promoCode.setActive(true);
            return promoCodeDao.update(promoCode);
        } catch (Exception e) {
            logger.error("Ошибка при активации промокода с ID: {}", id, e);
            return false;
        }
    }
    
    @Override
    public boolean deactivatePromoCode(Long id) {
        logger.debug("Деактивация промокода с ID: {}", id);
        
        Optional<PromoCode> promoCodeOptional = getPromoCodeById(id);
        
        if (promoCodeOptional.isEmpty()) {
            logger.warn("Промокод с ID {} не найден", id);
            return false;
        }
        
        try {
            PromoCode promoCode = promoCodeOptional.get();
            promoCode.setActive(false);
            return promoCodeDao.update(promoCode);
        } catch (Exception e) {
            logger.error("Ошибка при деактивации промокода с ID: {}", id, e);
            return false;
        }
    }
    
    @Override
    public Optional<PromoCode> incrementUsageCount(Long id) {
        logger.debug("Увеличение счетчика использований промокода с ID: {}", id);
        
        Optional<PromoCode> promoCodeOptional = getPromoCodeById(id);
        
        if (promoCodeOptional.isEmpty()) {
            logger.warn("Промокод с ID {} не найден", id);
            return Optional.empty();
        }
        
        PromoCode promoCode = promoCodeOptional.get();
        
        // Проверяем, можно ли использовать промокод
        LocalDate now = LocalDate.now();
        
        if (!promoCode.isActive()) {
            logger.warn("Промокод с ID {} неактивен", id);
            return Optional.empty();
        }
        
        if (promoCode.getExpiryDate() != null && promoCode.getExpiryDate().isBefore(now)) {
            logger.warn("Промокод с ID {} просрочен", id);
            return Optional.empty();
        }
        
        if (promoCode.getUsesLimit() != null && promoCode.getUsesCount() >= promoCode.getUsesLimit()) {
            logger.warn("Промокод с ID {} уже использован максимальное количество раз", id);
            return Optional.empty();
        }
        
        // Увеличиваем счетчик использований
        promoCode.setUsesCount(promoCode.getUsesCount() + 1);
        
        return Optional.of(promoCode);
    }
    
    @Override
    public String generateRandomCode() {
        // Генерируем случайный код длиной 8 символов из букв и цифр
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(chars.length());
            codeBuilder.append(chars.charAt(index));
        }
        
        return codeBuilder.toString();
    }
} 