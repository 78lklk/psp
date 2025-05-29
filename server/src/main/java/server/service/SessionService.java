package server.service;

import common.model.Session;
import common.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Интерфейс сервиса для работы с игровыми сессиями
 */
public interface SessionService {
    /**
     * Создать новую сессию
     * @param cardId ID карты
     * @param minutes длительность сессии в минутах
     * @return созданная сессия
     */
    Session createSession(Long cardId, int minutes);
    
    /**
     * Создать новую сессию с информацией о компьютере
     * @param cardId ID карты
     * @param minutes длительность сессии в минутах
     * @param computerInfo информация о компьютере
     * @return созданная сессия
     */
    Session createSession(Long cardId, int minutes, String computerInfo);
    
    /**
     * Создать новую сессию с информацией о компьютере, пользователе-сотруднике и IP-адресе
     * @param cardId ID карты
     * @param minutes длительность сессии в минутах
     * @param computerInfo информация о компьютере
     * @param staffUser пользователь-сотрудник, создавший сессию
     * @param ipAddress IP-адрес, с которого создана сессия
     * @return созданная сессия
     */
    Session createSession(Long cardId, int minutes, String computerInfo, User staffUser, String ipAddress);
    
    /**
     * Получить сессию по ID
     * @param id ID сессии
     * @return сессия или пустой Optional, если не найдена
     */
    Optional<Session> getSessionById(Long id);
    
    /**
     * Получить активные сессии карты
     * @param cardId ID карты
     * @return список активных сессий
     */
    List<Session> getActiveSessionsByCardId(Long cardId);
    
    /**
     * Получить сессии карты за указанный период
     * @param cardId ID карты
     * @param from начало периода
     * @param to конец периода
     * @return список сессий
     */
    List<Session> getSessionsByCardIdAndPeriod(Long cardId, LocalDateTime from, LocalDateTime to);
    
    /**
     * Получить все сессии
     * @return список всех сессий
     */
    List<Session> getAllSessions();
    
    /**
     * Завершить сессию
     * @param sessionId ID сессии
     * @return обновленная сессия или пустой Optional, если не найдена
     */
    Optional<Session> finishSession(Long sessionId);
    
    /**
     * Завершить сессию с информацией о пользователе-сотруднике и IP-адресе
     * @param sessionId ID сессии
     * @param staffUser пользователь-сотрудник, завершивший сессию
     * @param ipAddress IP-адрес, с которого завершена сессия
     * @return обновленная сессия или пустой Optional, если не найдена
     */
    Optional<Session> finishSession(Long sessionId, User staffUser, String ipAddress);
    
    /**
     * Удалить сессию
     * @param id ID сессии
     * @return true, если сессия успешно удалена
     */
    boolean deleteSession(Long id);
} 