package server.service;

import common.model.AuditLog;
import common.model.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для работы с аудитом действий в системе
 */
public interface AuditService {

    /**
     * Записывает действие в лог аудита
     * @param user пользователь, выполнивший действие
     * @param actionType тип действия
     * @param actionDetails детали действия
     * @param ipAddress IP-адрес, с которого выполнено действие
     * @param targetEntity целевая сущность (опционально)
     * @param targetId ID целевой сущности (опционально)
     * @return запись аудита
     */
    AuditLog logAction(User user, String actionType, String actionDetails, String ipAddress, 
                       String targetEntity, Long targetId);

    /**
     * Записывает действие в лог аудита
     * @param user пользователь, выполнивший действие
     * @param actionType тип действия
     * @param actionDetails детали действия
     * @param ipAddress IP-адрес, с которого выполнено действие
     * @return запись аудита
     */
    AuditLog logAction(User user, String actionType, String actionDetails, String ipAddress);

    /**
     * Получает все записи аудита
     * @return список записей аудита
     */
    List<AuditLog> getAllAuditLogs();

    /**
     * Получает записи аудита за период
     * @param from начало периода
     * @param to конец периода
     * @return список записей аудита
     */
    List<AuditLog> getAuditLogsByPeriod(LocalDateTime from, LocalDateTime to);

    /**
     * Получает записи аудита для пользователя
     * @param userId ID пользователя
     * @return список записей аудита
     */
    List<AuditLog> getAuditLogsByUser(Long userId);

    /**
     * Получает записи аудита по типу действия
     * @param actionType тип действия
     * @return список записей аудита
     */
    List<AuditLog> getAuditLogsByActionType(String actionType);
} 