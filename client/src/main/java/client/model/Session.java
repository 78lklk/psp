package client.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Модель сессии компьютерного клуба
 */
public class Session implements Serializable {
    private Long id;
    private Long cardId;
    private String computerName;
    private Integer computerNumber; // Номер компьютера для БД
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer minutes;
    private Integer points;
    private boolean active;
    
    // Дополнительные поля для отображения
    private String cardNumber;
    private String clientName;
    
    public Session() {
        this.active = true;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getCardId() {
        return cardId;
    }
    
    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }
    
    public String getComputerName() {
        return computerName;
    }
    
    public void setComputerName(String computerName) {
        this.computerName = computerName;
    }
    
    public Integer getComputerNumber() {
        return computerNumber;
    }
    
    public void setComputerNumber(Integer computerNumber) {
        this.computerNumber = computerNumber;
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
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public String getCardNumber() {
        return cardNumber;
    }
    
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    public String getClientName() {
        return clientName;
    }
    
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
    
    /**
     * Возвращает оставшееся время в минутах
     */
    public int getRemainingMinutes(LocalDateTime currentTime) {
        if (startTime == null || minutes == null || !active) {
            return 0;
        }
        
        // Рассчитываем прошедшее время в минутах
        long elapsedSeconds = java.time.Duration.between(startTime, currentTime).getSeconds();
        long elapsedMinutes = elapsedSeconds / 60;
        
        // Оставшееся время = общее время - прошедшее время
        int remainingMinutes = (int) (minutes - elapsedMinutes);
        
        // Не позволяем получить отрицательное значение
        return Math.max(0, remainingMinutes);
    }
    
    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", cardId=" + cardId +
                ", computerName='" + computerName + '\'' +
                ", computerNumber=" + computerNumber +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", minutes=" + minutes +
                ", points=" + points +
                ", active=" + active +
                ", cardNumber='" + cardNumber + '\'' +
                ", clientName='" + clientName + '\'' +
                '}';
    }
} 