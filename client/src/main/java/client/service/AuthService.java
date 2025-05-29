package client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.AuthRequest;
import common.dto.AuthResponse;
import common.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис для аутентификации пользователей
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    
    public AuthService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // Register all modules including Java 8 date/time
        this.baseUrl = String.format("http://%s:%d", Constants.SERVER_HOST, Constants.SERVER_PORT);
    }
    
    /**
     * Выполняет аутентификацию пользователя
     * @param login логин
     * @param password пароль
     * @return CompletableFuture с результатом аутентификации
     */
    public CompletableFuture<AuthResponse> authenticate(String login, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AuthRequest authRequest = new AuthRequest(login, password);
                
                URL url = new URL(baseUrl + "/api/auth");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                // Отправляем запрос
                connection.getOutputStream().write(objectMapper.writeValueAsBytes(authRequest));
                
                // Читаем ответ
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    AuthResponse response = objectMapper.readValue(connection.getInputStream(), AuthResponse.class);
                    logger.info("Пользователь успешно аутентифицирован: {}", login);
                    return response;
                } else {
                    AuthResponse errorResponse = objectMapper.readValue(connection.getErrorStream(), AuthResponse.class);
                    logger.warn("Ошибка аутентификации пользователя {}: {}", login, errorResponse.getErrorMessage());
                    return errorResponse;
                }
            } catch (Exception e) {
                logger.error("Ошибка при аутентификации", e);
                return AuthResponse.error("Ошибка соединения с сервером");
            }
        });
    }
} 