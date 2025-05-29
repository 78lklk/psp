package server.service;

import common.model.Tier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.db.dao.TierDao;
import server.db.dao.TierDaoImpl;

import java.util.List;
import java.util.Optional;

/**
 * Реализация сервиса для работы с уровнями лояльности
 */
public class TierServiceImpl implements TierService {
    private static final Logger logger = LoggerFactory.getLogger(TierServiceImpl.class);
    private final TierDao tierDao;
    
    public TierServiceImpl() {
        this.tierDao = new TierDaoImpl();
    }
    
    public TierServiceImpl(TierDao tierDao) {
        this.tierDao = tierDao;
    }

    @Override
    public Tier createTier(String name, int minPoints, int discountPercent) {
        logger.debug("Создание нового уровня лояльности: {}", name);
        
        try {
            // Проверяем, существует ли уровень с таким названием
            Optional<Tier> existingTier = tierDao.findByName(name);
            if (existingTier.isPresent()) {
                logger.debug("Уровень с названием {} уже существует", name);
                throw new IllegalArgumentException("Уровень с таким названием уже существует");
            }
            
            // Calculate bonus multiplier from discount percentage (e.g. 10% discount = 1.1 multiplier)
            double bonusMultiplier = 1.0 + (discountPercent / 100.0);
            
            // Create tier with a level placeholder - will be set by DAO during save
            Tier tier = new Tier(null, name);
            tier.setMinPoints(minPoints);
            tier.setBonusMultiplier(bonusMultiplier);
            
            Tier createdTier = tierDao.save(tier);
            logger.info("Создан новый уровень лояльности: {}", createdTier.getName());
            
            return createdTier;
        } catch (Exception e) {
            logger.error("Ошибка при создании уровня лояльности: {}", name, e);
            throw new RuntimeException("Ошибка при создании уровня лояльности", e);
        }
    }

    @Override
    public Optional<Tier> getTierById(Long id) {
        logger.debug("Получение уровня лояльности по id: {}", id);
        return tierDao.findById(id);
    }

    @Override
    public Optional<Tier> getTierByName(String name) {
        logger.debug("Получение уровня лояльности по названию: {}", name);
        return tierDao.findByName(name);
    }

    @Override
    public List<Tier> getAllTiers() {
        logger.debug("Получение всех уровней лояльности");
        return tierDao.findAll();
    }

    @Override
    public Optional<Tier> getTierForPoints(int points) {
        logger.debug("Получение уровня лояльности для {} баллов", points);
        return tierDao.findTierForPoints(points);
    }

    @Override
    public boolean updateTier(Tier tier) {
        logger.debug("Обновление уровня лояльности: {}", tier.getLevel());
        
        try {
            // Проверяем, существует ли уровень с таким названием (не учитывая текущий)
            Optional<Tier> tierByNameOpt = tierDao.findByName(tier.getName());
            if (tierByNameOpt.isPresent() && !tierByNameOpt.get().getLevel().equals(tier.getLevel())) {
                logger.debug("Уровень с названием {} уже существует", tier.getName());
                return false;
            }
            
            boolean updated = tierDao.update(tier);
            if (updated) {
                logger.info("Уровень лояльности обновлен: {}", tier.getName());
            } else {
                logger.error("Не удалось обновить уровень лояльности: {}", tier.getName());
            }
            
            return updated;
        } catch (Exception e) {
            logger.error("Ошибка при обновлении уровня лояльности: {}", tier.getName(), e);
            return false;
        }
    }

    @Override
    public boolean deleteTier(Long id) {
        logger.debug("Удаление уровня лояльности с id: {}", id);
        
        try {
            Optional<Tier> tierOpt = tierDao.findById(id);
            if (tierOpt.isEmpty()) {
                logger.debug("Уровень с id {} не найден", id);
                return false;
            }
            
            boolean deleted = tierDao.deleteById(id);
            if (deleted) {
                logger.info("Уровень лояльности удален: {}", id);
            } else {
                logger.error("Не удалось удалить уровень лояльности: {}", id);
            }
            
            return deleted;
        } catch (Exception e) {
            logger.error("Ошибка при удалении уровня лояльности: {}", id, e);
            return false;
        }
    }
} 