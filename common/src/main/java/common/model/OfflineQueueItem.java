package common.model;

import java.time.LocalDateTime;

/**
 * Модель элемента очереди офлайн-операций
 */
public class OfflineQueueItem {
    private Long id;
    private String payload;
    private LocalDateTime queuedAt;
    private LocalDateTime sentAt;
    private OfflineOperationType operationType;
    
    public OfflineQueueItem() {
    }
    
    public OfflineQueueItem(Long id, String payload, LocalDateTime queuedAt, LocalDateTime sentAt, OfflineOperationType operationType) {
        this.id = id;
        this.payload = payload;
        this.queuedAt = queuedAt;
        this.sentAt = sentAt;
        this.operationType = operationType;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public void setPayload(String payload) {
        this.payload = payload;
    }
    
    public LocalDateTime getQueuedAt() {
        return queuedAt;
    }
    
    public void setQueuedAt(LocalDateTime queuedAt) {
        this.queuedAt = queuedAt;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public OfflineOperationType getOperationType() {
        return operationType;
    }
    
    public void setOperationType(OfflineOperationType operationType) {
        this.operationType = operationType;
    }
    
    /**
     * Проверяет, отправлена ли операция
     * @return true, если операция уже отправлена на сервер
     */
    public boolean isSent() {
        return sentAt != null;
    }
    
    @Override
    public String toString() {
        return "OfflineQueueItem{" +
                "id=" + id +
                ", operationType=" + operationType +
                ", queuedAt=" + queuedAt +
                ", sentAt=" + sentAt +
                '}';
    }
    
    /**
     * Типы офлайн-операций
     */
    public enum OfflineOperationType {
        ADD_TRANSACTION("Добавление транзакции"),
        FINISH_SESSION("Завершение сессии"),
        ACTIVATE_PROMO("Активация промокода");
        
        private final String description;
        
        OfflineOperationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
} 