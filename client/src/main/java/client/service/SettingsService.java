package client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.ApiResponse;
import common.model.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Сервис для работы с настройками системы
 */
public class SettingsService {
    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);
    private static final String API_URL = ServiceUtils.getApiUrl();
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String authToken;
    private final Executor executor;
    
    /**
     * Класс для хранения значения и описания настройки
     */
    public static class SettingValue {
        private String value;
        private String description;
        
        public SettingValue() {
        }
        
        public SettingValue(String value, String description) {
            this.value = value;
            this.description = description;
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
    }
    
    /**
     * Создает новый сервис настроек
     * @param authToken токен авторизации
     */
    public SettingsService(String authToken) {
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
     * Получает все настройки системы
     * @return карта с ключами и значениями настроек
     */
    public CompletableFuture<Map<String, SettingValue>> getAllSettings() {
        logger.debug("Запрос всех настроек системы");
        
                HttpRequest request = HttpRequest.newBuilder()
                .GET()
                        .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/settings"))
                        .build();
                
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                int statusCode = response.statusCode();
                    logger.debug("Получен ответ на запрос настроек, статус: {}", statusCode);
                
                if (statusCode == 200) {
                    try {
                            return objectMapper.readValue(
                                response.body(), 
                                    new TypeReference<Map<String, SettingValue>>() {}
                            );
                        } catch (Exception e) {
                            logger.error("Ошибка при разборе ответа", e);
                            return Collections.emptyMap();
                        }
                    } else {
                        logger.error("Ошибка при получении настроек, код: {}", statusCode);
                        return Collections.emptyMap();
                    }
                }, executor);
    }
    
    /**
     * Получает значение настройки по ключу
     * @param key ключ настройки
     * @return значение настройки
     */
    public CompletableFuture<SettingValue> getSetting(String key) {
        logger.debug("Запрос настройки с ключом: {}", key);
        
                HttpRequest request = HttpRequest.newBuilder()
                .GET()
                        .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/settings/" + key))
                        .build();
                
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                int statusCode = response.statusCode();
                    logger.debug("Получен ответ на запрос настройки, статус: {}", statusCode);
                
                if (statusCode == 200) {
                    try {
                            return objectMapper.readValue(response.body(), SettingValue.class);
                        } catch (Exception e) {
                            logger.error("Ошибка при разборе ответа", e);
                            return null;
                        }
                    } else {
                        logger.error("Ошибка при получении настройки, код: {}", statusCode);
                        return null;
                    }
                }, executor);
    }
    
    /**
     * Сохраняет или обновляет настройку
     * @param key ключ настройки
     * @param value значение настройки
     * @param description описание настройки
     * @return результат операции
     */
    public CompletableFuture<Boolean> saveSetting(String key, String value, String description) {
        logger.debug("Сохранение настройки с ключом: {}", key);
        
        try {
            // Создаем настройку для отправки на сервер
            Setting setting = new Setting();
            setting.setKey(key);
            setting.setValue(value);
            setting.setDescription(description);
            setting.setLastUpdated(LocalDateTime.now());
            
            String requestBody = objectMapper.writeValueAsString(setting);
            logger.debug("Отправляемое тело запроса: {}", requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/settings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + authToken)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> {
                        int statusCode = response.statusCode();
                        String responseBody = response.body();
                        logger.debug("Ответ сервера: {} {}", statusCode, responseBody);
                        
                        if (statusCode == 200 || statusCode == 201) {
                            try {
                                ApiResponse<?> apiResponse = objectMapper.readValue(
                                        responseBody,
                                        new TypeReference<ApiResponse<?>>() {}
                                );
                                
                                if (apiResponse.isSuccess()) {
                                    logger.info("Настройка успешно сохранена: {}", key);
                                    return true;
                } else {
                                    logger.error("Ошибка сохранения настройки: {}", apiResponse.getErrorMessage());
                                    return false;
                }
            } catch (Exception e) {
                                logger.error("Ошибка при разборе ответа от сервера", e);
                                return false;
                            }
                        } else {
                            logger.error("Ошибка при сохранении настройки, код: {}, ответ: {}", statusCode, responseBody);
                            return false;
                        }
                    })
                    .exceptionally(ex -> {
                        logger.error("Исключение при сохранении настройки: {}", ex.getMessage(), ex);
                        return false;
                    });
        } catch (Exception e) {
            logger.error("Ошибка при отправке запроса сохранения настройки", e);
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.complete(false);
            return future;
        }
    }
    
    /**
     * Удаляет настройку
     * @param key ключ настройки
     * @return результат операции
     */
    public CompletableFuture<Boolean> deleteSetting(String key) {
        logger.debug("Удаление настройки с ключом: {}", key);
        
        HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/settings/" + key))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    logger.debug("Получен ответ на удаление настройки, статус: {}", statusCode);
                    return statusCode == 204 || statusCode == 200;
                }, executor);
    }
    
    /**
     * Получает группу настроек по префиксу ключа
     * @param prefix префикс ключа
     * @return карта с ключами и значениями настроек
     */
    public CompletableFuture<Map<String, SettingValue>> getSettingsByPrefix(String prefix) {
        logger.debug("Запрос настроек с префиксом: {}", prefix);
        
        return getAllSettings()
                .thenApplyAsync(allSettings -> {
                    Map<String, SettingValue> filteredSettings = new HashMap<>();
                    
                    allSettings.forEach((key, value) -> {
                        if (key.startsWith(prefix)) {
                            filteredSettings.put(key, value);
                        }
                    });
                    
                    return filteredSettings;
                }, executor);
    }
    
    /**
     * Получает значение настройки по ключу
     * @param key ключ настройки
     * @return настройка
     */
    public CompletableFuture<Setting> getSettingByKey(String key) {
        logger.debug("Запрос настройки с ключом: {}", key);
                
                HttpRequest request = HttpRequest.newBuilder()
                .GET()
                        .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/settings/" + key))
                        .build();
                
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                int statusCode = response.statusCode();
                    logger.debug("Получен ответ на запрос настройки, статус: {}", statusCode);
                
                if (statusCode == 200) {
                    try {
                            ApiResponse<Setting> apiResponse = objectMapper.readValue(
                                response.body(), 
                                    new TypeReference<ApiResponse<Setting>>() {}
                            );
                            return apiResponse.getData();
                        } catch (Exception e) {
                            logger.error("Ошибка при разборе ответа", e);
                            return null;
                        }
                    } else {
                        logger.error("Ошибка при получении настройки, код: {}", statusCode);
                        return null;
                    }
                }, executor);
    }
    
    /**
     * Создает новую настройку
     * @param key ключ настройки
     * @param value значение настройки
     * @param description описание настройки
     * @return созданная настройка
     */
    public CompletableFuture<Setting> createSetting(String key, String value, String description) {
        logger.debug("Создание настройки с ключом: {}", key);
        
        Setting setting = new Setting(key, value, description);
        
        try {
            String requestBody = objectMapper.writeValueAsString(setting);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + authToken)
                    .uri(URI.create(API_URL + "/settings"))
                        .build();
                
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> {
                int statusCode = response.statusCode();
                        logger.debug("Получен ответ на создание настройки, статус: {}", statusCode);
                        
                        if (statusCode == 201) {
                            try {
                                ApiResponse<Setting> apiResponse = objectMapper.readValue(
                                        response.body(),
                                        new TypeReference<ApiResponse<Setting>>() {}
                                );
                                return apiResponse.getData();
                    } catch (Exception e) {
                        logger.error("Ошибка при разборе ответа", e);
                        return null;
                    }
                } else {
                    logger.error("Ошибка при создании настройки, код: {}", statusCode);
                    return null;
                }
                    }, executor);
            } catch (Exception e) {
            logger.error("Ошибка при сериализации запроса", e);
            CompletableFuture<Setting> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Обновляет настройку
     * @param setting настройка
     * @return обновленная настройка
     */
    public CompletableFuture<Setting> updateSetting(Setting setting) {
        logger.debug("Обновление настройки с ключом: {}", setting.getKey());
        
        try {
            String requestBody = objectMapper.writeValueAsString(setting);
            
                HttpRequest request = HttpRequest.newBuilder()
                    .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + authToken)
                    .uri(URI.create(API_URL + "/settings/" + setting.getKey()))
                        .build();
                
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> {
                int statusCode = response.statusCode();
                        logger.debug("Получен ответ на обновление настройки, статус: {}", statusCode);
                        
                        if (statusCode == 200) {
                            try {
                                ApiResponse<Setting> apiResponse = objectMapper.readValue(
                                        response.body(),
                                        new TypeReference<ApiResponse<Setting>>() {}
                                );
                                return apiResponse.getData();
                            } catch (Exception e) {
                                logger.error("Ошибка при разборе ответа", e);
                                return null;
                            }
                } else {
                            logger.error("Ошибка при обновлении настройки, код: {}", statusCode);
                            return null;
                }
                    }, executor);
            } catch (Exception e) {
            logger.error("Ошибка при сериализации запроса", e);
            CompletableFuture<Setting> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
} 