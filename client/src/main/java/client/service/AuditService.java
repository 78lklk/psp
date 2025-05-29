package client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import common.dto.ApiResponse;
import common.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static client.service.ServiceUtils.*;

/**
 * Сервис для работы с аудитом
 */
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    private final HttpClient httpClient;
    private final String authToken;
    
    public AuditService(String authToken) {
        this.authToken = authToken;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }
    
    /**
     * Получает записи аудита
     * @return список записей аудита
     */
    public CompletableFuture<List<AuditLog>> getAuditLogs() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(getApiUrl() + "/audit"))
                        .header("Authorization", "Bearer " + authToken)
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();
                
                if (statusCode == 200) {
                    try {
                        ApiResponse<List<AuditLog>> apiResponse = OBJECT_MAPPER.readValue(
                                response.body(), 
                                new TypeReference<ApiResponse<List<AuditLog>>>() {});
                        
                        if (apiResponse.isSuccess()) {
                            List<AuditLog> logs = apiResponse.getData();
                            logger.debug("Получено {} записей аудита", logs.size());
                            return logs;
                        } else {
                            logger.error("Ошибка при получении записей аудита: {}", apiResponse.getErrorMessage());
                            return Collections.emptyList();
                        }
                    } catch (Exception e) {
                        logger.error("Ошибка при разборе ответа", e);
                        return Collections.emptyList();
                    }
                } else {
                    logger.error("Ошибка при получении записей аудита, код: {}", statusCode);
                    return Collections.emptyList();
                }
            } catch (Exception e) {
                logger.error("Ошибка при обращении к API аудита", e);
                return Collections.emptyList();
            }
        });
    }
    
    /**
     * Получает записи аудита за указанный период
     * @param from начало периода
     * @param to конец периода
     * @return список записей аудита
     */
    public CompletableFuture<List<AuditLog>> getAuditLogsByPeriod(LocalDateTime from, LocalDateTime to) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                String fromStr = URLEncoder.encode(from.format(formatter), StandardCharsets.UTF_8);
                String toStr = URLEncoder.encode(to.format(formatter), StandardCharsets.UTF_8);
                
                logger.debug("Запрос аудита за период {} - {}", from.format(formatter), to.format(formatter));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(getApiUrl() + "/audit?from=" + fromStr + "&to=" + toStr))
                        .header("Authorization", "Bearer " + authToken)
                        .GET()
                        .build();
                
                logger.debug("URL запроса: {}", request.uri().toString());
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();
                
                if (statusCode == 200) {
                    String responseBody = response.body();
                    logger.debug("Получен ответ от сервера: {}", responseBody);
                    
                    // Парсим ответ как ApiResponse с массивом AuditLog
                    ApiResponse<List<AuditLog>> apiResponse = OBJECT_MAPPER.readValue(
                            responseBody, 
                            new TypeReference<ApiResponse<List<AuditLog>>>() {}
                    );
                    
                    if (apiResponse.isSuccess()) {
                        List<AuditLog> logs = apiResponse.getData();
                        logger.info("Успешно получено {} записей аудита", logs.size());
                        return logs;
                    } else {
                        logger.error("Ошибка API при получении аудита: {}", apiResponse.getErrorMessage());
                        return new ArrayList<>();
                    }
                } else {
                    logger.error("Ошибка при получении аудита, код: {}", statusCode);
                    return new ArrayList<>();
                }
            } catch (Exception e) {
                logger.error("Ошибка при получении аудита", e);
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * Получает записи аудита по указанному пользователю
     * @param userId ID пользователя
     * @return список записей аудита
     */
    public CompletableFuture<List<AuditLog>> getAuditLogsByUser(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(getApiUrl() + "/audit/user/" + userId))
                        .header("Authorization", "Bearer " + authToken)
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();
                
                if (statusCode == 200) {
                    try {
                        ApiResponse<List<AuditLog>> apiResponse = OBJECT_MAPPER.readValue(
                                response.body(), 
                                new TypeReference<ApiResponse<List<AuditLog>>>() {});
                        
                        if (apiResponse.isSuccess()) {
                            List<AuditLog> logs = apiResponse.getData();
                            logger.debug("Получено {} записей аудита для пользователя {}", logs.size(), userId);
                            return logs;
                        } else {
                            logger.error("Ошибка при получении записей аудита: {}", apiResponse.getErrorMessage());
                            return Collections.emptyList();
                        }
                    } catch (Exception e) {
                        logger.error("Ошибка при разборе ответа", e);
                        return Collections.emptyList();
                    }
                } else {
                    logger.error("Ошибка при получении записей аудита, код: {}", statusCode);
                    return Collections.emptyList();
                }
            } catch (Exception e) {
                logger.error("Ошибка при обращении к API аудита", e);
                return Collections.emptyList();
            }
        });
    }
    
    /**
     * Получает записи аудита по указанному типу действия
     * @param actionType тип действия
     * @return список записей аудита
     */
    public CompletableFuture<List<AuditLog>> getAuditLogsByActionType(String actionType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(getApiUrl() + "/audit/action/" + actionType))
                        .header("Authorization", "Bearer " + authToken)
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();
                
                if (statusCode == 200) {
                    try {
                        ApiResponse<List<AuditLog>> apiResponse = OBJECT_MAPPER.readValue(
                                response.body(), 
                                new TypeReference<ApiResponse<List<AuditLog>>>() {});
                        
                        if (apiResponse.isSuccess()) {
                            List<AuditLog> logs = apiResponse.getData();
                            logger.debug("Получено {} записей аудита для действия {}", logs.size(), actionType);
                            return logs;
                        } else {
                            logger.error("Ошибка при получении записей аудита: {}", apiResponse.getErrorMessage());
                            return Collections.emptyList();
                        }
                    } catch (Exception e) {
                        logger.error("Ошибка при разборе ответа", e);
                        return Collections.emptyList();
                    }
                } else {
                    logger.error("Ошибка при получении записей аудита, код: {}", statusCode);
                    return Collections.emptyList();
                }
            } catch (Exception e) {
                logger.error("Ошибка при обращении к API аудита", e);
                return Collections.emptyList();
            }
        });
    }
} 