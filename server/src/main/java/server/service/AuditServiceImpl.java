package server.service;

import common.model.AuditLog;
import common.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.db.dao.AuditLogDao;
import server.db.dao.AuditLogDaoImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Реализация сервиса аудита с хранением в БД
 */
public class AuditServiceImpl implements AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);
    private final AuditLogDao auditLogDao;
    
    public AuditServiceImpl() {
        this.auditLogDao = new AuditLogDaoImpl();
    }
    
    @Override
    public AuditLog logAction(User user, String actionType, String actionDetails, String ipAddress,
                             String targetEntity, Long targetId) {
        AuditLog auditLog = new AuditLog(user, actionType, actionDetails, ipAddress);
        auditLog.setTargetEntity(targetEntity);
        auditLog.setTargetId(targetId);
        
        try {
            Long id = auditLogDao.insert(auditLog);
            if (id != null) {
                auditLog.setId(id);
                logger.debug("Записано действие в аудит: {}", auditLog);
                return auditLog;
            } else {
                logger.error("Не удалось записать действие в аудит");
                return null;
            }
        } catch (Exception e) {
            logger.error("Ошибка при записи действия в аудит", e);
            return null;
        }
    }
    
    @Override
    public AuditLog logAction(User user, String actionType, String actionDetails, String ipAddress) {
        return logAction(user, actionType, actionDetails, ipAddress, null, null);
    }
    
    @Override
    public List<AuditLog> getAllAuditLogs() {
        logger.debug("Запрос всех записей аудита");
        try {
            return auditLogDao.findAll();
        } catch (Exception e) {
            logger.error("Ошибка при получении всех записей аудита", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<AuditLog> getAuditLogsByPeriod(LocalDateTime from, LocalDateTime to) {
        logger.debug("Запрос записей аудита за период с {} по {}", from, to);
        
        try {
            return auditLogDao.findByPeriod(from, to);
        } catch (Exception e) {
            logger.error("Ошибка при получении записей аудита за период", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<AuditLog> getAuditLogsByUser(Long userId) {
        logger.debug("Запрос записей аудита для пользователя {}", userId);
        
        try {
            return auditLogDao.findByUserId(userId);
        } catch (Exception e) {
            logger.error("Ошибка при получении записей аудита для пользователя", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<AuditLog> getAuditLogsByActionType(String actionType) {
        logger.debug("Запрос записей аудита по типу действия {}", actionType);
        
        try {
            return auditLogDao.findByActionType(actionType);
        } catch (Exception e) {
            logger.error("Ошибка при получении записей аудита по типу действия", e);
            return new ArrayList<>();
        }
    }
} 