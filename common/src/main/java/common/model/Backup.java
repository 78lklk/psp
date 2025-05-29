package common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Модель резервной копии базы данных
 */
public class Backup implements Serializable {
    private Long id;
    private String fileName;
    private LocalDateTime createdAt;
    private User createdBy;
    private Long fileSize;
    private String hash;
    private Boolean isValid;
    private String description;
    
    public Backup() {
    }
    
    public Backup(Long id, String fileName, Double sizeMb, LocalDateTime createdAt, User createdBy) {
        this.id = id;
        this.fileName = fileName;
        this.fileSize = (long)(sizeMb * 1024 * 1024);
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.isValid = true;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public User getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getHash() {
        return hash;
    }
    
    public void setHash(String hash) {
        this.hash = hash;
    }
    
    public Boolean getIsValid() {
        return isValid;
    }
    
    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Получает размер в мегабайтах
     * @return размер файла в мегабайтах
     */
    public double getSizeMb() {
        if (fileSize == null) {
            return 0.0;
        }
        return fileSize / (1024.0 * 1024.0);
    }
    
    /**
     * Получает логин пользователя (для обратной совместимости)
     * @return логин пользователя
     */
    public String getLogin() {
        return createdBy != null ? createdBy.getLogin() : null;
    }
    
    @Override
    public String toString() {
        return "Backup{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", createdAt=" + createdAt +
                ", createdBy=" + (createdBy != null ? createdBy.getLogin() : "null") +
                ", fileSize=" + fileSize +
                ", hash='" + hash + '\'' +
                ", isValid=" + isValid +
                ", description='" + description + '\'' +
                '}';
    }
} 