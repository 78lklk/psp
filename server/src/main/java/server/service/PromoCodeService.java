package server.service;

import common.model.PromoCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с промокодами
 */
public interface PromoCodeService {

    /**
     * Получает все промокоды
     * @return список всех промокодов
     */
    List<PromoCode> getAllPromoCodes();

    /**
     * Получает активные промокоды
     * @return список активных промокодов
     */
    List<PromoCode> getActivePromoCodes();

    /**
     * Получает промокод по идентификатору
     * @param id идентификатор промокода
     * @return промокод или пустой Optional, если не найден
     */
    Optional<PromoCode> getPromoCodeById(Long id);

    /**
     * Получает промокод по коду
     * @param code код промокода
     * @return промокод или пустой Optional, если не найден
     */
    Optional<PromoCode> getPromoCodeByCode(String code);

    /**
     * Создает новый промокод
     * @param promoCode промокод для создания
     * @return созданный промокод
     */
    PromoCode createPromoCode(PromoCode promoCode);

    /**
     * Обновляет существующий промокод
     * @param id идентификатор промокода
     * @param promoCode промокод с обновленными данными
     * @return обновленный промокод или пустой Optional, если промокод не найден
     */
    Optional<PromoCode> updatePromoCode(Long id, PromoCode promoCode);

    /**
     * Удаляет промокод по идентификатору
     * @param id идентификатор промокода
     * @return true, если промокод успешно удален, false в противном случае
     */
    boolean deletePromoCode(Long id);

    /**
     * Активирует промокод
     * @param id идентификатор промокода
     * @return true, если промокод успешно активирован
     */
    boolean activatePromoCode(Long id);

    /**
     * Деактивирует промокод
     * @param id идентификатор промокода
     * @return true, если промокод успешно деактивирован
     */
    boolean deactivatePromoCode(Long id);

    /**
     * Увеличивает счетчик использований промокода
     * @param id идентификатор промокода
     * @return обновленный промокод или пустой Optional, если промокод не найден
     */
    Optional<PromoCode> incrementUsageCount(Long id);

    /**
     * Генерирует случайный код промокода
     * @return сгенерированный код
     */
    String generateRandomCode();
} 