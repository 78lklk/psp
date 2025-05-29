package common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Модель записи аудита
 */
public class AuditLog implements Serializable {
    private Long id;
    private User user;
    private String actionType;
    private String actionDetails;
    private String ipAddress;
    private LocalDateTime timestamp;
    private String targetEntity;
    private Long targetId;
    
    public AuditLog() {
    }
    
    public AuditLog(User user, String actionType, String actionDetails, String ipAddress) {
        this.user = user;
        this.actionType = actionType;
        this.actionDetails = actionDetails;
        this.ipAddress = ipAddress;
        this.timestamp = LocalDateTime.now();
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getActionType() {
        return actionType;
    }
    
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
    
    public String getActionDetails() {
        return actionDetails;
    }
    
    public void setActionDetails(String actionDetails) {
        this.actionDetails = actionDetails;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getTargetEntity() {
        return targetEntity;
    }
    
    public void setTargetEntity(String targetEntity) {
        this.targetEntity = targetEntity;
    }
    
    public Long getTargetId() {
        return targetId;
    }
    
    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }
    
    /**
     * Получает логин пользователя (для обратной совместимости)
     * @return логин пользователя
     */
    public String getLogin() {
        return user != null ? user.getLogin() : null;
    }
    
    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", user=" + (user != null ? user.getLogin() : "null") +
                ", actionType='" + actionType + '\'' +
                ", actionDetails='" + actionDetails + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", timestamp=" + timestamp +
                ", targetEntity='" + targetEntity + '\'' +
                ", targetId=" + targetId +
                '}';
    }
} 