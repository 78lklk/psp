package common.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO для передачи статистики акций и промокодов
 */
public class PromotionStatisticsDTO {
    private Long promotionId;
    private String promotionName;
    private int totalBonusPoints;
    private double averageBonusPoints;
    private int usedPromoCodes;
    private double promoCodeConversion;
    private int usageCount;
    private double discountAmount;
    private Map<String, Integer> promotionUsageData;
    private Map<String, Integer> promoCodeUsageData;
    private Map<String, Integer> dailyActivityData;
    private Map<String, Integer> usageByDay;
    
    public PromotionStatisticsDTO() {
        this.promotionUsageData = new HashMap<>();
        this.promoCodeUsageData = new HashMap<>();
        this.dailyActivityData = new HashMap<>();
        this.usageByDay = new HashMap<>();
    }
    
    public Long getPromotionId() {
        return promotionId;
    }
    
    public void setPromotionId(Long promotionId) {
        this.promotionId = promotionId;
    }
    
    public String getPromotionName() {
        return promotionName;
    }
    
    public void setPromotionName(String promotionName) {
        this.promotionName = promotionName;
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    public double getDiscountAmount() {
        return discountAmount;
    }
    
    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }
    
    public Map<String, Integer> getUsageByDay() {
        return usageByDay;
    }
    
    public void setUsageByDay(Map<String, Integer> usageByDay) {
        this.usageByDay = usageByDay;
    }
    
    public int getTotalBonusPoints() {
        return totalBonusPoints;
    }
    
    public void setTotalBonusPoints(int totalBonusPoints) {
        this.totalBonusPoints = totalBonusPoints;
    }
    
    public double getAverageBonusPoints() {
        return averageBonusPoints;
    }
    
    public void setAverageBonusPoints(double averageBonusPoints) {
        this.averageBonusPoints = averageBonusPoints;
    }
    
    public int getUsedPromoCodes() {
        return usedPromoCodes;
    }
    
    public void setUsedPromoCodes(int usedPromoCodes) {
        this.usedPromoCodes = usedPromoCodes;
    }
    
    public double getPromoCodeConversion() {
        return promoCodeConversion;
    }
    
    public void setPromoCodeConversion(double promoCodeConversion) {
        this.promoCodeConversion = promoCodeConversion;
    }
    
    public Map<String, Integer> getPromotionUsageData() {
        return promotionUsageData;
    }
    
    public void setPromotionUsageData(Map<String, Integer> promotionUsageData) {
        this.promotionUsageData = promotionUsageData;
    }
    
    public Map<String, Integer> getPromoCodeUsageData() {
        return promoCodeUsageData;
    }
    
    public void setPromoCodeUsageData(Map<String, Integer> promoCodeUsageData) {
        this.promoCodeUsageData = promoCodeUsageData;
    }
    
    public Map<String, Integer> getDailyActivityData() {
        return dailyActivityData;
    }
    
    public void setDailyActivityData(Map<String, Integer> dailyActivityData) {
        this.dailyActivityData = dailyActivityData;
    }
} 