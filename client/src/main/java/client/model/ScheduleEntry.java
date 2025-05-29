package client.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Упрощенная модель для записи в расписании компьютеров
 */
public class ScheduleEntry implements Serializable {
    private Long id;
    private String computerName;
    private String clientName;
    private String cardNumber;
    private LocalDateTime startTime;
    private Integer durationHours;
    private boolean isActive;
    
    public ScheduleEntry() {
        this.isActive = true;
    }
    
    public ScheduleEntry(String computerName, String clientName, String cardNumber, 
                        LocalDateTime startTime, Integer durationHours) {
        this.computerName = computerName;
        this.clientName = clientName;
        this.cardNumber = cardNumber;
        this.startTime = startTime;
        this.durationHours = durationHours;
        this.isActive = true;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getComputerName() {
        return computerName;
    }
    
    public void setComputerName(String computerName) {
        this.computerName = computerName;
    }
    
    public String getClientName() {
        return clientName;
    }
    
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
    
    public String getCardNumber() {
        return cardNumber;
    }
    
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public Integer getDurationHours() {
        return durationHours;
    }
    
    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }
    
    public LocalDateTime getEndTime() {
        if (startTime != null && durationHours != null) {
            return startTime.plusHours(durationHours);
        }
        return null;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    /**
     * Проверяет, пересекается ли данная запись с указанным временем
     * @param time время для проверки
     * @return true, если время попадает в интервал данной записи
     */
    public boolean containsTime(LocalDateTime time) {
        if (startTime == null || !isActive) {
            return false;
        }
        
        LocalDateTime endTime = getEndTime();
        return (time.isEqual(startTime) || time.isAfter(startTime)) && 
                (endTime == null || time.isBefore(endTime));
    }
    
    @Override
    public String toString() {
        return "ScheduleEntry{" +
                "id=" + id +
                ", computerName='" + computerName + '\'' +
                ", clientName='" + clientName + '\'' +
                ", cardNumber='" + cardNumber + '\'' +
                ", startTime=" + startTime +
                ", durationHours=" + durationHours +
                ", isActive=" + isActive +
                '}';
    }
} 