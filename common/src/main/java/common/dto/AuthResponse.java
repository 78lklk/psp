package common.dto;

import common.model.User;

/**
 * DTO для ответа на запрос аутентификации
 */
public class AuthResponse {
    private boolean success;
    private String token;
    private User user;
    private String errorMessage;
    private String message;
    
    public AuthResponse() {
    }
    
    public AuthResponse(boolean success, String token, User user, String errorMessage) {
        this.success = success;
        this.token = token;
        this.user = user;
        this.errorMessage = errorMessage;
    }
    
    public static AuthResponse success(String token, User user) {
        return new AuthResponse(true, token, user, null);
    }
    
    public static AuthResponse error(String errorMessage) {
        return new AuthResponse(false, null, null, errorMessage);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
} 