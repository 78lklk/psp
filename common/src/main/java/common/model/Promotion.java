package common.model;

import java.time.LocalDate;

/**
 * Модель акции
 */
public class Promotion {
    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private Integer bonusPercent;
    private Integer bonusPoints;
    
    public Promotion() {
    }
    
    public Promotion(Long id, String name, String description, LocalDate startDate, LocalDate endDate, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public Integer getBonusPercent() {
        return bonusPercent;
    }
    
    public void setBonusPercent(Integer bonusPercent) {
        this.bonusPercent = bonusPercent;
    }
    
    public Integer getBonusPoints() {
        return bonusPoints;
    }
    
    public void setBonusPoints(Integer bonusPoints) {
        this.bonusPoints = bonusPoints;
    }
    
    /**
     * Проверяет, активна ли акция на указанную дату
     * @param date дата для проверки
     * @return true, если акция активна на указанную дату
     */
    public boolean isActiveOn(LocalDate date) {
        if (!active) {
            return false;
        }
        
        if (date == null) {
            date = LocalDate.now();
        }
        
        boolean afterStart = startDate == null || !date.isBefore(startDate);
        boolean beforeEnd = endDate == null || !date.isAfter(endDate);
        
        return afterStart && beforeEnd;
    }
    
    @Override
    public String toString() {
        return "Promotion{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", active=" + active +
                '}';
    }
} 