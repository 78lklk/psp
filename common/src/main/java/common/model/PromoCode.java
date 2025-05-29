package common.model;

import java.time.LocalDate;

/**
 * Модель промокода
 */
public class PromoCode {
    private Long id;
    private String code;
    private Long promotionId;
    private boolean isUsed;
    private String usedBy;
    private LocalDate usedDate;
    private LocalDate expiryDate;
    private String createdBy;
    
    // Старые поля для совместимости с клиентским кодом
    private String description;
    private Integer bonusPoints;
    private Double discountPercent;
    private Integer usesLimit;
    private Integer usesCount;
    private boolean active;
    
    public PromoCode() {
    }
    
    public PromoCode(Long id, String code, Long promotionId, boolean isUsed, 
                    LocalDate expiryDate, String createdBy) {
        this.id = id;
        this.code = code;
        this.promotionId = promotionId;
        this.isUsed = isUsed;
        this.expiryDate = expiryDate;
        this.createdBy = createdBy;
        this.active = !isUsed;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public Long getPromotionId() {
        return promotionId;
    }
    
    public void setPromotionId(Long promotionId) {
        this.promotionId = promotionId;
    }
    
    public boolean isUsed() {
        return isUsed;
    }
    
    public void setUsed(boolean used) {
        isUsed = used;
        this.active = !used;
    }
    
    public String getUsedBy() {
        return usedBy;
    }
    
    public void setUsedBy(String usedBy) {
        this.usedBy = usedBy;
    }
    
    public LocalDate getUsedDate() {
        return usedDate;
    }
    
    public void setUsedDate(LocalDate usedDate) {
        this.usedDate = usedDate;
    }
    
    public LocalDate getExpiryDate() {
        return expiryDate;
    }
    
    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    // Методы для обратной совместимости
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getBonusPoints() {
        return bonusPoints;
    }
    
    public void setBonusPoints(Integer bonusPoints) {
        this.bonusPoints = bonusPoints;
    }
    
    public Double getDiscountPercent() {
        return discountPercent;
    }
    
    public void setDiscountPercent(Double discountPercent) {
        this.discountPercent = discountPercent;
    }
    
    public Integer getUsesLimit() {
        return usesLimit;
    }
    
    public void setUsesLimit(Integer usesLimit) {
        this.usesLimit = usesLimit;
    }
    
    public Integer getUsesCount() {
        return usesCount;
    }
    
    public void setUsesCount(Integer usesCount) {
        this.usesCount = usesCount;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
        this.isUsed = !active;
    }
    
    @Override
    public String toString() {
        return "PromoCode{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", promotionId=" + promotionId +
                ", isUsed=" + isUsed +
                ", expiryDate=" + expiryDate +
                '}';
    }
} 