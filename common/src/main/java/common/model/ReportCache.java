package common.model;

import java.time.LocalDateTime;

/**
 * Модель кеша отчетов
 */
public class ReportCache {
    private String key;
    private String csvPath;
    private LocalDateTime generated;
    private ReportType type;
    
    public ReportCache() {
    }
    
    public ReportCache(String key, String csvPath, LocalDateTime generated, ReportType type) {
        this.key = key;
        this.csvPath = csvPath;
        this.generated = generated;
        this.type = type;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getCsvPath() {
        return csvPath;
    }
    
    public void setCsvPath(String csvPath) {
        this.csvPath = csvPath;
    }
    
    public LocalDateTime getGenerated() {
        return generated;
    }
    
    public void setGenerated(LocalDateTime generated) {
        this.generated = generated;
    }
    
    public ReportType getType() {
        return type;
    }
    
    public void setType(ReportType type) {
        this.type = type;
    }
    
    /**
     * Проверяет, устарел ли отчет
     * @param maxAgeMinutes максимальное время жизни отчета в минутах
     * @return true, если отчет устарел
     */
    public boolean isExpired(int maxAgeMinutes) {
        if (generated == null) {
            return true;
        }
        return generated.plusMinutes(maxAgeMinutes).isBefore(LocalDateTime.now());
    }
    
    @Override
    public String toString() {
        return "ReportCache{" +
                "key='" + key + '\'' +
                ", csvPath='" + csvPath + '\'' +
                ", generated=" + generated +
                ", type=" + type +
                '}';
    }
    
    /**
     * Типы отчетов
     */
    public enum ReportType {
        DAILY("Ежедневный отчет"),
        POINTS("Отчет по баллам"),
        SESSIONS("Отчет по сессиям"),
        PROMOTIONS("Отчет по акциям");
        
        private final String description;
        
        ReportType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
} 