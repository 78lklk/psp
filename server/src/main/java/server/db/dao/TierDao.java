package server.db.dao;

import common.model.Tier;

import java.util.Optional;

/**
 * DAO для работы с уровнями лояльности
 */
public interface TierDao extends Dao<Tier, Long> {
    /**
     * Найти уровень по названию
     * @param name название уровня
     * @return уровень или пустой Optional, если не найден
     */
    Optional<Tier> findByName(String name);
    
    /**
     * Получить уровень для указанного количества баллов
     * @param points количество баллов
     * @return уровень, соответствующий указанному количеству баллов
     */
    Optional<Tier> findTierForPoints(int points);
} 