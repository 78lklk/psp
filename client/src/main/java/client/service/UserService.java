package client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.ApiResponse;
import common.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.Map;
import java.nio.charset.StandardCharsets;

/**
 * Сервис для работы с пользователями
 */
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String API_URL = ServiceUtils.getApiUrl();
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String authToken;
    private final Executor executor;
    
    /**
     * Создает новый сервис пользователей
     * @param authToken токен авторизации
     */
    public UserService(String authToken) {
        this.authToken = authToken;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.executor = Executors.newFixedThreadPool(5);
    }
    
    /**
     * Получает список всех пользователей
     * @return список пользователей
     */
    public CompletableFuture<List<User>> getAllUsers() {
        logger.debug("Запрос списка всех пользователей");
        
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/users"))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    logger.debug("Получен ответ на запрос пользователей, статус: {}", statusCode);
                    
                    if (statusCode == 200) {
                        try {
                            ApiResponse<List<User>> apiResponse = objectMapper.readValue(
                                    response.body(),
                                    new TypeReference<ApiResponse<List<User>>>() {}
                            );
                            return apiResponse.getData();
                        } catch (Exception e) {
                            logger.error("Ошибка при разборе ответа", e);
                            return Collections.emptyList();
                        }
                    } else {
                        logger.error("Ошибка при получении пользователей, код: {}", statusCode);
                        return Collections.emptyList();
                    }
                }, executor);
    }
    
    /**
     * Получает пользователя по ID
     * @param userId ID пользователя
     * @return пользователь
     */
    public CompletableFuture<User> getUserById(Long userId) {
        logger.debug("Запрос пользователя по ID: {}", userId);
        
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/users/" + userId))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    logger.debug("Получен ответ на запрос пользователя, статус: {}", statusCode);
                    
                    if (statusCode == 200) {
                        try {
                            ApiResponse<User> apiResponse = objectMapper.readValue(
                                    response.body(),
                                    new TypeReference<ApiResponse<User>>() {}
                            );
                            return apiResponse.getData();
                        } catch (Exception e) {
                            logger.error("Ошибка при разборе ответа", e);
                            return null;
                        }
                    } else {
                        logger.error("Ошибка при получении пользователя, код: {}", statusCode);
                        return null;
                    }
                }, executor);
    }
    
    /**
     * Создает нового пользователя
     * @param user пользователь
     * @return созданный пользователь
     */
    public CompletableFuture<User> createUser(User user) {
        logger.debug("Создание нового пользователя: {}", user.getUsername());
        
        try {
            String requestBody = objectMapper.writeValueAsString(user);
            logger.debug("Отправляемое тело запроса: {}", requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + authToken)
                    .uri(URI.create(API_URL + "/users"))
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> {
                        int statusCode = response.statusCode();
                        logger.debug("Получен ответ на создание пользователя, статус: {}", statusCode);
                        
                        if (statusCode == 200 || statusCode == 201) {
                            try {
                                ApiResponse<User> apiResponse = objectMapper.readValue(
                                        response.body(),
                                        new TypeReference<ApiResponse<User>>() {}
                                );
                                return apiResponse.getData();
                            } catch (Exception e) {
                                logger.error("Ошибка при разборе ответа", e);
                                return null;
                            }
                        } else {
                            logger.error("Ошибка при создании пользователя, код: {}", statusCode);
                            return null;
                        }
                    }, executor);
        } catch (Exception e) {
            logger.error("Ошибка при сериализации запроса", e);
            CompletableFuture<User> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Обновляет пользователя
     * @param user пользователь
     * @return обновленный пользователь
     */
    public CompletableFuture<User> updateUser(User user) {
        logger.debug("Обновление пользователя с ID: {}", user.getId());
        
        try {
            // Ensure login and username are synchronized
            if (user.getLogin() != null && user.getUsername() == null) {
                user.setUsername(user.getLogin());
            } else if (user.getUsername() != null && user.getLogin() == null) {
                user.setLogin(user.getUsername());
            }
            
            String requestBody = objectMapper.writeValueAsString(user);
            logger.debug("Отправляемое тело запроса: {}", requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .PUT(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + authToken)
                    .uri(URI.create(API_URL + "/users/" + user.getId()))
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> {
                        int statusCode = response.statusCode();
                        logger.debug("Получен ответ на обновление пользователя, статус: {}", statusCode);
                        
                        if (statusCode == 200) {
                            try {
                                ApiResponse<User> apiResponse = objectMapper.readValue(
                                        response.body(),
                                        new TypeReference<ApiResponse<User>>() {}
                                );
                                return apiResponse.getData();
                            } catch (Exception e) {
                                logger.error("Ошибка при разборе ответа", e);
                                return null;
                            }
                        } else {
                            logger.error("Ошибка при обновлении пользователя, код: {}", statusCode);
                            return null;
                        }
                    }, executor);
        } catch (Exception e) {
            logger.error("Ошибка при сериализации запроса", e);
            CompletableFuture<User> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Удаляет пользователя
     * @param userId ID пользователя
     * @return результат операции
     */
    public CompletableFuture<Boolean> deleteUser(Long userId) {
        logger.debug("Удаление пользователя с ID: {}", userId);
        
        HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/users/" + userId))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    logger.debug("Получен ответ на удаление пользователя, статус: {}", statusCode);
                    return statusCode == 204 || statusCode == 200;
                }, executor);
    }
    
    /**
     * Меняет пароль пользователя
     * @param userId ID пользователя
     * @param oldPassword старый пароль
     * @param newPassword новый пароль
     * @return результат операции
     */
    public CompletableFuture<Boolean> changePassword(Long userId, String oldPassword, String newPassword) {
        logger.debug("Запрос на смену пароля пользователя с ID: {}", userId);
        
        try {
            String requestBody = objectMapper.writeValueAsString(
                    Map.of("oldPassword", oldPassword, "newPassword", newPassword)
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + authToken)
                    .uri(URI.create(API_URL + "/users/" + userId + "/change-password"))
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> {
                        int statusCode = response.statusCode();
                        logger.debug("Получен ответ на смену пароля, статус: {}", statusCode);
                        return statusCode == 200;
                    }, executor);
        } catch (Exception e) {
            logger.error("Ошибка при сериализации запроса", e);
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
} 