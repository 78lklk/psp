package server.db.dao;

import common.model.Card;

import java.util.List;
import java.util.Optional;

/**
 * DAO для работы с картами лояльности
 */
public interface CardDao extends Dao<Card, Long> {
    /**
     * Найти карту по номеру
     * @param number номер карты
     * @return карта или пустой Optional, если не найдена
     */
    Optional<Card> findByNumber(String number);
    
    /**
     * Получить все карты пользователя
     * @param userId ID пользователя
     * @return список карт пользователя
     */
    List<Card> findByUserId(Long userId);
    
    /**
     * Обновить количество баллов на карте
     * @param cardId ID карты
     * @param points новое количество баллов
     * @return true, если обновление успешно
     */
    boolean updatePoints(Long cardId, int points);
    
    /**
     * Обновить уровень карты
     * @param cardId ID карты
     * @param tierId ID уровня
     * @return true, если обновление успешно
     */
    boolean updateTier(Long cardId, Long tierId);
} 