package server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import common.dto.ApiResponse;
import common.model.Session;
import common.model.User;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.service.SessionService;
import server.service.SessionServiceImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обработчик запросов к игровым сессиям
 */
public class SessionHandler extends AbstractRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(SessionHandler.class);
    private static final String URL_PATTERN = "/api/sessions.*";
    private static final Pattern ID_PATTERN = Pattern.compile("/api/sessions/(\\d+)");
    private static final Pattern FINISH_PATTERN = Pattern.compile("/api/sessions/(\\d+)/finish");
    
    private final SessionService sessionService;
    
    public SessionHandler() {
        super();
        this.sessionService = new SessionServiceImpl();
    }
    
    public SessionHandler(SessionService sessionService) {
        super();
        this.sessionService = sessionService;
    }

    @Override
    public boolean handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String method = request.method().name();
        
        if (!uri.startsWith("/api/sessions")) {
            return false;
        }
        
        try {
            if (uri.matches("/api/sessions") && method.equals(HttpMethod.GET.name())) {
                // GET /api/sessions - получить все сессии
                handleGetAllSessions(ctx);
                return true;
            } else if (uri.matches("/api/sessions") && method.equals(HttpMethod.POST.name())) {
                // POST /api/sessions - создать новую сессию
                handleCreateSession(ctx, request);
                return true;
            } else if (uri.matches("/api/sessions/\\d+") && method.equals(HttpMethod.GET.name())) {
                // GET /api/sessions/{id} - получить сессию по ID
                handleGetSession(ctx, uri);
                return true;
            } else if (uri.matches("/api/sessions/\\d+") && method.equals(HttpMethod.DELETE.name())) {
                // DELETE /api/sessions/{id} - удалить сессию
                handleDeleteSession(ctx, uri);
                return true;
            } else if (uri.matches("/api/sessions/\\d+/finish") && method.equals(HttpMethod.POST.name())) {
                // POST /api/sessions/{id}/finish - завершить сессию
                handleFinishSession(ctx, request, uri);
                return true;
            } else if (uri.matches("/api/sessions/card/\\d+") && method.equals(HttpMethod.GET.name())) {
                // GET /api/sessions/card/{cardId} - получить активные сессии карты
                handleGetSessionsByCard(ctx, uri);
                return true;
            } else if (uri.matches("/api/sessions/card/\\d+/period") && method.equals(HttpMethod.GET.name())) {
                // GET /api/sessions/card/{cardId}/period?from=...&to=... - получить сессии карты за период
                handleGetSessionsByCardAndPeriod(ctx, request);
                return true;
            }
        } catch (Exception e) {
            logger.error("Ошибка при обработке запроса к сессиям", e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
        
        return false;
    }
    
    private void handleGetAllSessions(ChannelHandlerContext ctx) throws JsonProcessingException {
        List<Session> sessions = sessionService.getAllSessions();
        ApiResponse<List<Session>> response = ApiResponse.success(sessions);
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private void handleCreateSession(ChannelHandlerContext ctx, FullHttpRequest request) throws JsonProcessingException {
        String content = request.content().toString(CharsetUtil.UTF_8);
        Map<String, Object> requestMap = objectMapper.readValue(content, HashMap.class);
        
        Long cardId = ((Number) requestMap.get("cardId")).longValue();
        int minutes = ((Number) requestMap.get("minutes")).intValue();
        String computerInfo = requestMap.get("computerInfo") != null ? requestMap.get("computerInfo").toString() : null;
        
        // Информация о пользователе для аудита
        User staffUser = createMockUserForSession(request);
        String ipAddress = getClientIpAddress(ctx);
        
        try {
            // Создаем сессию с учетом пользователя для аудита
            Session createdSession = sessionService.createSession(cardId, minutes, computerInfo, staffUser, ipAddress);
            ApiResponse<Session> response = ApiResponse.success(createdSession);
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } catch (IllegalArgumentException e) {
            ApiResponse<Object> response = ApiResponse.error(e.getMessage());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, jsonResponse);
        }
    }
    
    private void handleGetSession(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        Matcher matcher = ID_PATTERN.matcher(uri);
        if (matcher.matches()) {
            Long sessionId = Long.parseLong(matcher.group(1));
            Optional<Session> sessionOpt = sessionService.getSessionById(sessionId);
            
            if (sessionOpt.isPresent()) {
                ApiResponse<Session> response = ApiResponse.success(sessionOpt.get());
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendSuccessResponse(ctx, jsonResponse);
            } else {
                ApiResponse<Object> response = ApiResponse.error("Сессия не найдена");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
            }
        }
    }
    
    private void handleDeleteSession(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        Matcher matcher = ID_PATTERN.matcher(uri);
        if (matcher.matches()) {
            Long sessionId = Long.parseLong(matcher.group(1));
            boolean deleted = sessionService.deleteSession(sessionId);
            
            if (deleted) {
                ApiResponse<Object> response = ApiResponse.success("Сессия успешно удалена");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendSuccessResponse(ctx, jsonResponse);
            } else {
                ApiResponse<Object> response = ApiResponse.error("Не удалось удалить сессию");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
            }
        }
    }
    
    private void handleFinishSession(ChannelHandlerContext ctx, FullHttpRequest request, String uri) throws JsonProcessingException {
        Matcher matcher = FINISH_PATTERN.matcher(uri);
        if (matcher.matches()) {
            Long sessionId = Long.parseLong(matcher.group(1));
            
            // Информация о пользователе для аудита
            User staffUser = createMockUserForSession(request);
            String ipAddress = getClientIpAddress(ctx);
            
            // Завершаем сессию с учетом пользователя для аудита
            Optional<Session> sessionOpt = sessionService.finishSession(sessionId, staffUser, ipAddress);
            
            if (sessionOpt.isPresent()) {
                ApiResponse<Session> response = ApiResponse.success(sessionOpt.get());
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendSuccessResponse(ctx, jsonResponse);
            } else {
                ApiResponse<Object> response = ApiResponse.error("Не удалось завершить сессию");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
            }
        }
    }
    
    private void handleGetSessionsByCard(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        Long cardId = Long.parseLong(uri.substring("/api/sessions/card/".length()));
        List<Session> sessions = sessionService.getActiveSessionsByCardId(cardId);
        
        ApiResponse<List<Session>> response = ApiResponse.success(sessions);
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private void handleGetSessionsByCardAndPeriod(ChannelHandlerContext ctx, FullHttpRequest request) throws JsonProcessingException {
        String uri = request.uri();
        Long cardId = Long.parseLong(uri.substring("/api/sessions/card/".length(), uri.indexOf("/period")));
        
        // Получаем параметры from и to из строки запроса
        String queryString = uri.substring(uri.indexOf("?") + 1);
        Map<String, String> queryParams = parseQueryString(queryString);
        
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime from = LocalDateTime.parse(queryParams.get("from"), formatter);
        LocalDateTime to = LocalDateTime.parse(queryParams.get("to"), formatter);
        
        List<Session> sessions = sessionService.getSessionsByCardIdAndPeriod(cardId, from, to);
        
        ApiResponse<List<Session>> response = ApiResponse.success(sessions);
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private Map<String, String> parseQueryString(String queryString) {
        Map<String, String> result = new HashMap<>();
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                result.put(keyValue[0], keyValue[1]);
            }
        }
        return result;
    }
    
    /**
     * Создает временного пользователя для аудита
     * @param request HTTP запрос
     * @return временный пользователь
     */
    private User createMockUserForSession(FullHttpRequest request) {
        User user = new User();
        user.setId(1L); // Test ID
        user.setUsername("staff_admin"); // Test staff username
        user.setFirstName("Staff");
        user.setLastName("User");
        user.setEmail("staff@example.com");
        
        // Создаем роль STAFF
        common.model.Role role = new common.model.Role();
        role.setId(3L);
        role.setName("STAFF");
        user.setRole(role);
        
        return user;
    }
    
    /**
     * Получает IP-адрес клиента из запроса
     * @param ctx контекст канала
     * @return IP-адрес клиента
     */
    private String getClientIpAddress(ChannelHandlerContext ctx) {
        String ipAddress = ctx.channel().remoteAddress().toString();
        // Убираем слеш и порт, если они есть
        if (ipAddress.startsWith("/")) {
            ipAddress = ipAddress.substring(1);
        }
        if (ipAddress.contains(":")) {
            ipAddress = ipAddress.substring(0, ipAddress.indexOf(":"));
        }
        return ipAddress;
    }

    @Override
    protected String getUrlPattern() {
        return URL_PATTERN;
    }

    @Override
    protected String getMethod() {
        return "GET|POST|PUT|DELETE";  // List supported methods explicitly
    }
} 