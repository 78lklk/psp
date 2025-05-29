package server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.ApiResponse;
import common.model.PromoCode;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.service.PromoCodeService;
import server.service.PromoCodeServiceImpl;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обработчик запросов для промокодов
 */
public class PromoCodeHandler extends AbstractRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(PromoCodeHandler.class);
    private static final Pattern ID_PATTERN = Pattern.compile("/api/promo-codes/(\\d+)");
    private static final Pattern CODE_PATTERN = Pattern.compile("/api/promo-codes/code/([^/]+)");
    
    private final PromoCodeService promoCodeService;
    
    public PromoCodeHandler() {
        super();
        this.promoCodeService = new PromoCodeServiceImpl();
    }
    
    public PromoCodeHandler(PromoCodeService promoCodeService) {
        super();
        this.promoCodeService = promoCodeService;
    }
    
    @Override
    protected String getUrlPattern() {
        return "/api/promo-codes.*";
    }
    
    @Override
    protected String getMethod() {
        return "GET|POST|PUT|DELETE";
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return uri.startsWith("/api/promo-codes") && 
               method.matches(getMethod());
    }
    
    @Override
    public boolean handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String method = request.method().name();
        
        logger.debug("PromoCodeHandler processing: {} {}", method, uri);
        
        if (!canHandle(uri, method)) {
            return false;
        }
        
        try {
            // GET all promocodes
            if (uri.equals("/api/promo-codes") && method.equals(HttpMethod.GET.name())) {
                handleGetAllPromoCodes(ctx);
                return true;
            }
            
            // POST create new promocode
            if (uri.equals("/api/promo-codes") && method.equals(HttpMethod.POST.name())) {
                handleCreatePromoCode(ctx, request);
                return true;
            }
            
            // GET /api/promo-codes/{id} - get promocode by id
            Matcher idMatcher = ID_PATTERN.matcher(uri);
            if (idMatcher.matches() && method.equals(HttpMethod.GET.name())) {
                Long id = Long.parseLong(idMatcher.group(1));
                handleGetPromoCodeById(ctx, id);
                return true;
            }
            
            // PUT /api/promo-codes/{id} - update promocode
            if (idMatcher.matches() && method.equals(HttpMethod.PUT.name())) {
                Long id = Long.parseLong(idMatcher.group(1));
                handleUpdatePromoCode(ctx, id, request);
                return true;
            }
            
            // DELETE /api/promo-codes/{id} - delete promocode
            if (idMatcher.matches() && method.equals(HttpMethod.DELETE.name())) {
                Long id = Long.parseLong(idMatcher.group(1));
                handleDeletePromoCode(ctx, id);
                return true;
            }
            
            // GET /api/promo-codes/code/{code} - get promocode by code
            Matcher codeMatcher = CODE_PATTERN.matcher(uri);
            if (codeMatcher.matches() && method.equals(HttpMethod.GET.name())) {
                String code = codeMatcher.group(1);
                handleGetPromoCodeByCode(ctx, code);
                return true;
            }
            
            // GET /api/promo-codes/active - get active promocodes
            if (uri.equals("/api/promo-codes/active") && method.equals(HttpMethod.GET.name())) {
                handleGetActivePromoCodes(ctx);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error processing promocode request", e);
            try {
                ApiResponse<String> response = ApiResponse.error("Error processing request: " + e.getMessage());
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, response);
            } catch (Exception ex) {
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
            }
            return true;
        }
    }
    
    private void handleGetAllPromoCodes(ChannelHandlerContext ctx) throws JsonProcessingException {
        logger.debug("Getting all promocodes");
        
        List<PromoCode> promocodes = promoCodeService.getAllPromoCodes();
        ApiResponse<List<PromoCode>> response = ApiResponse.success(promocodes);
        
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private void handleGetActivePromoCodes(ChannelHandlerContext ctx) throws JsonProcessingException {
        logger.debug("Getting active promocodes");
        
        List<PromoCode> promocodes = promoCodeService.getActivePromoCodes();
        ApiResponse<List<PromoCode>> response = ApiResponse.success(promocodes);
        
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private void handleGetPromoCodeById(ChannelHandlerContext ctx, Long id) throws JsonProcessingException {
        logger.debug("Getting promocode by id: {}", id);
        
        Optional<PromoCode> promoCodeOpt = promoCodeService.getPromoCodeById(id);
        
        if (promoCodeOpt.isPresent()) {
            ApiResponse<PromoCode> response = ApiResponse.success(promoCodeOpt.get());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } else {
            ApiResponse<String> response = ApiResponse.error("Promocode not found");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
        }
    }
    
    private void handleGetPromoCodeByCode(ChannelHandlerContext ctx, String code) throws JsonProcessingException {
        logger.debug("Getting promocode by code: {}", code);
        
        Optional<PromoCode> promoCodeOpt = promoCodeService.getPromoCodeByCode(code);
        
        if (promoCodeOpt.isPresent()) {
            ApiResponse<PromoCode> response = ApiResponse.success(promoCodeOpt.get());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } else {
            ApiResponse<String> response = ApiResponse.error("Promocode not found");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
        }
    }
    
    private void handleCreatePromoCode(ChannelHandlerContext ctx, FullHttpRequest request) throws JsonProcessingException {
        logger.debug("Creating new promocode");
        
        String content = request.content().toString(CharsetUtil.UTF_8);
        logger.debug("Received promo code create request body: {}", content);
        
        PromoCode promoCode = objectMapper.readValue(content, PromoCode.class);
        
        // Validate required fields
        if (promoCode.getCode() == null || promoCode.getCode().isEmpty()) {
            ApiResponse<String> response = ApiResponse.error("Code is required");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, jsonResponse);
            return;
        }
        
        // Устанавливаем значения по умолчанию для отсутствующих полей
        if (promoCode.getPromotionId() == null) {
            // Если не указана акция, связываем с первой акцией из базы
            promoCode.setPromotionId(1L);
        }
        
        if (promoCode.getCreatedBy() == null) {
            promoCode.setCreatedBy("system");
        }
        
        if (promoCode.getUsesCount() == null) {
            promoCode.setUsesCount(0);
        }
        
        // Create promocode
        PromoCode createdPromoCode = promoCodeService.createPromoCode(promoCode);
        
        if (createdPromoCode != null) {
            ApiResponse<PromoCode> response = ApiResponse.success(createdPromoCode);
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } else {
            ApiResponse<String> response = ApiResponse.error("Failed to create promocode");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonResponse);
        }
    }
    
    private void handleUpdatePromoCode(ChannelHandlerContext ctx, Long id, FullHttpRequest request) throws JsonProcessingException {
        logger.debug("Updating promocode with id: {}", id);
        
        // Проверяем, существует ли промокод
        Optional<PromoCode> existingPromoCode = promoCodeService.getPromoCodeById(id);
        if (existingPromoCode.isEmpty()) {
            ApiResponse<String> response = ApiResponse.error("Промокод не найден на сервере. Возможно, он был удален.");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
            return;
        }
        
        String content = request.content().toString(CharsetUtil.UTF_8);
        PromoCode promoCode = objectMapper.readValue(content, PromoCode.class);
        
        // Validate required fields
        if (promoCode.getCode() == null || promoCode.getCode().isEmpty()) {
            ApiResponse<String> response = ApiResponse.error("Code is required");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, jsonResponse);
            return;
        }
        
        // Update promocode
        Optional<PromoCode> updatedPromoCodeOpt = promoCodeService.updatePromoCode(id, promoCode);
        
        if (updatedPromoCodeOpt.isPresent()) {
            ApiResponse<PromoCode> response = ApiResponse.success(updatedPromoCodeOpt.get());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } else {
            ApiResponse<String> response = ApiResponse.error("Промокод не найден или не может быть обновлен");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
        }
    }
    
    private void handleDeletePromoCode(ChannelHandlerContext ctx, Long id) throws JsonProcessingException {
        logger.debug("Deleting promocode with id: {}", id);
        
        // Check if promocode exists
        Optional<PromoCode> promoCodeOpt = promoCodeService.getPromoCodeById(id);
        
        if (promoCodeOpt.isEmpty()) {
            ApiResponse<String> response = ApiResponse.error("Promocode not found");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
            return;
        }
        
        // Delete promocode - assuming there's a delete method in the service
        boolean deleted = promoCodeService.deletePromoCode(id);
        
        if (deleted) {
            ApiResponse<String> response = ApiResponse.success("Promocode deleted successfully");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } else {
            ApiResponse<String> response = ApiResponse.error("Failed to delete promocode");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonResponse);
        }
    }
} 