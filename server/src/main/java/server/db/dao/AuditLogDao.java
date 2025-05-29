package server.db.dao;

import common.model.AuditLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DAO для работы с записями аудита
 */
public interface AuditLogDao {
    
    /**
     * Получает все записи аудита
     * @return список всех записей аудита
     */
    List<AuditLog> findAll();
    
    /**
     * Получает записи аудита за указанный период
     * @param from начало периода
     * @param to конец периода
     * @return список записей аудита
     */
    List<AuditLog> findByPeriod(LocalDateTime from, LocalDateTime to);
    
    /**
     * Получает записи аудита для указанного пользователя
     * @param userId идентификатор пользователя
     * @return список записей аудита
     */
    List<AuditLog> findByUserId(Long userId);
    
    /**
     * Получает записи аудита по типу действия
     * @param actionType тип действия
     * @return список записей аудита
     */
    List<AuditLog> findByActionType(String actionType);
    
    /**
     * Сохраняет новую запись аудита
     * @param auditLog запись аудита
     * @return идентификатор созданной записи или null в случае ошибки
     */
    Long insert(AuditLog auditLog);
} 