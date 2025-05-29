package server.service;

import common.model.Card;
import common.model.Session;
import common.model.User;
import common.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.db.dao.CardDao;
import server.db.dao.CardDaoImpl;
import server.db.dao.SessionDao;
import server.db.dao.SessionDaoImpl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Реализация сервиса для работы с игровыми сессиями
 */
public class SessionServiceImpl implements SessionService {
    private static final Logger logger = LoggerFactory.getLogger(SessionServiceImpl.class);
    private final SessionDao sessionDao;
    private final CardDao cardDao;
    private final CardService cardService;
    private final AuditService auditService;
    
    public SessionServiceImpl() {
        this.sessionDao = new SessionDaoImpl();
        this.cardDao = new CardDaoImpl();
        this.cardService = new CardServiceImpl();
        this.auditService = new AuditServiceImpl();
    }
    
    public SessionServiceImpl(SessionDao sessionDao, CardDao cardDao, CardService cardService, AuditService auditService) {
        this.sessionDao = sessionDao;
        this.cardDao = cardDao;
        this.cardService = cardService;
        this.auditService = auditService;
    }

    @Override
    public Session createSession(Long cardId, int minutes) {
        return createSession(cardId, minutes, null, null, null);
    }
    
    @Override
    public Session createSession(Long cardId, int minutes, String computerInfo) {
        return createSession(cardId, minutes, computerInfo, null, null);
    }
    
    @Override
    public Session createSession(Long cardId, int minutes, String computerInfo, User staffUser, String ipAddress) {
        logger.debug("Создание новой сессии для карты {} продолжительностью {} минут на компьютере {}", cardId, minutes, computerInfo);
        
        try {
            // Проверяем, существует ли карта
            Optional<Card> cardOpt = cardDao.findById(cardId);
            if (cardOpt.isEmpty()) {
                logger.debug("Карта с id {} не найдена", cardId);
                throw new IllegalArgumentException("Карта не найдена");
            }
            
            // Создаем сессию
            Session session = new Session();
            session.setCard(cardOpt.get());
            session.setStartTime(LocalDateTime.now());
            session.setMinutes(minutes);
            session.setPoints(0); // Баллы будут начислены при завершении сессии
            session.setStatus("ACTIVE");
            
            if (computerInfo != null && !computerInfo.isEmpty()) {
                session.setComputerInfo(computerInfo);
            }
            
            // Сохраняем в БД только базовые поля, остальные храним в памяти
            Session createdSession = sessionDao.save(session);
            
            // Вручную копируем поля, не хранящиеся в БД
            createdSession.setComputerInfo(computerInfo);
            createdSession.setStatus("ACTIVE");
            
            logger.info("Создана новая сессия: {}", createdSession.getId());
            
            // Добавляем запись в аудит, если есть пользователь staff
            if (staffUser != null && auditService != null) {
                String actionDetails = String.format(
                    "Начата игровая сессия для карты %s на %d минут. Компьютер: %s",
                    cardOpt.get().getNumber(), minutes, computerInfo != null ? computerInfo : "не указан"
                );
                
                auditService.logAction(
                    staffUser, 
                    "CREATE_SESSION", 
                    actionDetails, 
                    ipAddress != null ? ipAddress : "unknown",
                    "Session",
                    createdSession.getId()
                );
                
                logger.debug("Добавлена запись в аудит о создании сессии пользователем {}", staffUser.getUsername());
            }
            
            return createdSession;
        } catch (Exception e) {
            logger.error("Ошибка при создании сессии для карты: {}", cardId, e);
            throw new RuntimeException("Ошибка при создании сессии", e);
        }
    }

    @Override
    public Optional<Session> getSessionById(Long id) {
        logger.debug("Получение сессии по id: {}", id);
        return sessionDao.findById(id);
    }

    @Override
    public List<Session> getActiveSessionsByCardId(Long cardId) {
        logger.debug("Получение активных сессий для карты: {}", cardId);
        return sessionDao.findActiveSessionsByCardId(cardId);
    }

    @Override
    public List<Session> getSessionsByCardIdAndPeriod(Long cardId, LocalDateTime from, LocalDateTime to) {
        logger.debug("Получение сессий для карты {} в период с {} по {}", cardId, from, to);
        return sessionDao.findSessionsByCardIdAndPeriod(cardId, from, to);
    }

    @Override
    public List<Session> getAllSessions() {
        logger.debug("Получение всех сессий");
        return sessionDao.findAll();
    }

    @Override
    public Optional<Session> finishSession(Long sessionId) {
        return finishSession(sessionId, null, null);
    }
    
    @Override
    public Optional<Session> finishSession(Long sessionId, User staffUser, String ipAddress) {
        logger.debug("Завершение сессии: {}", sessionId);
        
        try {
            // Получаем сессию
            Optional<Session> sessionOpt = sessionDao.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                logger.debug("Сессия с id {} не найдена", sessionId);
                return Optional.empty();
            }
            
            Session session = sessionOpt.get();
            
            // Проверяем, не завершена ли сессия уже
            if (session.getEndTime() != null) {
                logger.debug("Сессия {} уже завершена", sessionId);
                return Optional.of(session);
            }
            
            // Устанавливаем время окончания
            LocalDateTime endTime = LocalDateTime.now();
            session.setEndTime(endTime);
            session.setStatus("COMPLETED");
            
            // Вычисляем фактическое время сессии в минутах
            long actualMinutes = ChronoUnit.MINUTES.between(session.getStartTime(), endTime);
            if (actualMinutes > session.getMinutes()) {
                // Если клиент переиграл свое время, используем запланированное время
                actualMinutes = session.getMinutes();
            }
            
            // Начисляем баллы: 1 час = POINTS_PER_HOUR баллов
            int earnedPoints = (int) (actualMinutes * Constants.POINTS_PER_HOUR / 60);
            session.setPoints(earnedPoints);
            
            // Обновляем сессию
            boolean updated = sessionDao.finishSession(sessionId, endTime, earnedPoints);
            if (!updated) {
                logger.error("Не удалось обновить сессию {}", sessionId);
                return Optional.empty();
            }
            
            // Начисляем баллы на карту
            cardService.addPoints(session.getCard().getId(), earnedPoints);
            
            logger.info("Сессия {} завершена. Начислено {} баллов", sessionId, earnedPoints);
            
            // Добавляем запись в аудит, если есть пользователь staff
            if (staffUser != null && auditService != null) {
                String actionDetails = String.format(
                    "Завершена игровая сессия для карты %s. Длительность: %d минут. Начислено баллов: %d",
                    session.getCard().getNumber(), actualMinutes, earnedPoints
                );
                
                auditService.logAction(
                    staffUser, 
                    "FINISH_SESSION", 
                    actionDetails, 
                    ipAddress != null ? ipAddress : "unknown",
                    "Session",
                    sessionId
                );
                
                logger.debug("Добавлена запись в аудит о завершении сессии пользователем {}", staffUser.getUsername());
            }
            
            return Optional.of(session);
        } catch (Exception e) {
            logger.error("Ошибка при завершении сессии: {}", sessionId, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean deleteSession(Long id) {
        logger.debug("Удаление сессии с id: {}", id);
        
        try {
            boolean deleted = sessionDao.deleteById(id);
            if (deleted) {
                logger.info("Сессия удалена: {}", id);
            } else {
                logger.error("Не удалось удалить сессию: {}", id);
            }
            
            return deleted;
        } catch (Exception e) {
            logger.error("Ошибка при удалении сессии: {}", id, e);
            return false;
        }
    }
} 