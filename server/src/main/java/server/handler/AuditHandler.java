package server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.ApiResponse;
import common.model.AuditLog;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.service.AuditService;
import server.service.AuditServiceImpl;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обработчик запросов для аудита действий пользователей
 */
public class AuditHandler extends AbstractRequestHandler {
    private static final String URL_PATTERN = "/api/audit.*";
    private final AuditService auditService;

    public AuditHandler() {
        super();
        this.auditService = new AuditServiceImpl();
    }

    @Override
    protected String getUrlPattern() {
        return URL_PATTERN;
    }
    
    @Override
    protected String getMethod() {
        return HttpMethod.GET.name();
    }

    @Override
    public boolean handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String path = request.uri().split("\\?")[0];
        String method = request.method().name();
        String queryString = request.uri().contains("?") ? 
                request.uri().substring(request.uri().indexOf("?") + 1) : "";

        // GET /api/audit - получить все записи аудита или с параметрами
        if (path.equals("/api/audit") && method.equals(HttpMethod.GET.name())) {
            return handleGetAuditLogs(ctx, queryString);
        }

        // GET /api/audit/user/{userId} - получить записи аудита для пользователя
        Pattern userPattern = Pattern.compile("/api/audit/user/(\\d+)");
        Matcher userMatcher = userPattern.matcher(path);
        if (userMatcher.matches() && method.equals(HttpMethod.GET.name())) {
            Long userId = Long.parseLong(userMatcher.group(1));
            return handleGetAuditLogsByUser(ctx, userId);
        }

        // GET /api/audit/action/{actionType} - получить записи аудита по типу действия
        Pattern actionPattern = Pattern.compile("/api/audit/action/([\\w-]+)");
        Matcher actionMatcher = actionPattern.matcher(path);
        if (actionMatcher.matches() && method.equals(HttpMethod.GET.name())) {
            String actionType = actionMatcher.group(1);
            return handleGetAuditLogsByActionType(ctx, actionType);
        }

        return false;
    }

    /**
     * Обрабатывает запрос на получение записей аудита
     */
    private boolean handleGetAuditLogs(ChannelHandlerContext ctx, String queryString) {
        try {
            List<AuditLog> logs;
            
            // Обработка параметров запроса
            if (!queryString.isEmpty()) {
                Map<String, String> queryParams = parseQueryString(queryString);
                
                // Фильтр по дате
                if (queryParams.containsKey("from") && queryParams.containsKey("to")) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                        LocalDateTime from = LocalDateTime.parse(queryParams.get("from"), formatter);
                        LocalDateTime to = LocalDateTime.parse(queryParams.get("to"), formatter);
                        
                        logger.debug("Запрос записей аудита за период с {} по {}", from, to);
                        logs = auditService.getAuditLogsByPeriod(from, to);
                    } catch (DateTimeParseException e) {
                        logger.error("Ошибка парсинга дат", e);
                        sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Неверный формат даты");
                        return true;
                    }
                }
                // Фильтр по пользователю
                else if (queryParams.containsKey("userId")) {
                    try {
                        Long userId = Long.parseLong(queryParams.get("userId"));
                        logger.debug("Запрос записей аудита для пользователя {}", userId);
                        logs = auditService.getAuditLogsByUser(userId);
                    } catch (NumberFormatException e) {
                        logger.error("Ошибка парсинга ID пользователя", e);
                        sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Неверный формат ID пользователя");
                        return true;
                    }
                }
                // Фильтр по типу действия
                else if (queryParams.containsKey("actionType")) {
                    String actionType = queryParams.get("actionType");
                    logger.debug("Запрос записей аудита по типу действия {}", actionType);
                    logs = auditService.getAuditLogsByActionType(actionType);
                }
                // Нет подходящих фильтров
                else {
                    logger.debug("Запрос всех записей аудита");
                    logs = auditService.getAllAuditLogs();
                }
            } else {
                logger.debug("Запрос всех записей аудита");
                logs = auditService.getAllAuditLogs();
            }

            ApiResponse<List<AuditLog>> response = ApiResponse.success(logs);
            String responseJson = objectMapper.writeValueAsString(response);

            sendSuccessResponse(ctx, responseJson);
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при получении записей аудита", e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
    }
    
    /**
     * Парсит строку запроса в карту параметров
     */
    private Map<String, String> parseQueryString(String queryString) {
        Map<String, String> params = new HashMap<>();
        if (queryString == null || queryString.isEmpty()) {
            return params;
        }
        
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                try {
                    // Декодирование URL-encoded параметров
                    key = URLDecoder.decode(key, StandardCharsets.UTF_8);
                    value = URLDecoder.decode(value, StandardCharsets.UTF_8);
                    params.put(key, value);
                } catch (Exception e) {
                    logger.error("Ошибка декодирования параметров", e);
                }
            }
        }
        
        return params;
    }

    /**
     * Обрабатывает запрос на получение записей аудита для пользователя
     */
    private boolean handleGetAuditLogsByUser(ChannelHandlerContext ctx, Long userId) {
        try {
            List<AuditLog> logs = auditService.getAuditLogsByUser(userId);
            logger.debug("Запрос записей аудита для пользователя {}", userId);

            ApiResponse<List<AuditLog>> response = ApiResponse.success(logs);
            String responseJson = objectMapper.writeValueAsString(response);

            sendSuccessResponse(ctx, responseJson);
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при получении записей аудита для пользователя {}", userId, e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
    }

    /**
     * Обрабатывает запрос на получение записей аудита по типу действия
     */
    private boolean handleGetAuditLogsByActionType(ChannelHandlerContext ctx, String actionType) {
        try {
            List<AuditLog> logs = auditService.getAuditLogsByActionType(actionType);
            logger.debug("Запрос записей аудита по типу действия {}", actionType);

            ApiResponse<List<AuditLog>> response = ApiResponse.success(logs);
            String responseJson = objectMapper.writeValueAsString(response);

            sendSuccessResponse(ctx, responseJson);
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при получении записей аудита по типу действия {}", actionType, e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
    }
} 