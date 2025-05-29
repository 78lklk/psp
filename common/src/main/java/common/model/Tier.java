package common.model;

import java.io.Serializable;

/**
 * Модель для уровней программы лояльности
 */
public class Tier implements Serializable {
    private Integer level;
    private String name;
    private Integer minPoints;
    private Integer maxPoints;
    private Double bonusMultiplier;
    
    public Tier() {
    }
    
    public Tier(Integer level, String name) {
        this.level = level;
        this.name = name;
    }
    
    public Tier(Integer level, String name, Integer minPoints, Integer maxPoints, Double bonusMultiplier) {
        this.level = level;
        this.name = name;
        this.minPoints = minPoints;
        this.maxPoints = maxPoints;
        this.bonusMultiplier = bonusMultiplier;
    }
    
    public Integer getLevel() {
        return level;
    }
    
    public void setLevel(Integer level) {
        this.level = level;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Integer getMinPoints() {
        return minPoints;
    }
    
    public void setMinPoints(Integer minPoints) {
        this.minPoints = minPoints;
    }
    
    public Integer getMaxPoints() {
        return maxPoints;
    }
    
    public void setMaxPoints(Integer maxPoints) {
        this.maxPoints = maxPoints;
    }
    
    public Double getBonusMultiplier() {
        return bonusMultiplier;
    }
    
    public void setBonusMultiplier(Double bonusMultiplier) {
        this.bonusMultiplier = bonusMultiplier;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Tier tier = (Tier) o;
        
        return level != null ? level.equals(tier.level) : tier.level == null;
    }
    
    @Override
    public int hashCode() {
        return level != null ? level.hashCode() : 0;
    }
} 