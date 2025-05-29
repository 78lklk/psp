package server.service;

import common.model.Card;

import java.util.List;
import java.util.Optional;

/**
 * Интерфейс сервиса для работы с картами лояльности
 */
public interface CardService {
    /**
     * Создать новую карту
     * @param userId ID пользователя
     * @param cardNumber номер карты
     * @return созданная карта
     */
    Card createCard(Long userId, String cardNumber);
    
    /**
     * Получить карту по ID
     * @param id ID карты
     * @return карта или пустой Optional, если не найдена
     */
    Optional<Card> getCardById(Long id);
    
    /**
     * Получить карту по номеру
     * @param number номер карты
     * @return карта или пустой Optional, если не найдена
     */
    Optional<Card> getCardByNumber(String number);
    
    /**
     * Получить все карты пользователя
     * @param userId ID пользователя
     * @return список карт пользователя
     */
    List<Card> getCardsByUserId(Long userId);
    
    /**
     * Получить все карты
     * @return список всех карт
     */
    List<Card> getAllCards();
    
    /**
     * Начислить баллы на карту
     * @param cardId ID карты
     * @param points количество баллов
     * @return обновленная карта или пустой Optional, если операция не удалась
     */
    Optional<Card> addPoints(Long cardId, int points);
    
    /**
     * Списать баллы с карты
     * @param cardId ID карты
     * @param points количество баллов
     * @return обновленная карта или пустой Optional, если операция не удалась
     */
    Optional<Card> deductPoints(Long cardId, int points);
    
    /**
     * Обновить уровень карты в соответствии с текущим количеством баллов
     * @param cardId ID карты
     * @return обновленная карта или пустой Optional, если операция не удалась
     */
    Optional<Card> updateTierBasedOnPoints(Long cardId);
    
    /**
     * Удалить карту
     * @param id ID карты
     * @return true, если карта удалена успешно
     */
    boolean deleteCard(Long id);
} 