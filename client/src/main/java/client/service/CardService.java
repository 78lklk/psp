package client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import common.dto.ApiResponse;
import common.model.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Сервис для работы с картами лояльности
 */
public class CardService {
    private static final Logger logger = LoggerFactory.getLogger(CardService.class);
    private static final String API_URL = ServiceUtils.getApiUrl();
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String authToken;
    private final Executor executor;

    /**
     * Создает новый сервис для работы с картами
     * @param authToken токен авторизации
     */
    public CardService(String authToken) {
        this.authToken = authToken;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        this.executor = Executors.newFixedThreadPool(5);
    }
    
    /**
     * Получает список всех карт
     * @return список карт
     */
    public CompletableFuture<List<Card>> getAllCards() {
        logger.debug("Requesting all cards");
        
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/cards"))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    logger.debug("Received response for cards request, status: {}", statusCode);
                    
                    if (statusCode == 200) {
                        try {
                            ApiResponse<List<Card>> apiResponse = objectMapper.readValue(
                                    response.body(),
                                    new TypeReference<ApiResponse<List<Card>>>() {}
                            );
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                logger.debug("Successfully parsed {} cards", apiResponse.getData().size());
                                return apiResponse.getData();
                } else {
                                logger.error("API returned error: {}", apiResponse.getErrorMessage());
                    return Collections.emptyList();
                }
            } catch (Exception e) {
                            logger.error("Error parsing response", e);
                            return Collections.emptyList();
                        }
                    } else {
                        logger.error("Error retrieving card list, status code: {}", statusCode);
                return Collections.emptyList();
            }
                }, executor);
    }
    
    /**
     * Получает карту по ID
     * @param cardId ID карты
     * @return данные карты
     */
    public CompletableFuture<Card> getCardById(Long cardId) {
        logger.debug("Запрос карты по ID: {}", cardId);
        
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/cards/" + cardId))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    logger.debug("Получен ответ на запрос карты, статус: {}", statusCode);
                    
                    if (statusCode == 200) {
                        try {
                            ApiResponse<Card> apiResponse = objectMapper.readValue(
                                    response.body(),
                                    new TypeReference<ApiResponse<Card>>() {}
                            );
                            return apiResponse.getData();
                        } catch (Exception e) {
                            logger.error("Ошибка при разборе ответа", e);
                            return null;
                        }
                } else {
                        logger.error("Ошибка при получении карты, код: {}", statusCode);
                        return null;
                    }
                }, executor);
    }
    
    /**
     * Создает новую карту
     * @param card данные карты
     * @return созданная карта
     */
    public CompletableFuture<Card> createCard(Card card) {
        logger.debug("Создание новой карты");
        
        try {
            String requestBody = objectMapper.writeValueAsString(card);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + authToken)
                    .uri(URI.create(API_URL + "/cards"))
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> {
                        int statusCode = response.statusCode();
                        logger.debug("Received card creation response, status: {}", statusCode);
                        
                        if (statusCode == 200 || statusCode == 201) {
                            try {
                                ApiResponse<Card> apiResponse = objectMapper.readValue(
                                        response.body(),
                                        new TypeReference<ApiResponse<Card>>() {}
                                );
                                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                    logger.debug("Card created successfully with ID: {}", apiResponse.getData().getId());
                                    return apiResponse.getData();
                                } else {
                                    logger.error("Card creation failed: {}", apiResponse.getErrorMessage());
                                    return null;
                                }
                            } catch (Exception e) {
                                logger.error("Error parsing response", e);
                                return null;
                            }
                } else {
                            logger.error("Error creating card, code: {}", statusCode);
                            return null;
                }
                    }, executor);
            } catch (Exception e) {
            logger.error("Ошибка при подготовке запроса", e);
            CompletableFuture<Card> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Обновляет карту
     * @param card данные карты
     * @return обновленная карта
     */
    public CompletableFuture<Card> updateCard(Card card) {
        logger.debug("Обновление карты с ID: {}", card.getId());
        
        try {
            String requestBody = objectMapper.writeValueAsString(card);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + authToken)
                    .uri(URI.create(API_URL + "/cards/" + card.getId()))
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> {
                        int statusCode = response.statusCode();
                        logger.debug("Получен ответ на обновление карты, статус: {}", statusCode);
                        
                        if (statusCode == 200) {
                            try {
                                ApiResponse<Card> apiResponse = objectMapper.readValue(
                                        response.body(),
                                        new TypeReference<ApiResponse<Card>>() {}
                                );
                                return apiResponse.getData();
                            } catch (Exception e) {
                                logger.error("Ошибка при разборе ответа", e);
                                return null;
                            }
                } else {
                            logger.error("Ошибка при обновлении карты, код: {}", statusCode);
                            return null;
                }
                    }, executor);
            } catch (Exception e) {
            logger.error("Ошибка при подготовке запроса", e);
            CompletableFuture<Card> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Удаляет карту
     * @param cardId ID карты
     * @return результат операции
     */
    public CompletableFuture<Boolean> deleteCard(Long cardId) {
        logger.debug("Удаление карты с ID: {}", cardId);
        
        HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/cards/" + cardId))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    logger.debug("Получен ответ на удаление карты, статус: {}", statusCode);
                    return statusCode == 204 || statusCode == 200;
                }, executor);
    }
    
    /**
     * Начисляет баллы на карту
     * @param cardId ID карты
     * @param points количество баллов
     * @param description описание операции
     * @return обновленная карта
     */
    public CompletableFuture<Card> addPoints(Long cardId, int points, String description) {
        logger.debug("Начисление {} баллов на карту с ID: {}", points, cardId);
        
        // Проверяем доступность сервера
        if (!ServiceUtils.isServerAvailable()) {
            logger.error("Сервер недоступен. Не удалось начислить баллы на карту {}", cardId);
            CompletableFuture<Card> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Сервер недоступен. Проверьте сетевое соединение."));
            return future;
        }
        
        try {
            String requestBody = objectMapper.writeValueAsString(
                    Map.of("points", points, "description", description)
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + authToken)
                    .uri(URI.create(API_URL + "/cards/" + cardId + "/add"))
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> {
                        int statusCode = response.statusCode();
                        String responseBody = response.body();
                        ServiceUtils.logResponseDetails(statusCode, responseBody, logger);
                        
                        if (statusCode == 200) {
                            try {
                                ApiResponse<Card> apiResponse = objectMapper.readValue(
                                        responseBody,
                                        new TypeReference<ApiResponse<Card>>() {}
                                );
                                
                                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                    logger.debug("Баллы успешно начислены на карту {}", cardId);
                                    return apiResponse.getData();
                } else {
                                    String errorMessage = apiResponse.getErrorMessage() != null 
                                        ? apiResponse.getErrorMessage() 
                                        : "Неизвестная ошибка при начислении баллов";
                                    logger.error("Ошибка API при начислении баллов: {}", errorMessage);
                                    return null;
                }
            } catch (Exception e) {
                                logger.error("Ошибка при разборе ответа на начисление баллов", e);
                                return null;
                            }
                        } else {
                            String errorMessage = ServiceUtils.getErrorMessage(statusCode);
                            logger.error("Ошибка при начислении баллов, код: {}, сообщение: {}", statusCode, errorMessage);
                            return null;
                        }
                    }, executor)
                    .exceptionally(e -> {
                        logger.error("Исключение при выполнении запроса на начисление баллов", e);
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Ошибка при подготовке запроса на начисление баллов", e);
            CompletableFuture<Card> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Списывает баллы с карты
     * @param cardId ID карты
     * @param points количество баллов
     * @param description описание операции
     * @return обновленная карта
     */
    public CompletableFuture<Card> deductPoints(Long cardId, int points, String description) {
        logger.debug("Списание {} баллов с карты с ID: {}", points, cardId);
        
        // Проверяем доступность сервера
        if (!ServiceUtils.isServerAvailable()) {
            logger.error("Сервер недоступен. Не удалось списать баллы с карты {}", cardId);
            CompletableFuture<Card> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Сервер недоступен. Проверьте сетевое соединение."));
            return future;
        }
        
        try {
            String requestBody = objectMapper.writeValueAsString(
                    Map.of("points", points, "description", description)
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + authToken)
                    .uri(URI.create(API_URL + "/cards/" + cardId + "/deduct"))
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> {
                        int statusCode = response.statusCode();
                        String responseBody = response.body();
                        ServiceUtils.logResponseDetails(statusCode, responseBody, logger);
                        
                        if (statusCode == 200) {
                            try {
                                ApiResponse<Card> apiResponse = objectMapper.readValue(
                                        responseBody,
                                        new TypeReference<ApiResponse<Card>>() {}
                                );
                                
                                if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                    logger.debug("Баллы успешно списаны с карты {}", cardId);
                                    return apiResponse.getData();
                } else {
                                    String errorMessage = apiResponse.getErrorMessage() != null 
                                        ? apiResponse.getErrorMessage() 
                                        : "Неизвестная ошибка при списании баллов";
                                    logger.error("Ошибка API при списании баллов: {}", errorMessage);
                                    return null;
                }
            } catch (Exception e) {
                                logger.error("Ошибка при разборе ответа на списание баллов", e);
                                return null;
                            }
                        } else {
                            String errorMessage = ServiceUtils.getErrorMessage(statusCode);
                            logger.error("Ошибка при списании баллов, код: {}, сообщение: {}", statusCode, errorMessage);
                            return null;
                        }
                    }, executor)
                    .exceptionally(e -> {
                        logger.error("Исключение при выполнении запроса на списание баллов", e);
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Ошибка при подготовке запроса на списание баллов", e);
            CompletableFuture<Card> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Получает карту по номеру
     * @param cardNumber номер карты
     * @return карта (или пустой Optional, если не найдена)
     */
    public CompletableFuture<Optional<Card>> getCardByNumber(String cardNumber) {
        logger.debug("Запрос карты по номеру: {}", cardNumber);
        
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/cards/number/" + cardNumber))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    logger.debug("Получен ответ на запрос карты по номеру, статус: {}", statusCode);
                    
                    if (statusCode == 200) {
                        try {
                            ApiResponse<Card> apiResponse = objectMapper.readValue(
                                    response.body(),
                                    new TypeReference<ApiResponse<Card>>() {}
                            );
                            
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                return Optional.of(apiResponse.getData());
                } else {
                                logger.debug("Карта с номером {} не найдена", cardNumber);
                    return Optional.empty();
                }
            } catch (Exception e) {
                            logger.error("Ошибка при разборе ответа", e);
                            return Optional.empty();
                        }
                    } else {
                        logger.error("Ошибка при получении карты, код: {}", statusCode);
                return Optional.empty();
            }
                }, executor);
    }
    
    /**
     * Получает карты пользователя
     * @param userId идентификатор пользователя
     * @return список карт
     */
    public CompletableFuture<List<Card>> getCardsByUserId(Long userId) {
        logger.debug("Запрос карт пользователя: {}", userId);
        
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Authorization", "Bearer " + authToken)
                .uri(URI.create(API_URL + "/cards/user/" + userId))
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(response -> {
                    int statusCode = response.statusCode();
                    logger.debug("Получен ответ на запрос карт пользователя, статус: {}", statusCode);
                    
                    if (statusCode == 200) {
                        try {
                            ApiResponse<List<Card>> apiResponse = objectMapper.readValue(
                                    response.body(),
                                    objectMapper.getTypeFactory().constructParametricType(
                                            ApiResponse.class,
                                            objectMapper.getTypeFactory().constructCollectionType(List.class, Card.class)
                                    )
                            );
                            
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                return apiResponse.getData();
                            } else {
                                logger.debug("Карты пользователя {} не найдены", userId);
                                return Collections.emptyList();
                            }
            } catch (Exception e) {
                            logger.error("Ошибка при разборе ответа", e);
                            return Collections.emptyList();
                        }
                    } else {
                        logger.error("Ошибка при получении карт пользователя, код: {}", statusCode);
                        return Collections.emptyList();
                    }
                }, executor);
    }

    /**
     * Получает карты пользователя (алиас для getCardsByUserId)
     * @param userId идентификатор пользователя
     * @return список карт
     */
    public CompletableFuture<List<Card>> getUserCards(Long userId) {
        return getCardsByUserId(userId);
    }

    /**
     * Ensures that a runnable is executed on the JavaFX Application Thread
     * @param runnable the runnable to execute
     */
    private void runOnFxThread(Runnable runnable) {
        if (javafx.application.Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            javafx.application.Platform.runLater(runnable);
        }
    }
} 