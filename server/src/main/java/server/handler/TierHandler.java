package server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import common.dto.ApiResponse;
import common.model.Tier;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.service.TierService;
import server.service.TierServiceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обработчик запросов к уровням лояльности
 */
public class TierHandler extends AbstractRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(TierHandler.class);
    private static final String URL_PATTERN = "/api/tiers.*";
    private static final Pattern ID_PATTERN = Pattern.compile("/api/tiers/(\\d+)");
    
    private final TierService tierService;
    
    public TierHandler() {
        super();
        this.tierService = new TierServiceImpl();
    }
    
    public TierHandler(TierService tierService) {
        super();
        this.tierService = tierService;
    }

    @Override
    public boolean handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String method = request.method().name();
        
        if (!uri.startsWith("/api/tiers")) {
            return false;
        }
        
        try {
            if (uri.matches("/api/tiers") && method.equals(HttpMethod.GET.name())) {
                // GET /api/tiers - получить все уровни
                handleGetAllTiers(ctx);
                return true;
            } else if (uri.matches("/api/tiers") && method.equals(HttpMethod.POST.name())) {
                // POST /api/tiers - создать новый уровень
                handleCreateTier(ctx, request);
                return true;
            } else if (uri.matches("/api/tiers/\\d+") && method.equals(HttpMethod.GET.name())) {
                // GET /api/tiers/{id} - получить уровень по ID
                handleGetTier(ctx, uri);
                return true;
            } else if (uri.matches("/api/tiers/\\d+") && method.equals(HttpMethod.PUT.name())) {
                // PUT /api/tiers/{id} - обновить уровень
                handleUpdateTier(ctx, request, uri);
                return true;
            } else if (uri.matches("/api/tiers/\\d+") && method.equals(HttpMethod.DELETE.name())) {
                // DELETE /api/tiers/{id} - удалить уровень
                handleDeleteTier(ctx, uri);
                return true;
            } else if (uri.matches("/api/tiers/name/[\\w-]+") && method.equals(HttpMethod.GET.name())) {
                // GET /api/tiers/name/{name} - получить уровень по названию
                handleGetTierByName(ctx, uri);
                return true;
            } else if (uri.matches("/api/tiers/points/\\d+") && method.equals(HttpMethod.GET.name())) {
                // GET /api/tiers/points/{points} - получить уровень для указанного количества баллов
                handleGetTierForPoints(ctx, uri);
                return true;
            }
        } catch (Exception e) {
            logger.error("Ошибка при обработке запроса к уровням лояльности", e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
        
        return false;
    }
    
    private void handleGetAllTiers(ChannelHandlerContext ctx) throws JsonProcessingException {
        List<Tier> tiers = tierService.getAllTiers();
        ApiResponse<List<Tier>> response = ApiResponse.success(tiers);
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private void handleCreateTier(ChannelHandlerContext ctx, FullHttpRequest request) throws JsonProcessingException {
        String content = getRequestContent(request);
        Map<String, Object> requestMap = objectMapper.readValue(content, HashMap.class);
        
        String name = (String) requestMap.get("name");
        int minPoints = ((Number) requestMap.get("minPoints")).intValue();
        int discountPercent = ((Number) requestMap.get("discountPercent")).intValue();
        
        try {
            Tier createdTier = tierService.createTier(name, minPoints, discountPercent);
            ApiResponse<Tier> response = ApiResponse.success(createdTier);
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } catch (IllegalArgumentException e) {
            ApiResponse<Object> response = ApiResponse.error(e.getMessage());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, jsonResponse);
        }
    }
    
    private void handleGetTier(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        Matcher matcher = ID_PATTERN.matcher(uri);
        if (matcher.matches()) {
            Long tierId = Long.parseLong(matcher.group(1));
            Optional<Tier> tierOpt = tierService.getTierById(tierId);
            
            if (tierOpt.isPresent()) {
                ApiResponse<Tier> response = ApiResponse.success(tierOpt.get());
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendSuccessResponse(ctx, jsonResponse);
            } else {
                ApiResponse<Object> response = ApiResponse.error("Уровень не найден");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
            }
        }
    }
    
    private void handleUpdateTier(ChannelHandlerContext ctx, FullHttpRequest request, String uri) throws JsonProcessingException {
        Matcher matcher = ID_PATTERN.matcher(uri);
        if (matcher.matches()) {
            Long tierId = Long.parseLong(matcher.group(1));
            
            String content = getRequestContent(request);
            Map<String, Object> requestMap = objectMapper.readValue(content, HashMap.class);
            
            // Проверяем, существует ли уровень
            Optional<Tier> existingTierOpt = tierService.getTierById(tierId);
            if (existingTierOpt.isEmpty()) {
                ApiResponse<Object> response = ApiResponse.error("Уровень не найден");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
                return;
            }
            
            // Обновляем данные уровня
            Tier tier = existingTierOpt.get();
            tier.setName((String) requestMap.get("name"));
            tier.setMinPoints(((Number) requestMap.get("minPoints")).intValue());
            
            // Convert discount percent to bonus multiplier
            double discountPercent = ((Number) requestMap.get("discountPercent")).doubleValue();
            double bonusMultiplier = 1.0 + (discountPercent / 100.0);
            tier.setBonusMultiplier(bonusMultiplier);
            
            boolean updated = tierService.updateTier(tier);
            
            if (updated) {
                ApiResponse<Tier> response = ApiResponse.success(tier);
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendSuccessResponse(ctx, jsonResponse);
            } else {
                ApiResponse<Object> response = ApiResponse.error("Не удалось обновить уровень");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, jsonResponse);
            }
        }
    }
    
    private void handleDeleteTier(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        Matcher matcher = ID_PATTERN.matcher(uri);
        if (matcher.matches()) {
            Long tierId = Long.parseLong(matcher.group(1));
            boolean deleted = tierService.deleteTier(tierId);
            
            if (deleted) {
                ApiResponse<Object> response = ApiResponse.success("Уровень успешно удален");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendSuccessResponse(ctx, jsonResponse);
            } else {
                ApiResponse<Object> response = ApiResponse.error("Не удалось удалить уровень");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
            }
        }
    }
    
    private void handleGetTierByName(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        String tierName = uri.substring("/api/tiers/name/".length());
        Optional<Tier> tierOpt = tierService.getTierByName(tierName);
        
        if (tierOpt.isPresent()) {
            ApiResponse<Tier> response = ApiResponse.success(tierOpt.get());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } else {
            ApiResponse<Object> response = ApiResponse.error("Уровень не найден");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
        }
    }
    
    private void handleGetTierForPoints(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        int points = Integer.parseInt(uri.substring("/api/tiers/points/".length()));
        Optional<Tier> tierOpt = tierService.getTierForPoints(points);
        
        if (tierOpt.isPresent()) {
            ApiResponse<Tier> response = ApiResponse.success(tierOpt.get());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } else {
            ApiResponse<Object> response = ApiResponse.error("Подходящий уровень не найден");
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
        return "GET|POST|PUT|DELETE";  // List supported methods explicitly
    }
} 