package server.db.dao;

import common.model.Promotion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * DAO для работы с акциями
 */
public interface PromotionDao {
    
    /**
     * Получает акцию по идентификатору
     * @param id идентификатор акции
     * @return акция или пустой Optional
     */
    Optional<Promotion> findById(Long id);
    
    /**
     * Получает все акции
     * @return список всех акций
     */
    List<Promotion> findAll();
    
    /**
     * Получает все активные акции
     * @return список активных акций
     */
    List<Promotion> findActive();
    
    /**
     * Получает все акции, активные на указанную дату
     * @param date дата
     * @return список акций
     */
    List<Promotion> findActiveOnDate(LocalDate date);
    
    /**
     * Сохраняет новую акцию
     * @param promotion акция для сохранения
     * @return идентификатор созданной акции или null в случае ошибки
     */
    Long insert(Promotion promotion);
    
    /**
     * Обновляет существующую акцию
     * @param promotion акция с обновленными данными
     * @return true, если обновление выполнено успешно
     */
    boolean update(Promotion promotion);
    
    /**
     * Удаляет акцию
     * @param id идентификатор акции для удаления
     * @return true, если удаление выполнено успешно
     */
    boolean delete(Long id);
} 