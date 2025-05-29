package server.db.dao;

import common.model.PromoCode;

import java.util.List;
import java.util.Optional;

/**
 * DAO для работы с промокодами
 */
public interface PromoCodeDao {
    
    /**
     * Получает промокод по идентификатору
     * @param id идентификатор промокода
     * @return промокод или пустой Optional
     */
    Optional<PromoCode> findById(Long id);
    
    /**
     * Получает промокод по коду
     * @param code код промокода
     * @return промокод или пустой Optional
     */
    Optional<PromoCode> findByCode(String code);
    
    /**
     * Получает все промокоды
     * @return список всех промокодов
     */
    List<PromoCode> findAll();
    
    /**
     * Получает все активные промокоды
     * @return список активных промокодов
     */
    List<PromoCode> findActive();
    
    /**
     * Сохраняет новый промокод
     * @param promoCode промокод для сохранения
     * @return идентификатор созданного промокода или null в случае ошибки
     */
    Long insert(PromoCode promoCode);
    
    /**
     * Обновляет существующий промокод
     * @param promoCode промокод с обновленными данными
     * @return true, если обновление выполнено успешно
     */
    boolean update(PromoCode promoCode);
    
    /**
     * Удаляет промокод
     * @param id идентификатор промокода для удаления
     * @return true, если удаление выполнено успешно
     */
    boolean delete(Long id);
} 