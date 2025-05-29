package server.service;

import common.model.Promotion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с акциями
 */
public interface PromotionService {

    /**
     * Получает все акции
     * @return список всех акций
     */
    List<Promotion> getAllPromotions();

    /**
     * Получает активные акции
     * @return список активных акций
     */
    List<Promotion> getActivePromotions();

    /**
     * Получает акции, активные на указанную дату
     * @param date дата
     * @return список акций, активных на указанную дату
     */
    List<Promotion> getPromotionsActiveOnDate(LocalDate date);

    /**
     * Получает акцию по идентификатору
     * @param id идентификатор акции
     * @return акция или пустой Optional, если не найдена
     */
    Optional<Promotion> getPromotionById(Long id);

    /**
     * Создает новую акцию
     * @param promotion акция для создания
     * @return созданная акция
     */
    Promotion createPromotion(Promotion promotion);

    /**
     * Обновляет существующую акцию
     * @param id идентификатор акции
     * @param promotion акция с обновленными данными
     * @return обновленная акция или пустой Optional, если акция не найдена
     */
    Optional<Promotion> updatePromotion(Long id, Promotion promotion);

    /**
     * Удаляет акцию
     * @param id идентификатор акции
     * @return true, если акция успешно удалена
     */
    boolean deletePromotion(Long id);

    /**
     * Активирует акцию
     * @param id идентификатор акции
     * @return true, если акция успешно активирована
     */
    boolean activatePromotion(Long id);

    /**
     * Деактивирует акцию
     * @param id идентификатор акции
     * @return true, если акция успешно деактивирована
     */
    boolean deactivatePromotion(Long id);

    /**
     * Получает статистику по акциям и промокодам
     * @return статистика в виде Map с данными
     */
    java.util.Map<String, Object> getPromotionStatistics();
} 