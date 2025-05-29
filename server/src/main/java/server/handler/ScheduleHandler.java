package server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import common.dto.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Простой обработчик запросов для работы с расписанием компьютеров
 */
public class ScheduleHandler implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleHandler.class);
    private final ObjectMapper objectMapper;
    
    public ScheduleHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Override
    public boolean handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        
        if (uri.startsWith("/api/schedule")) {
            HttpMethod method = request.method();
            
            try {
                // GET /api/schedule - получение расписания
                if (method == HttpMethod.GET && uri.equals("/api/schedule")) {
                    handleGetSchedule(ctx);
                    return true;
                }
                
                // POST /api/schedule - создание записи в расписании
                if (method == HttpMethod.POST && uri.equals("/api/schedule")) {
                    handleCreateScheduleEntry(ctx);
                    return true;
                }
                
                // DELETE /api/schedule/{id} - удаление записи в расписании
                if (method == HttpMethod.DELETE && uri.matches("/api/schedule/\\d+")) {
                    Long id = Long.parseLong(uri.substring("/api/schedule/".length()));
                    handleDeleteScheduleEntry(ctx, id);
                    return true;
                }
            } catch (Exception e) {
                logger.error("Ошибка при обработке запроса к расписанию", e);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, 
                        "Внутренняя ошибка сервера: " + e.getMessage());
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean canHandle(String path, String method) {
        return path.startsWith("/api/schedule");
    }
    
    /**
     * Обрабатывает запрос на получение расписания
     */
    private void handleGetSchedule(ChannelHandlerContext ctx) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Функциональность расписания доступна только в локальном режиме");
        response.put("status", "success");
        
        String jsonResponse = objectMapper.writeValueAsString(
                ApiResponse.success(response));
        sendJsonResponse(ctx, jsonResponse);
    }
    
    /**
     * Обрабатывает запрос на создание записи в расписании
     */
    private void handleCreateScheduleEntry(ChannelHandlerContext ctx) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Запись в расписании успешно создана");
        response.put("status", "success");
        response.put("id", 1L);
        
        String jsonResponse = objectMapper.writeValueAsString(
                ApiResponse.success(response));
        sendJsonResponse(ctx, jsonResponse);
    }
    
    /**
     * Обрабатывает запрос на удаление записи в расписании
     */
    private void handleDeleteScheduleEntry(ChannelHandlerContext ctx, Long id) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Запись в расписании с ID " + id + " успешно удалена");
        response.put("status", "success");
        
        String jsonResponse = objectMapper.writeValueAsString(
                ApiResponse.success(response));
        sendJsonResponse(ctx, jsonResponse);
    }
    
    /**
     * Отправляет успешный HTTP-ответ (private helper method)
     */
    private void sendJsonResponse(ChannelHandlerContext ctx, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(content, StandardCharsets.UTF_8)
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        ctx.writeAndFlush(response);
    }
    
    @Override
    public void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        try {
            Map<String, String> errorData = new HashMap<>();
            errorData.put("error", message);
            
            String jsonResponse = objectMapper.writeValueAsString(errorData);
            
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    status,
                    Unpooled.copiedBuffer(jsonResponse, StandardCharsets.UTF_8)
            );
            
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            
            ctx.writeAndFlush(response);
        } catch (Exception e) {
            logger.error("Ошибка при отправке ответа с ошибкой", e);
            ctx.close();
        }
    }
    
    @Override
    public void sendSuccessResponse(ChannelHandlerContext ctx, String content) {
        try {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(content, StandardCharsets.UTF_8)
            );
            
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            
            ctx.writeAndFlush(response);
        } catch (Exception e) {
            logger.error("Ошибка при отправке успешного ответа", e);
            ctx.close();
        }
    }
} 