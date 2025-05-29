package server.service;

import common.model.Card;
import common.model.Tier;
import common.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.db.dao.CardDao;
import server.db.dao.CardDaoImpl;
import server.db.dao.TierDao;
import server.db.dao.TierDaoImpl;
import server.db.dao.UserDao;
import server.db.dao.UserDaoImpl;

import java.util.List;
import java.util.Optional;

/**
 * Реализация сервиса для работы с картами лояльности
 */
public class CardServiceImpl implements CardService {
    private static final Logger logger = LoggerFactory.getLogger(CardServiceImpl.class);
    private final CardDao cardDao;
    private final UserDao userDao;
    private final TierDao tierDao;
    
    public CardServiceImpl() {
        this.cardDao = new CardDaoImpl();
        this.userDao = new UserDaoImpl();
        this.tierDao = new TierDaoImpl();
    }
    
    public CardServiceImpl(CardDao cardDao, UserDao userDao, TierDao tierDao) {
        this.cardDao = cardDao;
        this.userDao = userDao;
        this.tierDao = tierDao;
    }

    @Override
    public Card createCard(Long userId, String cardNumber) {
        logger.debug("Создание новой карты лояльности с номером {} для пользователя {}", cardNumber, userId);
        
        try {
            // Проверяем, существует ли карта с таким номером
            Optional<Card> existingCard = cardDao.findByNumber(cardNumber);
            if (existingCard.isPresent()) {
                logger.debug("Карта с номером {} уже существует", cardNumber);
                throw new IllegalArgumentException("Карта с таким номером уже существует");
            }
            logger.debug("Карта с номером {} не найдена, можно создать", cardNumber);
            
            // Проверяем, существует ли пользователь
            Optional<User> userOpt = userDao.findById(userId);
            if (userOpt.isEmpty()) {
                logger.debug("Пользователь с id {} не найден", userId);
                throw new IllegalArgumentException("Пользователь не найден");
            }
            logger.debug("Пользователь с id {} найден: {}", userId, userOpt.get().getLogin());
            
            // Получаем базовый уровень лояльности (с минимальным количеством баллов)
            logger.debug("Ищем базовый уровень лояльности для 0 баллов");
            List<Tier> allTiers = tierDao.findAll();
            logger.debug("Найдено {} уровней лояльности", allTiers.size());
            for (Tier tier : allTiers) {
                logger.debug("Уровень: level={}, name={}, minPoints={}", 
                    tier.getLevel(), tier.getName(), tier.getMinPoints());
            }
            
            Optional<Tier> baseLevel = tierDao.findTierForPoints(0);
            if (baseLevel.isEmpty()) {
                logger.error("Базовый уровень лояльности не найден");
                
                // Если не можем найти через findTierForPoints, попробуем вручную найти уровень с минимальными баллами
                if (!allTiers.isEmpty()) {
                    Tier lowestTier = allTiers.stream()
                            .min((t1, t2) -> Integer.compare(
                                t1.getMinPoints() != null ? t1.getMinPoints() : 0, 
                                t2.getMinPoints() != null ? t2.getMinPoints() : 0))
                            .orElse(null);
                    
                    if (lowestTier != null) {
                        logger.debug("Используем уровень с минимальными баллами как запасной вариант: {}", lowestTier.getName());
                        baseLevel = Optional.of(lowestTier);
                    } else {
                        throw new IllegalStateException("Базовый уровень лояльности не найден");
                    }
                } else {
                    throw new IllegalStateException("Базовый уровень лояльности не найден, список уровней пуст");
                }
            } else {
                logger.debug("Базовый уровень лояльности найден: {}", baseLevel.get().getName());
            }
            
            Card card = new Card();
            card.setCardNumber(cardNumber);
            card.setUserId(userId);
            card.setLevel(baseLevel.get().getLevel());
            card.setPoints(0);
            
            logger.debug("Сохраняем карту: номер={}, пользователь={}, уровень={}", 
                    card.getCardNumber(), userId, baseLevel.get().getName());
            Card createdCard = cardDao.save(card);
            logger.info("Создана новая карта лояльности: {}", createdCard.getCardNumber());
            
            return createdCard;
        } catch (Exception e) {
            logger.error("Ошибка при создании карты лояльности: {}", cardNumber, e);
            throw new RuntimeException("Ошибка при создании карты лояльности: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Card> getCardById(Long id) {
        logger.debug("Получение карты лояльности по id: {}", id);
        return cardDao.findById(id);
    }

    @Override
    public Optional<Card> getCardByNumber(String number) {
        logger.debug("Получение карты лояльности по номеру: {}", number);
        return cardDao.findByNumber(number);
    }

    @Override
    public List<Card> getCardsByUserId(Long userId) {
        logger.debug("Получение карт лояльности пользователя: {}", userId);
        return cardDao.findByUserId(userId);
    }

    @Override
    public List<Card> getAllCards() {
        logger.debug("Получение всех карт лояльности");
        return cardDao.findAll();
    }

    @Override
    public Optional<Card> addPoints(Long cardId, int points) {
        logger.debug("Начисление {} баллов на карту {}", points, cardId);
        
        if (points <= 0) {
            logger.debug("Количество баллов должно быть положительным");
            throw new IllegalArgumentException("Количество баллов должно быть положительным");
        }
        
        try {
            // Получаем карту
            Optional<Card> cardOpt = cardDao.findById(cardId);
            if (cardOpt.isEmpty()) {
                logger.debug("Карта с id {} не найдена", cardId);
                return Optional.empty();
            }
            
            Card card = cardOpt.get();
            int newPoints = card.getPoints() + points;
            
            // Обновляем количество баллов
            boolean updated = cardDao.updatePoints(cardId, newPoints);
            if (!updated) {
                logger.error("Не удалось обновить количество баллов для карты {}", cardId);
                return Optional.empty();
            }
            
            card.setPoints(newPoints);
            
            // Обновляем уровень карты, если нужно
            updateTierIfNeeded(card);
            
            logger.info("На карту {} начислено {} баллов. Текущий баланс: {}", 
                    card.getCardNumber(), points, newPoints);
            
            return Optional.of(card);
        } catch (Exception e) {
            logger.error("Ошибка при начислении баллов на карту {}: {}", cardId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Card> deductPoints(Long cardId, int points) {
        logger.debug("Списание {} баллов с карты {}", points, cardId);
        
        if (points <= 0) {
            logger.debug("Количество баллов должно быть положительным");
            throw new IllegalArgumentException("Количество баллов должно быть положительным");
        }
        
        try {
            // Получаем карту
            Optional<Card> cardOpt = cardDao.findById(cardId);
            if (cardOpt.isEmpty()) {
                logger.debug("Карта с id {} не найдена", cardId);
                return Optional.empty();
            }
            
            Card card = cardOpt.get();
            
            // Проверяем, достаточно ли баллов на карте
            if (card.getPoints() < points) {
                logger.debug("Недостаточно баллов на карте {}. Доступно: {}, требуется: {}", 
                        cardId, card.getPoints(), points);
                return Optional.empty();
            }
            
            int newPoints = card.getPoints() - points;
            
            // Обновляем количество баллов
            boolean updated = cardDao.updatePoints(cardId, newPoints);
            if (!updated) {
                logger.error("Не удалось обновить количество баллов для карты {}", cardId);
                return Optional.empty();
            }
            
            card.setPoints(newPoints);
            
            // Обновляем уровень карты, если нужно
            updateTierIfNeeded(card);
            
            logger.info("С карты {} списано {} баллов. Текущий баланс: {}", 
                    card.getCardNumber(), points, newPoints);
            
            return Optional.of(card);
        } catch (Exception e) {
            logger.error("Ошибка при списании баллов с карты {}: {}", cardId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Card> updateTierBasedOnPoints(Long cardId) {
        logger.debug("Обновление уровня карты {} в соответствии с баллами", cardId);
        
        try {
            // Получаем карту
            Optional<Card> cardOpt = cardDao.findById(cardId);
            if (cardOpt.isEmpty()) {
                logger.debug("Карта с id {} не найдена", cardId);
                return Optional.empty();
            }
            
            Card card = cardOpt.get();
            
            // Обновляем уровень карты
            updateTierIfNeeded(card);
            
            return Optional.of(card);
        } catch (Exception e) {
            logger.error("Ошибка при обновлении уровня карты {}: {}", cardId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Обновляет уровень карты в соответствии с количеством баллов, если необходимо
     * @param card карта
     * @return true, если уровень был обновлен
     */
    private boolean updateTierIfNeeded(Card card) {
        // Получаем уровень для текущего количества баллов
        Optional<Tier> tierForPointsOpt = tierDao.findTierForPoints(card.getPoints());
        if (tierForPointsOpt.isEmpty()) {
            logger.error("Не найден подходящий уровень для {} баллов", card.getPoints());
            return false;
        }
        
        Tier tierForPoints = tierForPointsOpt.get();
        
        // Если уровень не изменился, ничего не делаем
        if (tierForPoints.getLevel().equals(card.getLevel())) {
            return false;
        }
        
        // Обновляем уровень карты - используем level вместо ID
        boolean updated = cardDao.updateTier(card.getId(), tierForPoints.getLevel().longValue());
        if (updated) {
            logger.info("Уровень карты {} обновлен с {} на {}", 
                    card.getCardNumber(), card.getLevel(), tierForPoints.getLevel());
            card.setLevel(tierForPoints.getLevel());
            return true;
        } else {
            logger.error("Не удалось обновить уровень карты {}", card.getCardNumber());
            return false;
        }
    }

    @Override
    public boolean deleteCard(Long id) {
        logger.debug("Удаление карты лояльности с id: {}", id);
        
        try {
            boolean deleted = cardDao.deleteById(id);
            if (deleted) {
                logger.info("Карта лояльности удалена: {}", id);
            } else {
                logger.error("Не удалось удалить карту лояльности: {}", id);
            }
            
            return deleted;
        } catch (Exception e) {
            logger.error("Ошибка при удалении карты лояльности: {}", id, e);
            return false;
        }
    }
} 