package common.model;

import java.time.LocalDateTime;

/**
 * Модель транзакции баллов на карте
 */
public class Transaction {
    public enum Type {
        DEPOSIT,    // Начисление баллов
        WITHDRAW,   // Списание баллов
        BONUS,      // Бонусное начисление
        EXPIRY,     // Сгорание баллов
        ADJUSTMENT  // Корректировка
    }
    
    private Long id;
    private Card card;
    private Type type;
    private Integer points;
    private LocalDateTime timestamp;
    private String description;
    
    public Transaction() {
    }
    
    public Transaction(Long id, Card card, Type type, Integer points, LocalDateTime timestamp, String description) {
        this.id = id;
        this.card = card;
        this.type = type;
        this.points = points;
        this.timestamp = timestamp;
        this.description = description;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Card getCard() {
        return card;
    }
    
    public void setCard(Card card) {
        this.card = card;
    }
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public Integer getPoints() {
        return points;
    }
    
    public void setPoints(Integer points) {
        this.points = points;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", card=" + card +
                ", type=" + type +
                ", points=" + points +
                ", timestamp=" + timestamp +
                ", description='" + description + '\'' +
                '}';
    }
} 