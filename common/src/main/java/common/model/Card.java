package common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Модель карты лояльности
 */
public class Card implements Serializable {
    private Long id;
    private String cardNumber;
    private Long userId;
    private Integer points;
    private Integer level;
    private String status;
    private LocalDateTime issueDate;
    private LocalDateTime lastUsed;
    
    public Card() {
        this.points = 0;
        this.level = 1;
        this.status = "ACTIVE";
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCardNumber() {
        return cardNumber;
    }
    
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Integer getPoints() {
        return points;
    }
    
    public void setPoints(Integer points) {
        this.points = points;
    }
    
    public Integer getLevel() {
        return level;
    }
    
    public void setLevel(Integer level) {
        this.level = level;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getIssueDate() {
        return issueDate;
    }
    
    public void setIssueDate(LocalDateTime issueDate) {
        this.issueDate = issueDate;
    }
    
    public LocalDateTime getLastUsed() {
        return lastUsed;
    }
    
    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }
    
    /**
     * Get the card's number (alias for cardNumber)
     * @return the card number
     */
    @JsonIgnore
    public String getNumber() {
        return cardNumber;
    }
    
    /**
     * Gets the tier information
     * @return the tier object
     */
    public Tier getTier() {
        // For now, create a simple tier based on the level
        if (level == null) {
            return null;
        }
        
        return new Tier(level, getTierName(level));
    }
    
    /**
     * Get tier name based on level
     * @param level the tier level
     * @return the tier name
     */
    @JsonIgnore
    private String getTierName(int level) {
        switch (level) {
            case 1: return "Бронза";
            case 2: return "Серебро";
            case 3: return "Золото";
            case 4: return "Платина";
            case 5: return "Бриллиант";
            default: return "Уровень " + level;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Card card = (Card) o;
        
        return id != null ? id.equals(card.id) : card.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "Card{" +
                "id=" + id +
                ", cardNumber='" + cardNumber + '\'' +
                ", points=" + points +
                ", level=" + level +
                ", status='" + status + '\'' +
                '}';
    }
} 