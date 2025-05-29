package server.db.dao;

import common.model.Session;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO для работы с игровыми сессиями
 */
public interface SessionDao extends Dao<Session, Long> {
    /**
     * Найти активные сессии карты
     * @param cardId ID карты
     * @return список активных сессий
     */
    List<Session> findActiveSessionsByCardId(Long cardId);
    
    /**
     * Найти сессии карты за указанный период
     * @param cardId ID карты
     * @param from начало периода
     * @param to конец периода
     * @return список сессий
     */
    List<Session> findSessionsByCardIdAndPeriod(Long cardId, LocalDateTime from, LocalDateTime to);
    
    /**
     * Завершить сессию
     * @param sessionId ID сессии
     * @param endTime время завершения
     * @param points начисленные баллы
     * @return true, если сессия успешно завершена
     */
    boolean finishSession(Long sessionId, LocalDateTime endTime, int points);
} 