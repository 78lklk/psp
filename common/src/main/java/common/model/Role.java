package common.model;

import java.io.Serializable;

/**
 * Модель роли пользователя в системе
 */
public class Role implements Serializable {
    private Long id;
    private String name;
    private String description;
    
    public Role() {
    }
    
    public Role(Long id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public Role(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Role role = (Role) o;
        
        return id != null ? id.equals(role.id) : role.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return name;
    }
} 