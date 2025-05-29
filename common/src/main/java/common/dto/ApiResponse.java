package common.dto;

/**
 * Обертка для ответов API
 * @param <T> тип данных в ответе
 */
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String errorMessage;
    private String errorCode;
    
    // Default constructor for Jackson
    public ApiResponse() {
    }
    
    public ApiResponse(boolean success, T data, String errorMessage, String errorCode) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }
    
    /**
     * Создает успешный ответ с данными
     * @param data данные ответа
     * @param <T> тип данных
     * @return ответ API
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }
    
    /**
     * Создает ответ с ошибкой
     * @param errorMessage сообщение об ошибке
     * @param errorCode код ошибки
     * @param <T> тип данных
     * @return ответ API
     */
    public static <T> ApiResponse<T> error(String errorMessage, String errorCode) {
        return new ApiResponse<>(false, null, errorMessage, errorCode);
    }
    
    /**
     * Создает ответ с ошибкой (с кодом ошибки "ERROR")
     * @param errorMessage сообщение об ошибке
     * @param <T> тип данных
     * @return ответ API
     */
    public static <T> ApiResponse<T> error(String errorMessage) {
        return error(errorMessage, "ERROR");
    }
    
    /**
     * Проверяет, является ли ответ успешным
     * @return true, если ответ успешный
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Получает данные ответа
     * @return данные ответа
     */
    public T getData() {
        return data;
    }
    
    /**
     * Получает сообщение об ошибке
     * @return сообщение об ошибке
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Устанавливает признак успешности ответа
     * @param success true, если ответ успешный
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    /**
     * Устанавливает данные ответа
     * @param data данные ответа
     */
    public void setData(T data) {
        this.data = data;
    }
    
    /**
     * Устанавливает сообщение об ошибке
     * @param errorMessage сообщение об ошибке
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * Получает код ошибки
     * @return код ошибки
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Устанавливает код ошибки
     * @param errorCode код ошибки
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
} 