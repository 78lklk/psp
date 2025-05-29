package server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import common.dto.ApiResponse;
import common.model.Setting;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.service.SettingsService;
import server.service.SettingsServiceImpl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обработчик запросов к настройкам системы
 */
public class SettingsHandler extends AbstractRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(SettingsHandler.class);
    private static final String URL_PATTERN = "/api/settings.*";
    private static final Pattern KEY_PATTERN = Pattern.compile("/api/settings/([^/]+)");
    
    private final SettingsService settingsService;
    
    public SettingsHandler() {
        super();
        this.settingsService = new SettingsServiceImpl();
        logger.info("SettingsHandler initialized");
    }
    
    public SettingsHandler(SettingsService settingsService) {
        super();
        this.settingsService = settingsService;
        logger.info("SettingsHandler initialized with custom SettingsService");
    }

    @Override
    public boolean handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String method = request.method().name();
        
        if (uri.startsWith("/api/settings")) {
            logger.debug("Обработка запроса к настройкам: {} {}", method, uri);
            
            try {
                if (uri.equals("/api/settings") && method.equals(HttpMethod.GET.name())) {
                    handleGetAllSettings(ctx);
                    return true;
                } else if (uri.matches("/api/settings/\\w+") && method.equals(HttpMethod.GET.name())) {
                    String key = uri.replaceFirst("/api/settings/", "");
                    handleGetSetting(ctx, key);
                    return true;
                } else if (uri.equals("/api/settings") && method.equals(HttpMethod.POST.name())) {
                    handleSaveSetting(ctx, request);
                    return true;
                } else if (uri.matches("/api/settings/\\w+") && method.equals(HttpMethod.DELETE.name())) {
                    String key = uri.replaceFirst("/api/settings/", "");
                    handleDeleteSetting(ctx, key);
                    return true;
                }
            } catch (JsonProcessingException e) {
                logger.error("Ошибка при обработке запроса настроек", e);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Ошибка при обработке запроса: " + e.getMessage());
                return true;
            } catch (Exception e) {
                logger.error("Непредвиденная ошибка при обработке запроса настроек", e);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Обрабатывает запрос на получение всех настроек
     */
    private void handleGetAllSettings(ChannelHandlerContext ctx) {
        try {
            Map<String, Setting> settings = settingsService.getAllSettings();
            ApiResponse<Map<String, Setting>> response = ApiResponse.success(settings);
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendJsonResponse(ctx, jsonResponse);
            
            logger.debug("Отправлены все настройки. Количество: {}", settings.size());
        } catch (JsonProcessingException e) {
            logger.error("Ошибка при сериализации настроек", e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Ошибка при получении настроек");
        }
    }
    
    /**
     * Обрабатывает запрос на получение настройки по ключу
     */
    private void handleGetSetting(ChannelHandlerContext ctx, String key) {
        try {
            Optional<Setting> setting = settingsService.getSetting(key);
            
            if (setting.isPresent()) {
                ApiResponse<Setting> response = ApiResponse.success(setting.get());
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendJsonResponse(ctx, jsonResponse);
                
                logger.debug("Отправлена настройка: {}", key);
            } else {
                ApiResponse<String> response = ApiResponse.error("Настройка не найдена: " + key);
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendJsonResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
                
                logger.debug("Настройка не найдена: {}", key);
            }
        } catch (JsonProcessingException e) {
            logger.error("Ошибка при сериализации настройки", e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Ошибка при получении настройки");
        }
    }
    
    private void handleSaveSetting(ChannelHandlerContext ctx, FullHttpRequest request) throws JsonProcessingException {
        String content = getRequestContent(request);
        logger.debug("Получено тело запроса для сохранения настройки: {}", content);
        
        try {
            // Используем специальную настройку чтобы игнорировать неизвестные поля в JSON
            Setting setting = objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(content, Setting.class);
            
            logger.debug("Десериализованная настройка: ключ={}, значение={}, описание={}",
                setting.getKey(), setting.getValue(), setting.getDescription());
            
            if (setting.getKey() == null || setting.getKey().isEmpty()) {
                ApiResponse<Object> response = ApiResponse.error("Ключ настройки не может быть пустым");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, jsonResponse);
                return;
            }
            
            if (setting.getValue() == null) {
                setting.setValue(""); // Устанавливаем пустую строку вместо null
            }
            
            // Установка времени последнего обновления
            if (setting.getLastUpdated() == null) {
                setting.setLastUpdated(LocalDateTime.now());
            }
            
            boolean created = settingsService.saveSetting(setting);
            
            if (created) {
                logger.info("Настройка успешно сохранена: {}", setting.getKey());
                ApiResponse<Setting> response = ApiResponse.success(setting);
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendSuccessResponse(ctx, jsonResponse);
            } else {
                logger.error("Не удалось сохранить настройку: {}", setting.getKey());
                ApiResponse<Object> response = ApiResponse.error("Не удалось создать или обновить настройку");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonResponse);
            }
        } catch (Exception e) {
            logger.error("Ошибка при обработке запроса настройки: {}", e.getMessage(), e);
            ApiResponse<Object> response = ApiResponse.error("Некорректный формат данных настройки: " + e.getMessage());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, jsonResponse);
        }
    }
    
    private void handleDeleteSetting(ChannelHandlerContext ctx, String key) throws JsonProcessingException {
        boolean deleted = settingsService.deleteSetting(key);
        
        if (deleted) {
            ApiResponse<Object> response = ApiResponse.success("Настройка успешно удалена");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } else {
            ApiResponse<Object> response = ApiResponse.error("Не удалось удалить настройку");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
        }
    }

    @Override
    protected String getUrlPattern() {
        return URL_PATTERN;
    }
    
    @Override
    protected String getMethod() {
        return "GET|POST|PUT|DELETE";
    }

    /**
     * Отправляет JSON-ответ клиенту
     * @param ctx контекст канала
     * @param jsonResponse JSON-строка с ответом
     */
    private void sendJsonResponse(ChannelHandlerContext ctx, String jsonResponse) {
        sendJsonResponse(ctx, HttpResponseStatus.OK, jsonResponse);
    }
    
    /**
     * Отправляет JSON-ответ клиенту с указанным статусом
     * @param ctx контекст канала
     * @param status статус HTTP-ответа
     * @param jsonResponse JSON-строка с ответом
     */
    private void sendJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String jsonResponse) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(jsonResponse, StandardCharsets.UTF_8));
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        // Установка CORS заголовков для веб-клиентов
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization");
        
        ctx.writeAndFlush(response);
    }
} 