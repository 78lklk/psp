package server.service;

import common.model.Tier;

import java.util.List;
import java.util.Optional;

/**
 * Интерфейс сервиса для работы с уровнями лояльности
 */
public interface TierService {
    /**
     * Создать новый уровень лояльности
     * @param name название уровня
     * @param minPoints минимальное количество баллов
     * @param discountPercent процент скидки
     * @return созданный уровень
     */
    Tier createTier(String name, int minPoints, int discountPercent);
    
    /**
     * Получить уровень по ID
     * @param id ID уровня
     * @return уровень или пустой Optional, если не найден
     */
    Optional<Tier> getTierById(Long id);
    
    /**
     * Получить уровень по названию
     * @param name название уровня
     * @return уровень или пустой Optional, если не найден
     */
    Optional<Tier> getTierByName(String name);
    
    /**
     * Получить все уровни
     * @return список всех уровней
     */
    List<Tier> getAllTiers();
    
    /**
     * Получить уровень для указанного количества баллов
     * @param points количество баллов
     * @return уровень, соответствующий указанному количеству баллов
     */
    Optional<Tier> getTierForPoints(int points);
    
    /**
     * Обновить уровень
     * @param tier уровень
     * @return true, если уровень обновлен успешно
     */
    boolean updateTier(Tier tier);
    
    /**
     * Удалить уровень
     * @param id ID уровня
     * @return true, если уровень удален успешно
     */
    boolean deleteTier(Long id);
} 