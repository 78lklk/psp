package common.model;

import java.time.LocalDateTime;

/**
 * Модель связи карты клиента с акцией
 */
public class CardPromotion {
    private Card card;
    private Promotion promotion;
    private LocalDateTime activatedAt;
    
    public CardPromotion() {
    }
    
    public CardPromotion(Card card, Promotion promotion, LocalDateTime activatedAt) {
        this.card = card;
        this.promotion = promotion;
        this.activatedAt = activatedAt;
    }
    
    public Card getCard() {
        return card;
    }
    
    public void setCard(Card card) {
        this.card = card;
    }
    
    public Promotion getPromotion() {
        return promotion;
    }
    
    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
    }
    
    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }
    
    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }
    
    @Override
    public String toString() {
        return "CardPromotion{" +
                "card=" + card +
                ", promotion=" + promotion +
                ", activatedAt=" + activatedAt +
                '}';
    }
} 