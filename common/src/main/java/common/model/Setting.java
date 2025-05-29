package common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Модель настройки системы
 */
public class Setting implements Serializable {
    private String key;
    private String value;
    private String description;
    private LocalDateTime lastUpdated;
    private String updatedBy;
    
    public Setting() {
    }
    
    public Setting(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    /**
     * Gets the login (for backward compatibility)
     * @return the updatedBy as login
     */
    public String getLogin() {
        return updatedBy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Setting setting = (Setting) o;
        
        return key.equals(setting.key);
    }
    
    @Override
    public int hashCode() {
        return key.hashCode();
    }
    
    /**
     * Получить значение как целое число
     * @param defaultValue значение по умолчанию, если не удалось преобразовать
     * @return значение как целое число или defaultValue
     */
    public Integer getValueAsInt(Integer defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Получить значение как логическое
     * @param defaultValue значение по умолчанию, если не удалось преобразовать
     * @return значение как логическое или defaultValue
     */
    public Boolean getValueAsBoolean(Boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }
    
    @Override
    public String toString() {
        return "Setting{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", description='" + description + '\'' +
                ", lastUpdated=" + lastUpdated +
                ", updatedBy='" + updatedBy + '\'' +
                '}';
    }
} 