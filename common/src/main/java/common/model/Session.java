package common.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Модель игровой сессии
 */
public class Session implements Serializable {
    private Long id;
    private Long userId;
    private Long cardId;
    private Long computerId;
    private User user;
    private Card card;
    private String computerInfo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer minutes;
    private Integer points;
    private String status;
    private String notes;
    
    public Session() {
    }
    
    public Session(Long id, User user, Card card, String computerInfo, LocalDateTime startTime, LocalDateTime endTime, Integer minutes, Integer points, String status, String notes) {
        this.id = id;
        this.user = user;
        this.userId = user != null ? user.getId() : null;
        this.card = card;
        this.cardId = card != null ? card.getId() : null;
        this.computerInfo = computerInfo;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minutes = minutes;
        this.points = points;
        this.status = status;
        this.notes = notes;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getCardId() {
        return cardId;
    }
    
    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }
    
    public Long getComputerId() {
        return computerId;
    }
    
    public void setComputerId(Long computerId) {
        this.computerId = computerId;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
        this.userId = user != null ? user.getId() : null;
    }
    
    public Card getCard() {
        return card;
    }
    
    public void setCard(Card card) {
        this.card = card;
        this.cardId = card != null ? card.getId() : null;
    }
    
    public String getComputerInfo() {
        return computerInfo;
    }
    
    public void setComputerInfo(String computerInfo) {
        this.computerInfo = computerInfo;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public Integer getMinutes() {
        return minutes;
    }
    
    public void setMinutes(Integer minutes) {
        this.minutes = minutes;
    }
    
    public Integer getPoints() {
        return points;
    }
    
    public void setPoints(Integer points) {
        this.points = points;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    /**
     * Calculates the remaining minutes in the session
     * @param currentTime current time
     * @return remaining minutes
     */
    public int getRemainingMinutes(LocalDateTime currentTime) {
        if (minutes == null || startTime == null) {
            return 0;
        }
        
        if (endTime != null) {
            return 0; // Session is already over
        }
        
        long elapsedMinutes = startTime.until(currentTime, ChronoUnit.MINUTES);
        return Math.max(0, minutes - (int) elapsedMinutes);
    }
    
    /**
     * Checks if the session is active
     * @return true if the session is active
     */
    public boolean isActive() {
        return endTime == null;
    }
    
    /**
     * Calculates the duration in minutes
     * @return duration in minutes
     */
    public int getDurationMinutes() {
        if (startTime == null) {
            return 0;
        }
        
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return (int) startTime.until(end, ChronoUnit.MINUTES);
    }
    
    /**
     * Get computer name based on computerId
     * @return computer name in format "ПК #X"
     */
    public String getComputerName() {
        if (computerInfo != null && !computerInfo.isEmpty()) {
            return computerInfo;
        }
        
        if (computerId != null) {
            return "ПК #" + computerId;
        }
        
        return "Неизвестно";
    }
    
    /**
     * Get computer number for database operations
     * @return computer number as integer
     */
    public Integer getComputerNumber() {
        if (computerId != null) {
            return computerId.intValue();
        }
        
        // Попытка извлечь номер из computerInfo
        if (computerInfo != null && !computerInfo.isEmpty()) {
            try {
                // Ищем числа в строке типа "ПК #1", "Компьютер 5", "PC-03" и т.д.
                String numbers = computerInfo.replaceAll("\\D+", "");
                if (!numbers.isEmpty()) {
                    return Integer.parseInt(numbers);
                }
            } catch (NumberFormatException e) {
                // Игнорируем ошибки парсинга
            }
        }
        
        return 1; // Значение по умолчанию
    }
    
    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", userId=" + userId +
                ", cardId=" + cardId +
                ", computerId=" + computerId +
                ", user=" + (user != null ? user.getUsername() : "null") +
                ", card=" + (card != null ? card.getNumber() : "null") +
                ", computerInfo='" + computerInfo + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", minutes=" + minutes +
                ", points=" + points +
                ", status='" + status + '\'' +
                '}';
    }
} 