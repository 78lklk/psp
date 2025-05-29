package common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Модель пользователя системы
 */
public class User implements Serializable {
    private Long id;
    private String username; // corresponds to login in DB
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;  // added to match DB
    private String phone;     // added to match DB
    private Role role;
    private boolean active = true;  // default to true, not in DB
    private LocalDateTime createdAt; // registration_date in DB
    private LocalDateTime lastLogin;
    
    public User() {
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getFullName() {
        // If fullName is directly set, return it
        if (fullName != null && !fullName.isEmpty()) {
            return fullName;
        }
        
        // Otherwise construct from first and last name
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) {
            sb.append(firstName);
        }
        if (lastName != null && !lastName.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(lastName);
        }
        return sb.toString();
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
        
        // When setting fullName, try to parse it into firstName and lastName
        if (fullName != null && !fullName.isEmpty()) {
            String[] parts = fullName.split("\\s+", 2);
            if (parts.length > 0) {
                this.firstName = parts[0];
                if (parts.length > 1) {
                    this.lastName = parts[1];
                }
            }
        }
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public Role getRole() {
        return role;
    }
    
    public void setRole(Role role) {
        this.role = role;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    /**
     * Gets the login (for backward compatibility)
     * @return the username as login
     */
    public String getLogin() {
        return username;
    }

    /**
     * Sets the login (for backward compatibility)
     * @param login the login to set
     */
    public void setLogin(String login) {
        this.username = login;
    }
    
    /**
     * Gets the registration date (alias for createdAt)
     * @return the registration date
     */
    public LocalDateTime getRegistrationDate() {
        return createdAt;
    }
    
    /**
     * Sets the registration date (alias for createdAt)
     * @param registrationDate the registration date
     */
    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.createdAt = registrationDate;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        User user = (User) o;
        
        return id != null ? id.equals(user.id) : user.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", role='" + role + '\'' +
                ", active=" + active +
                '}';
    }
} 