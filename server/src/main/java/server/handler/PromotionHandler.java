package server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import common.dto.ApiResponse;
import common.model.Promotion;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpResponseStatus;
import server.service.PromotionService;
import server.service.PromotionServiceImpl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обработчик запросов для управления акциями
 */
public class PromotionHandler extends AbstractRequestHandler {
    private static final String URL_PATTERN = "/api/promotions.*";
    private final PromotionService promotionService;
    private final ObjectMapper objectMapper;

    public PromotionHandler() {
        super();
        this.promotionService = new PromotionServiceImpl();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.objectMapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
        this.objectMapper.findAndRegisterModules();
    }

    @Override
    protected String getUrlPattern() {
        return URL_PATTERN;
    }
    
    @Override
    protected String getMethod() {
        // Поддерживает все HTTP методы
        return ".*";
    }

    @Override
    public boolean handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String path = request.uri().split("\\?")[0];
        String method = request.method().name();
        String queryString = request.uri().contains("?") ? 
                request.uri().substring(request.uri().indexOf("?") + 1) : "";

        // GET /api/promotions - получить все акции или активные на дату
        if (path.equals("/api/promotions") && method.equals(HttpMethod.GET.name())) {
            return handleGetPromotions(ctx, queryString);
        }

        // GET /api/promotions/active - получить активные акции
        if (path.equals("/api/promotions/active") && method.equals(HttpMethod.GET.name())) {
            return handleGetActivePromotions(ctx);
        }

        // GET /api/promotions/{id} - получить акцию по ID
        Pattern idPattern = Pattern.compile("/api/promotions/(\\d+)");
        Matcher idMatcher = idPattern.matcher(path);
        if (idMatcher.matches() && method.equals(HttpMethod.GET.name())) {
            Long promotionId = Long.parseLong(idMatcher.group(1));
            return handleGetPromotionById(ctx, promotionId);
        }

        // POST /api/promotions - создать акцию
        if (path.equals("/api/promotions") && method.equals(HttpMethod.POST.name())) {
            String body = request.content().toString(StandardCharsets.UTF_8);
            return handleCreatePromotion(ctx, body);
        }

        // PUT /api/promotions/{id} - обновить акцию
        if (idMatcher.matches() && method.equals(HttpMethod.PUT.name())) {
            Long promotionId = Long.parseLong(idMatcher.group(1));
            String body = request.content().toString(StandardCharsets.UTF_8);
            return handleUpdatePromotion(ctx, promotionId, body);
        }

        // DELETE /api/promotions/{id} - удалить акцию
        if (idMatcher.matches() && method.equals(HttpMethod.DELETE.name())) {
            Long promotionId = Long.parseLong(idMatcher.group(1));
            return handleDeletePromotion(ctx, promotionId);
        }

        // POST /api/promotions/{id}/activate - активировать акцию
        Pattern activatePattern = Pattern.compile("/api/promotions/(\\d+)/activate");
        Matcher activateMatcher = activatePattern.matcher(path);
        if (activateMatcher.matches() && method.equals(HttpMethod.POST.name())) {
            Long promotionId = Long.parseLong(activateMatcher.group(1));
            return handleActivatePromotion(ctx, promotionId);
        }

        // POST /api/promotions/{id}/deactivate - деактивировать акцию
        Pattern deactivatePattern = Pattern.compile("/api/promotions/(\\d+)/deactivate");
        Matcher deactivateMatcher = deactivatePattern.matcher(path);
        if (deactivateMatcher.matches() && method.equals(HttpMethod.POST.name())) {
            Long promotionId = Long.parseLong(deactivateMatcher.group(1));
            return handleDeactivatePromotion(ctx, promotionId);
        }

        // GET /api/promotions/statistics - получить статистику по акциям
        if (path.equals("/api/promotions/statistics") && method.equals(HttpMethod.GET.name())) {
            return handleGetPromotionStatistics(ctx);
        }

        return false;
    }

    /**
     * Обрабатывает запрос на получение акций
     */
    private boolean handleGetPromotions(ChannelHandlerContext ctx, String queryString) {
        try {
            // Проверяем параметры запроса
            LocalDate date = null;
            if (!queryString.isEmpty()) {
                Pattern datePattern = Pattern.compile("date=([^&]+)");
                Matcher dateMatcher = datePattern.matcher(queryString);
                if (dateMatcher.find()) {
                    try {
                        date = LocalDate.parse(dateMatcher.group(1), DateTimeFormatter.ISO_DATE);
                    } catch (DateTimeParseException e) {
                        logger.warn("Некорректный формат даты: {}", dateMatcher.group(1));
                    }
                }
            }

            List<Promotion> promotions;
            if (date != null) {
                promotions = promotionService.getPromotionsActiveOnDate(date);
                logger.debug("Запрос акций, активных на дату: {}", date);
            } else {
                promotions = promotionService.getAllPromotions();
                logger.debug("Запрос всех акций");
            }

            ApiResponse<List<Promotion>> response = ApiResponse.success(promotions);
            String responseJson = objectMapper.writeValueAsString(response);

            // Устанавливаем явно кодировку UTF-8 для ответа
            sendJsonResponse(ctx, responseJson, HttpResponseStatus.OK);
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при получении акций", e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
    }

    /**
     * Обрабатывает запрос на получение активных акций
     */
    private boolean handleGetActivePromotions(ChannelHandlerContext ctx) {
        try {
            List<Promotion> activePromotions = promotionService.getActivePromotions();
            logger.debug("Запрос активных акций. Количество: {}", activePromotions.size());

            ApiResponse<List<Promotion>> response = ApiResponse.success(activePromotions);
            String responseJson = objectMapper.writeValueAsString(response);

            // Устанавливаем явно кодировку UTF-8 для ответа
            sendJsonResponse(ctx, responseJson, HttpResponseStatus.OK);
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при получении активных акций", e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
    }

    /**
     * Обрабатывает запрос на получение акции по ID
     */
    private boolean handleGetPromotionById(ChannelHandlerContext ctx, Long promotionId) {
        try {
            Optional<Promotion> promotionOptional = promotionService.getPromotionById(promotionId);
            logger.debug("Запрос акции по ID: {}", promotionId);

            if (promotionOptional.isPresent()) {
                ApiResponse<Promotion> response = ApiResponse.success(promotionOptional.get());
                String responseJson = objectMapper.writeValueAsString(response);

                // Устанавливаем явно кодировку UTF-8 для ответа
                sendJsonResponse(ctx, responseJson, HttpResponseStatus.OK);
            } else {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Акция не найдена");
            }
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при получении акции по ID: {}", promotionId, e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
    }

    /**
     * Обрабатывает запрос на создание акции
     */
    private boolean handleCreatePromotion(ChannelHandlerContext ctx, String body) {
        try {
            logger.debug("Создание новой акции. Запрос: {}", body);
            Promotion promotion = objectMapper.readValue(body, Promotion.class);
            
            // Проверяем обязательные поля
            if (promotion.getName() == null || promotion.getName().trim().isEmpty()) {
                ApiResponse<String> response = ApiResponse.error("Имя акции обязательно для заполнения");
                String responseJson = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, responseJson);
                return true;
            }
            
            // Создаем акцию
            Promotion createdPromotion = promotionService.createPromotion(promotion);
            
            if (createdPromotion == null) {
                ApiResponse<String> response = ApiResponse.error("Не удалось создать акцию. Проверьте данные и попробуйте снова.");
                String responseJson = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, responseJson);
                return true;
            }
            
            ApiResponse<Promotion> response = ApiResponse.success(createdPromotion);
            String responseJson = objectMapper.writeValueAsString(response);
            
            sendJsonResponse(ctx, responseJson, HttpResponseStatus.CREATED);
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при создании акции", e);
            try {
                ApiResponse<String> response = ApiResponse.error("Ошибка при создании акции: " + e.getMessage());
                String responseJson = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, responseJson);
            } catch (JsonProcessingException ex) {
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Ошибка при обработке запроса");
            }
            return true;
        }
    }

    /**
     * Обрабатывает запрос на обновление акции
     */
    private boolean handleUpdatePromotion(ChannelHandlerContext ctx, Long promotionId, String body) {
        try {
            Promotion promotion = objectMapper.readValue(body, Promotion.class);
            logger.debug("Обновление акции с ID: {}", promotionId);

            // Проверяем существование акции
            Optional<Promotion> existingPromotion = promotionService.getPromotionById(promotionId);
            if (existingPromotion.isEmpty()) {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Акция не найдена");
                return true;
            }

            // Устанавливаем ID из пути
            promotion.setId(promotionId);

            Optional<Promotion> updatedPromotionOpt = promotionService.updatePromotion(promotionId, promotion);
            if (updatedPromotionOpt.isEmpty()) {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Не удалось обновить акцию");
                return true;
            }
            
            ApiResponse<Promotion> response = ApiResponse.success(updatedPromotionOpt.get());
            String responseJson = objectMapper.writeValueAsString(response);

            // Устанавливаем явно кодировку UTF-8 для ответа
            sendJsonResponse(ctx, responseJson, HttpResponseStatus.OK);
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при обновлении акции с ID: {}", promotionId, e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
    }

    /**
     * Обрабатывает запрос на удаление акции
     */
    private boolean handleDeletePromotion(ChannelHandlerContext ctx, Long promotionId) {
        try {
            boolean success = promotionService.deletePromotion(promotionId);
            logger.debug("Удаление акции с ID: {}", promotionId);

            if (success) {
                ApiResponse<Void> response = ApiResponse.success(null);
                String responseJson = objectMapper.writeValueAsString(response);

                // Устанавливаем явно кодировку UTF-8 для ответа
                sendJsonResponse(ctx, responseJson, HttpResponseStatus.NO_CONTENT);
            } else {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Акция не найдена");
            }
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при удалении акции с ID: {}", promotionId, e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
    }

    /**
     * Обрабатывает запрос на активацию акции
     */
    private boolean handleActivatePromotion(ChannelHandlerContext ctx, Long promotionId) {
        try {
            boolean success = promotionService.activatePromotion(promotionId);
            logger.debug("Активация акции с ID: {}", promotionId);

            if (success) {
                ApiResponse<Void> response = ApiResponse.success(null);
                String responseJson = objectMapper.writeValueAsString(response);

                // Устанавливаем явно кодировку UTF-8 для ответа
                sendJsonResponse(ctx, responseJson, HttpResponseStatus.NO_CONTENT);
            } else {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Акция не найдена");
            }
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при активации акции с ID: {}", promotionId, e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
    }

    /**
     * Обрабатывает запрос на деактивацию акции
     */
    private boolean handleDeactivatePromotion(ChannelHandlerContext ctx, Long promotionId) {
        try {
            boolean success = promotionService.deactivatePromotion(promotionId);
            logger.debug("Деактивация акции с ID: {}", promotionId);

            if (success) {
                ApiResponse<Void> response = ApiResponse.success(null);
                String responseJson = objectMapper.writeValueAsString(response);

                // Устанавливаем явно кодировку UTF-8 для ответа
                sendJsonResponse(ctx, responseJson, HttpResponseStatus.NO_CONTENT);
            } else {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Акция не найдена");
            }
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при деактивации акции с ID: {}", promotionId, e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
    }

    /**
     * Обрабатывает запрос на получение статистики по акциям
     */
    private boolean handleGetPromotionStatistics(ChannelHandlerContext ctx) {
        try {
            logger.debug("Запрос на получение статистики по акциям");
            
            java.util.Map<String, Object> statistics = promotionService.getPromotionStatistics();
            
            ApiResponse<java.util.Map<String, Object>> response = ApiResponse.success(statistics);
            String responseJson = objectMapper.writeValueAsString(response);

            // Устанавливаем явно кодировку UTF-8 для ответа
            sendJsonResponse(ctx, responseJson, HttpResponseStatus.OK);
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при получении статистики по акциям", e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
    }

    private void sendJsonResponse(ChannelHandlerContext ctx, String json, HttpResponseStatus status) {
        try {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, status, 
                    Unpooled.copiedBuffer(json, StandardCharsets.UTF_8));
            
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization");
            
            ctx.writeAndFlush(response);
        } catch (Exception e) {
            logger.error("Ошибка при отправке JSON ответа", e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Ошибка при формировании ответа");
        }
    }
} 