package client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import common.dto.ApiResponse;
import common.model.Promotion;
import common.model.PromoCode;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.PromotionStatisticsDTO;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import java.util.concurrent.CompletionException;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.charset.StandardCharsets;

/**
 * Сервис для работы с акциями и промокодами
 */
public class PromotionService {
    private static final Logger logger = LoggerFactory.getLogger(PromotionService.class);
    
    private static final String API_URL = ServiceUtils.getApiUrl();
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String authToken;
    private final Executor executor;
    
    public PromotionService(String authToken) {
        this.authToken = authToken;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.objectMapper.findAndRegisterModules();
        this.executor = Executors.newFixedThreadPool(5);
    }
    
    /**
     * Получает список всех акций
     * @return список акций
     */
    public CompletableFuture<List<Promotion>> getAllPromotions() {
        logger.debug("Запрос на получение всех акций");
        
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/promotions"))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    logger.debug("Получен ответ от сервера с кодом: {}", response.statusCode());
                    
                    if (response.statusCode() == 200) {
                        try {
                    String responseBody = response.body();
                            logger.debug("Тело ответа: {}", responseBody);
                    
                            ApiResponse<List<Promotion>> apiResponse = objectMapper.readValue(
                                    responseBody,
                                    new TypeReference<ApiResponse<List<Promotion>>>() {}
                            );
                            
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                List<Promotion> promotions = apiResponse.getData();
                                logger.debug("Успешно получено {} акций", promotions.size());
                                return promotions;
                            } else {
                                logger.error("Ошибка при получении акций: {}", 
                                    apiResponse.getErrorMessage() != null ? apiResponse.getErrorMessage() : "Неизвестная ошибка");
                                return new ArrayList<Promotion>();
                            }
                        } catch (Exception e) {
                            logger.error("Ошибка при обработке ответа сервера: {}", e.getMessage(), e);
                            return new ArrayList<Promotion>();
                        }
                    } else {
                        logger.error("Ошибка при получении акций. Код ответа: {}", response.statusCode());
                        return new ArrayList<Promotion>();
                    }
                })
                .exceptionally(e -> {
                    logger.error("Ошибка при выполнении запроса на получение акций: {}", e.getMessage(), e);
                    return new ArrayList<Promotion>();
                });
    }
    
    /**
     * Создает новую акцию
     * @param promotion акция
     * @return созданная акция
     */
    public CompletableFuture<Promotion> createPromotion(Promotion promotion) {
        logger.debug("Запрос на создание акции: {}", promotion.getName());
        
        try {
            String requestBody = objectMapper.writeValueAsString(promotion);
            logger.debug("Отправляемое тело запроса: {}", requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + authToken)
                    .uri(URI.create(API_URL + "/promotions"))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> {
                        int statusCode = response.statusCode();
                        String responseBody = response.body();
                        logger.debug("Ответ сервера: {}, тело: {}", statusCode, responseBody);
                        
                        if (statusCode == 200 || statusCode == 201) {
                            try {
                                ApiResponse<Promotion> apiResponse = objectMapper.readValue(
                                        responseBody,
                                        new TypeReference<ApiResponse<Promotion>>() {}
                                );
                                
                                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                    Promotion createdPromotion = apiResponse.getData();
                                    logger.info("Акция успешно создана: {}", createdPromotion.getId());
                                    return createdPromotion;
                                } else {
                                    logger.error("Ошибка создания акции: {}", apiResponse.getErrorMessage());
                                    // Показываем всплывающее сообщение с ошибкой
                                    String errorMsg = apiResponse.getErrorMessage() != null ? 
                                            apiResponse.getErrorMessage() : "Неизвестная ошибка";
                                    showErrorAlert("Ошибка создания акции", "Не удалось создать акцию: " + errorMsg);
                                    return null;
                                }
                            } catch (Exception e) {
                                logger.error("Ошибка парсинга ответа при создании акции", e);
                                showErrorAlert("Ошибка обработки ответа сервера", e.getMessage());
                                return null;
                            }
                        } else {
                            logger.error("Ошибка создания акции, код: {}, ответ: {}", statusCode, responseBody);
                            String errorMessage = "Код ошибки: " + statusCode;
                            try {
                                // Пытаемся извлечь сообщение об ошибке из ответа
                                ApiResponse<?> errorResponse = objectMapper.readValue(
                                        responseBody,
                                        new TypeReference<ApiResponse<?>>() {}
                                );
                                if (!errorResponse.isSuccess() && errorResponse.getErrorMessage() != null) {
                                    errorMessage = errorResponse.getErrorMessage();
                                }
                            } catch (Exception e) {
                                // Если не удалось распарсить ответ, используем исходное сообщение
                                logger.debug("Не удалось распарсить сообщение об ошибке", e);
                            }
                            
                            final String finalErrorMessage = errorMessage;
                            showErrorAlert("Ошибка создания акции", finalErrorMessage);
                            return null;
                        }
                    }, executor)
                    .exceptionally(e -> {
                        logger.error("Исключение при обработке запроса на создание акции", e);
                        showErrorAlert("Ошибка создания акции", "Не удалось отправить запрос: " + e.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Ошибка при подготовке запроса на создание акции", e);
            showErrorAlert("Ошибка создания акции", "Ошибка при подготовке запроса: " + e.getMessage());
            CompletableFuture<Promotion> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Обновляет акцию
     * @param promotion акция
     * @return обновленная акция
     */
    public CompletableFuture<Promotion> updatePromotion(Promotion promotion) {
        logger.debug("Запрос на обновление акции: {}", promotion.getName());
        
        try {
            String requestBody = objectMapper.writeValueAsString(promotion);
            logger.debug("Отправляемое тело запроса: {}", requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .PUT(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + authToken)
                    .uri(URI.create(API_URL + "/promotions/" + promotion.getId()))
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> {
                        int statusCode = response.statusCode();
                        String responseBody = response.body();
                        logger.debug("Получен ответ на запрос обновления акции, статус: {}, тело: {}", statusCode, responseBody);
                        
                        if (statusCode == 200) {
                            try {
                                ApiResponse<Promotion> apiResponse = objectMapper.readValue(
                                        responseBody,
                                        new TypeReference<ApiResponse<Promotion>>() {}
                                );
                                
                                if (apiResponse.isSuccess()) {
                                    Promotion updatedPromotion = apiResponse.getData();
                                    logger.info("Акция успешно обновлена: {}", updatedPromotion.getId());
                                    return updatedPromotion;
                                } else {
                                    logger.error("Ошибка обновления акции: {}", apiResponse.getErrorMessage());
                                    showErrorAlert("Ошибка обновления акции", apiResponse.getErrorMessage());
                                    return null;
                                }
                            } catch (Exception e) {
                                logger.error("Ошибка при обновлении акции", e);
                                showErrorAlert("Ошибка обновления акции", e.getMessage());
                                return null;
                            }
                        } else {
                            logger.error("Ошибка при обновлении акции, код: {}", statusCode);
                            showErrorAlert("Ошибка обновления акции", "Код ошибки: " + statusCode);
                            return null;
                        }
                    }, executor)
                    .exceptionally(e -> {
                        logger.error("Исключение при обработке запроса на обновление акции", e);
                        showErrorAlert("Ошибка обновления акции", e.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Ошибка при сериализации запроса", e);
            showErrorAlert("Ошибка обновления акции", e.getMessage());
            CompletableFuture<Promotion> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Удаляет акцию
     * @param promotionId ID акции
     * @return результат операции
     */
    public CompletableFuture<Boolean> deletePromotion(Long promotionId) {
        logger.debug("Запрос на удаление акции: {}", promotionId);
        
        HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/promotions/" + promotionId))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    logger.debug("Получен ответ на запрос удаления акции, статус: {}", statusCode);
                    
                    if (statusCode == 204) {
                        logger.debug("Акция успешно удалена");
                        return true;
                    } else {
                        logger.error("Ошибка при удалении акции, код: {}", statusCode);
                        return false;
                    }
                }, executor);
    }
    
    /**
     * Получает список всех промокодов
     * @return список промокодов
     */
    public CompletableFuture<List<PromoCode>> getAllPromoCodes() {
        logger.debug("Запрос на получение всех промокодов");
        
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/promo-codes"))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    String responseBody = response.body();
                    logger.debug("Получен ответ на запрос всех промокодов, статус: {}, body: {}", statusCode, responseBody);
                    
                    if (statusCode == 200) {
                        try {
                            ApiResponse<List<PromoCode>> apiResponse = objectMapper.readValue(
                                    responseBody,
                                    new TypeReference<ApiResponse<List<PromoCode>>>() {}
                            );
                            
                            if (apiResponse.isSuccess()) {
                                List<PromoCode> promoCodes = apiResponse.getData();
                                logger.info("Успешно получено {} промокодов", promoCodes.size());
                                return promoCodes;
                            } else {
                                logger.error("Ошибка при получении промокодов: {}", apiResponse.getErrorMessage());
                                return Collections.emptyList();
                            }
                        } catch (Exception e) {
                            logger.error("Ошибка при разборе ответа", e);
                            return Collections.emptyList();
                        }
                    } else {
                        logger.error("Ошибка при получении промокодов, код: {}", statusCode);
                        return Collections.emptyList();
                    }
                }, executor);
    }
    
    /**
     * Создает новый промокод
     * @param promoCode промокод
     * @return созданный промокод
     */
    public CompletableFuture<PromoCode> createPromoCode(PromoCode promoCode) {
        logger.debug("Запрос на создание промокода: {}", promoCode.getCode());
        
        try {
            // Создаем копию объекта для отправки, чтобы не модифицировать оригинал
            ObjectMapper objectMapperCopy = new ObjectMapper();
            objectMapperCopy.registerModule(new JavaTimeModule());
            objectMapperCopy.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            objectMapperCopy.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            
            String requestBody = objectMapperCopy.writeValueAsString(promoCode);
            logger.debug("Отправляемое тело запроса: {}", requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + authToken)
                    .uri(URI.create(API_URL + "/promo-codes"))
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> {
                        int statusCode = response.statusCode();
                        String responseBody = response.body();
                        
                        logger.debug("Получен ответ на запрос создания промокода, статус: {}, тело: {}", statusCode, responseBody);
                        
                        if (statusCode == 200) {
                            try {
                                ApiResponse<PromoCode> apiResponse = objectMapper.readValue(
                                        responseBody,
                                        new TypeReference<ApiResponse<PromoCode>>() {});
                                
                                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                    logger.info("Промокод успешно создан: {}", apiResponse.getData().getCode());
                                    return apiResponse.getData();
                                } else {
                                    String errorMessage = apiResponse.getErrorMessage() != null ? 
                                            apiResponse.getErrorMessage() : "Неизвестная ошибка";
                                    logger.error("Ошибка создания промокода: {}", errorMessage);
                                    throw new CompletionException(new RuntimeException(errorMessage));
                                }
                            } catch (Exception e) {
                                logger.error("Ошибка при обработке ответа от сервера: {}", e.getMessage());
                                throw new CompletionException(new RuntimeException("Ошибка при обработке ответа от сервера", e));
                            }
                        } else {
                            logger.error("Ошибка создания промокода. Код: {}", statusCode);
                            throw new CompletionException(new RuntimeException("Ошибка создания промокода. Код ошибки: " + statusCode));
                        }
                    })
                    .exceptionally(e -> {
                        logger.error("Ошибка создания промокода: {}", e.getMessage());
                        throw new CompletionException(e);
                    });
        } catch (Exception e) {
            logger.error("Ошибка при формировании запроса на создание промокода: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Обновляет промокод
     * @param promoCode промокод
     * @return обновленный промокод
     */
    public CompletableFuture<PromoCode> updatePromoCode(PromoCode promoCode) {
        logger.debug("Запрос на обновление промокода: {}", promoCode.getCode());
        
        try {
            if (promoCode.getId() == null) {
                logger.error("Невозможно обновить промокод без ID");
                CompletableFuture<PromoCode> future = new CompletableFuture<>();
                future.completeExceptionally(new IllegalArgumentException("ID промокода не может быть null"));
                return future;
            }
            
            String requestBody = objectMapper.writeValueAsString(promoCode);
            logger.debug("Отправляемое тело запроса: {}", requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .PUT(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + authToken)
                    .uri(URI.create(API_URL + "/promo-codes/" + promoCode.getId()))
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> {
                        int statusCode = response.statusCode();
                        String responseBody = response.body();
                        logger.debug("Получен ответ на запрос обновления промокода, статус: {}, тело: {}", 
                                statusCode, responseBody);
                        
                        if (statusCode == 200) {
                            try {
                                ApiResponse<PromoCode> apiResponse = objectMapper.readValue(
                                        responseBody,
                                        new TypeReference<ApiResponse<PromoCode>>() {}
                                );
                                
                                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                    PromoCode updatedPromoCode = apiResponse.getData();
                                    logger.info("Промокод успешно обновлен: {}", updatedPromoCode.getId());
                                    return updatedPromoCode;
                                } else {
                                    String errorMsg = apiResponse.getErrorMessage() != null ? 
                                            apiResponse.getErrorMessage() : "Неизвестная ошибка";
                                    logger.error("Ошибка обновления промокода: {}", errorMsg);
                                    showErrorAlert("Ошибка обновления промокода", errorMsg);
                                    return null;
                                }
                            } catch (Exception e) {
                                logger.error("Ошибка парсинга ответа при обновлении промокода", e);
                                showErrorAlert("Ошибка обработки ответа", "Произошла ошибка при обработке ответа сервера: " + e.getMessage());
                                return null;
                            }
                        } else {
                            String errorMsg = "Ошибка обновления промокода. Код ошибки: " + statusCode;
                            logger.error(errorMsg);
                            
                            // Пытаемся извлечь сообщение об ошибке из ответа
                            try {
                                ApiResponse<?> errorResponse = objectMapper.readValue(
                                        responseBody,
                                        new TypeReference<ApiResponse<?>>() {}
                                );
                                if (errorResponse != null && errorResponse.getErrorMessage() != null) {
                                    errorMsg += ". " + errorResponse.getErrorMessage();
                                }
                            } catch (Exception e) {
                                logger.debug("Не удалось распарсить ответ с ошибкой: {}", e.getMessage());
                            }
                            
                            if (statusCode == 404) {
                                errorMsg = "Промокод не найден на сервере. Возможно, он был удален.";
                            }
                            
                            showErrorAlert("Ошибка обновления промокода", errorMsg);
                            return null;
                        }
                    }, executor);
        } catch (Exception e) {
            logger.error("Ошибка при создании запроса на обновление промокода", e);
            CompletableFuture<PromoCode> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Удаляет промокод
     * @param promoCodeId ID промокода
     * @return true если удаление прошло успешно
     */
    public CompletableFuture<Boolean> deletePromoCode(Long promoCodeId) {
        logger.debug("Запрос на удаление промокода с ID: {}", promoCodeId);
        
        HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/promo-codes/" + promoCodeId))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    String responseBody = response.body();
                    logger.debug("Получен ответ на запрос удаления промокода, статус: {}, тело: {}", 
                            statusCode, responseBody);
                    
                    if (statusCode == 200 || statusCode == 204) {
                        try {
                            ApiResponse<?> apiResponse = objectMapper.readValue(
                                    responseBody,
                                    new TypeReference<ApiResponse<?>>() {}
                            );
                            
                            if (apiResponse.isSuccess()) {
                                logger.info("Промокод успешно удален: {}", promoCodeId);
                                return true;
                            } else {
                                String errorMsg = apiResponse.getErrorMessage() != null ? 
                                        apiResponse.getErrorMessage() : "Неизвестная ошибка";
                                logger.error("Ошибка удаления промокода: {}", errorMsg);
                                showErrorAlert("Ошибка удаления промокода", errorMsg);
                                return false;
                            }
                        } catch (Exception e) {
                            // Если ответ пустой, но статус успешный
                            if (responseBody == null || responseBody.trim().isEmpty()) {
                                logger.info("Промокод успешно удален (пустой ответ): {}", promoCodeId);
                        return true;
                            }
                            
                            logger.error("Ошибка парсинга ответа при удалении промокода", e);
                            showErrorAlert("Ошибка обработки ответа", e.getMessage());
                            return false;
                        }
                    } else {
                        logger.error("Ошибка удаления промокода, код: {}", statusCode);
                        String errorMessage = "Код ошибки: " + statusCode;
                        try {
                            ApiResponse<?> errorResponse = objectMapper.readValue(
                                    responseBody,
                                    new TypeReference<ApiResponse<?>>() {}
                            );
                            if (errorResponse.getErrorMessage() != null) {
                                errorMessage = errorResponse.getErrorMessage();
                            }
                        } catch (Exception e) {
                            logger.debug("Не удалось распарсить сообщение об ошибке", e);
                        }
                        
                        showErrorAlert("Ошибка удаления промокода", errorMessage);
                        return false;
                    }
                }, executor)
                .exceptionally(e -> {
                    logger.error("Исключение при удалении промокода", e);
                    showErrorAlert("Ошибка удаления промокода", "Не удалось соединиться с сервером: " + e.getMessage());
                    return false;
                });
    }
    
    /**
     * Получает статистику по акциям и промокодам
     * @return объект статистики
     */
    public CompletableFuture<PromotionStatistics> getPromotionStatistics() {
        logger.debug("Запрос на получение статистики акций и промокодов");
        
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/promotions/statistics"))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    String responseBody = response.body();
                    
                    logger.debug("Получен ответ на запрос статистики акций и промокодов, статус: {}, body: {}", 
                            statusCode, responseBody);
                    
                    if (statusCode == 200) {
                        try {
                            // Сервер возвращает объект статистики напрямую
                            ApiResponse<Map<String, Object>> apiResponse = objectMapper.readValue(
                                    responseBody,
                                    new TypeReference<ApiResponse<Map<String, Object>>>() {});
                            
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                Map<String, Object> statisticsData = apiResponse.getData();
                                logger.info("Успешно получена статистика акций");
                                
                                // Создаем объект статистики из полученных данных
                                PromotionStatistics stats = new PromotionStatistics();
                                
                                // Устанавливаем значения из ответа сервера
                                stats.setTotalPromotions(getIntValue(statisticsData.get("totalPromotions")));
                                stats.setActivePromotions(getIntValue(statisticsData.get("activePromotions")));
                                stats.setTotalPromoCodes(getIntValue(statisticsData.get("totalPromoCodes")));
                                stats.setUsedPromoCodes(getIntValue(statisticsData.get("usedPromoCodes")));
                                stats.setTotalBonusPoints(getIntValue(statisticsData.get("totalBonusPoints")));
                                stats.setTotalUsageCount(getIntValue(statisticsData.get("totalUsageCount")));
                                
                                // Рассчитываем производные значения
                                if (stats.getTotalUsageCount() > 0) {
                                    stats.setAverageBonusPoints((double) stats.getTotalBonusPoints() / stats.getTotalUsageCount());
                                } else {
                                    stats.setAverageBonusPoints(0.0);
                                        }
                                
                                if (stats.getTotalPromoCodes() > 0) {
                                    stats.setPromoCodeConversion((double) stats.getUsedPromoCodes() / stats.getTotalPromoCodes());
                                } else {
                                    stats.setPromoCodeConversion(0.0);
                                    }
                                    
                                // Устанавливаем данные по дням и категориям
                                            @SuppressWarnings("unchecked")
                                Map<String, Integer> dailyActivityData = (Map<String, Integer>) statisticsData.get("dailyActivityData");
                                if (dailyActivityData != null) {
                                    stats.setDailyActivityData(dailyActivityData);
                                } else {
                                    stats.setDailyActivityData(new HashMap<>());
                                }
                                
                                            @SuppressWarnings("unchecked")
                                Map<String, Integer> promoCodeUsageData = (Map<String, Integer>) statisticsData.get("promoCodeUsageData");
                                if (promoCodeUsageData != null) {
                                    stats.setPromoCodeUsageData(promoCodeUsageData);
                                } else {
                                    stats.setPromoCodeUsageData(new HashMap<>());
                                }
                                
                                            @SuppressWarnings("unchecked")
                                Map<String, Integer> promotionUsageData = (Map<String, Integer>) statisticsData.get("promotionUsageData");
                                if (promotionUsageData != null) {
                                    stats.setPromotionUsageData(promotionUsageData);
                                } else {
                                    stats.setPromotionUsageData(new HashMap<>());
                                }
                                
                                return stats;
                            } else {
                                String errorMsg = apiResponse.getErrorMessage() != null ? 
                                        apiResponse.getErrorMessage() : "Неизвестная ошибка";
                                logger.error("Ошибка получения статистики акций: {}", errorMsg);
                                throw new CompletionException(new RuntimeException("Ошибка получения статистики: " + errorMsg));
                            }
                        } catch (Exception e) {
                            logger.error("Ошибка при обработке ответа статистики", e);
                            throw new CompletionException(new RuntimeException("Ошибка при обработке ответа статистики", e));
                        }
                    } else {
                        logger.error("Ошибка получения статистики акций. Код ошибки: {}", statusCode);
                        throw new CompletionException(new RuntimeException("Ошибка получения статистики. Код ошибки: " + statusCode));
                    }
                });
    }
    
    /**
     * Помогает преобразовать Object в Integer
     */
    private Integer getIntValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            logger.error("Не удалось преобразовать {} в Integer", value);
            return null;
        }
    }
    
    /**
     * Объединяет два Map, складывая значения для ключей, которые есть в обоих Map
     */
    private void aggregateMap(Map<String, Integer> target, Map<String, Integer> source) {
        if (source == null) return;
        
        source.forEach((key, value) -> {
            target.compute(key, (k, v) -> (v == null) ? value : v + value);
        });
    }
    
    /**
     * Класс для представления статистики акций
     */
    public static class PromotionStatistics {
        private int totalPromotions;
        private int activePromotions;
        private int totalPromoCodes;
        private int usedPromoCodes;
        private int totalBonusPoints;
        private int totalUsageCount;
        private double averageBonusPoints;
        private double promoCodeConversion;
        private Map<String, Integer> promotionUsageData = new HashMap<>();
        private Map<String, Integer> promoCodeUsageData = new HashMap<>();
        private Map<String, Integer> dailyActivityData = new HashMap<>();
        
        public int getTotalPromotions() {
            return totalPromotions;
        }
        
        public void setTotalPromotions(int totalPromotions) {
            this.totalPromotions = totalPromotions;
        }
        
        public int getActivePromotions() {
            return activePromotions;
        }
        
        public void setActivePromotions(int activePromotions) {
            this.activePromotions = activePromotions;
        }
        
        public int getTotalPromoCodes() {
            return totalPromoCodes;
        }
        
        public void setTotalPromoCodes(int totalPromoCodes) {
            this.totalPromoCodes = totalPromoCodes;
        }
        
        public int getUsedPromoCodes() {
            return usedPromoCodes;
        }
        
        public void setUsedPromoCodes(int usedPromoCodes) {
            this.usedPromoCodes = usedPromoCodes;
        }
        
        public int getTotalBonusPoints() {
            return totalBonusPoints;
        }
        
        public void setTotalBonusPoints(int totalBonusPoints) {
            this.totalBonusPoints = totalBonusPoints;
        }
        
        public int getTotalUsageCount() {
            return totalUsageCount;
        }
        
        public void setTotalUsageCount(int totalUsageCount) {
            this.totalUsageCount = totalUsageCount;
        }
        
        public double getAverageBonusPoints() {
            return averageBonusPoints;
        }
        
        public void setAverageBonusPoints(double averageBonusPoints) {
            this.averageBonusPoints = averageBonusPoints;
        }
        
        public double getPromoCodeConversion() {
            return promoCodeConversion;
        }
        
        public void setPromoCodeConversion(double promoCodeConversion) {
            this.promoCodeConversion = promoCodeConversion;
        }
        
        public Map<String, Integer> getPromotionUsageData() {
            return promotionUsageData;
        }
        
        public void setPromotionUsageData(Map<String, Integer> promotionUsageData) {
            this.promotionUsageData = promotionUsageData;
        }
        
        public Map<String, Integer> getPromoCodeUsageData() {
            return promoCodeUsageData;
        }
        
        public void setPromoCodeUsageData(Map<String, Integer> promoCodeUsageData) {
            this.promoCodeUsageData = promoCodeUsageData;
        }
        
        public Map<String, Integer> getDailyActivityData() {
            return dailyActivityData;
        }
        
        public void setDailyActivityData(Map<String, Integer> dailyActivityData) {
            this.dailyActivityData = dailyActivityData;
        }
    }
    
    /**
     * Показывает диалоговое окно с ошибкой
     * @param title заголовок окна
     * @param message сообщение об ошибке
     */
    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
} 