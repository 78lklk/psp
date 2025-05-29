package server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import common.dto.ApiResponse;
import common.model.Card;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.service.CardService;
import server.service.CardServiceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обработчик запросов к картам лояльности
 */
public class CardHandler extends AbstractRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(CardHandler.class);
    private static final String URL_PATTERN = "/api/cards(/\\d+)?";
    private static final Pattern ID_PATTERN = Pattern.compile("/api/cards/(\\d+)");
    private static final Pattern POINTS_PATTERN = Pattern.compile("/api/cards/(\\d+)/(add|deduct)");
    
    private final CardService cardService;
    
    public CardHandler() {
        super();
        this.cardService = new CardServiceImpl();
    }
    
    public CardHandler(CardService cardService) {
        super();
        this.cardService = cardService;
    }

    @Override
    protected String getUrlPattern() {
        return URL_PATTERN;
    }
    
    @Override
    protected String getMethod() {
        return "GET|POST|PUT|DELETE";  // List supported methods explicitly
    }

    @Override
    public boolean handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String method = request.method().name();
        
        if (!uri.startsWith("/api/cards")) {
            return false;
        }
        
        try {
            logger.debug("Processing request: {} {}", method, uri);
            
            if (uri.equals("/api/cards") && method.equals(HttpMethod.GET.name())) {
                // GET /api/cards - get all cards
                handleGetAllCards(ctx);
                return true;
            } else if (uri.equals("/api/cards") && method.equals(HttpMethod.POST.name())) {
                // POST /api/cards - create new card
                handleCreateCard(ctx, request);
                return true;
            } else if (uri.matches("/api/cards/\\d+") && method.equals(HttpMethod.GET.name())) {
                // GET /api/cards/{id} - get card by ID
                handleGetCard(ctx, uri);
                return true;
            } else if (uri.matches("/api/cards/\\d+") && method.equals(HttpMethod.DELETE.name())) {
                // DELETE /api/cards/{id} - delete card
                handleDeleteCard(ctx, uri);
                return true;
            } else if (uri.startsWith("/api/cards/number/") && method.equals(HttpMethod.GET.name())) {
                // GET /api/cards/number/{number} - get card by number
                handleGetCardByNumber(ctx, uri);
                return true;
            } else if (uri.startsWith("/api/cards/user/") && method.equals(HttpMethod.GET.name())) {
                // GET /api/cards/user/{userId} - get cards by user
                handleGetCardsByUser(ctx, uri);
                return true;
            } else if (uri.matches("/api/cards/\\d+/add") && method.equals(HttpMethod.POST.name())) {
                // POST /api/cards/{id}/add - add points to card
                handleAddPoints(ctx, request, uri);
                return true;
            } else if (uri.matches("/api/cards/\\d+/deduct") && method.equals(HttpMethod.POST.name())) {
                // POST /api/cards/{id}/deduct - deduct points from card
                handleDeductPoints(ctx, request, uri);
                return true;
            } else if (uri.matches("/api/cards/\\d+/tier") && method.equals(HttpMethod.POST.name())) {
                // POST /api/cards/{id}/tier - update card tier
                handleUpdateTier(ctx, uri);
                return true;
            }
            
            logger.debug("Request did not match any card handler patterns: {} {}", method, uri);
            return false;
        } catch (Exception e) {
            logger.error("Error processing card request", e);
            try {
                ApiResponse<Object> response = ApiResponse.error("Server error: " + e.getMessage());
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonResponse);
            } catch (JsonProcessingException ex) {
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
            }
            return true;
        }
    }
    
    private void handleGetAllCards(ChannelHandlerContext ctx) throws JsonProcessingException {
        try {
            logger.debug("Handling request to get all cards");
        List<Card> cards = cardService.getAllCards();
            logger.debug("Retrieved {} cards from database", cards.size());
            
        ApiResponse<List<Card>> response = ApiResponse.success(cards);
        String jsonResponse = objectMapper.writeValueAsString(response);
            logger.debug("Sending success response with {} cards", cards.size());
            
            // Set appropriate headers
        sendSuccessResponse(ctx, jsonResponse);
            logger.debug("Successfully sent {} cards to client", cards.size());
        } catch (Exception e) {
            logger.error("Error retrieving all cards", e);
            ApiResponse<Object> response = ApiResponse.error("Failed to retrieve cards: " + e.getMessage());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonResponse);
        }
    }
    
    private void handleCreateCard(ChannelHandlerContext ctx, FullHttpRequest request) throws JsonProcessingException {
        try {
        String content = getRequestContent(request);
        Map<String, Object> requestMap = objectMapper.readValue(content, HashMap.class);
        
        Long userId = ((Number) requestMap.get("userId")).longValue();
        String cardNumber = (String) requestMap.get("cardNumber");
            
            logger.debug("Received request to create card with number {} for user {}", cardNumber, userId);
        
        try {
            Card createdCard = cardService.createCard(userId, cardNumber);
                logger.debug("Card created successfully: {}", createdCard.getCardNumber());
                
            ApiResponse<Card> response = ApiResponse.success(createdCard);
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
                logger.debug("Successfully sent created card response to client");
        } catch (IllegalArgumentException e) {
                logger.error("Invalid request parameters for card creation: {}", e.getMessage());
                ApiResponse<Object> response = ApiResponse.error("Invalid request: " + e.getMessage());
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, jsonResponse);
            } catch (Exception e) {
                logger.error("Unexpected error creating card", e);
                ApiResponse<Object> response = ApiResponse.error("Failed to create card: " + e.getMessage());
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonResponse);
            }
        } catch (Exception e) {
            logger.error("Error parsing card creation request", e);
            ApiResponse<Object> response = ApiResponse.error("Invalid card creation request format");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, jsonResponse);
        }
    }
    
    private void handleGetCard(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        Matcher matcher = ID_PATTERN.matcher(uri);
        if (matcher.matches()) {
            Long cardId = Long.parseLong(matcher.group(1));
            Optional<Card> cardOpt = cardService.getCardById(cardId);
            
            if (cardOpt.isPresent()) {
                ApiResponse<Card> response = ApiResponse.success(cardOpt.get());
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendSuccessResponse(ctx, jsonResponse);
            } else {
                ApiResponse<Object> response = ApiResponse.error("Карта не найдена");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
            }
        }
    }
    
    private void handleDeleteCard(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        try {
        Matcher matcher = ID_PATTERN.matcher(uri);
        if (matcher.matches()) {
            Long cardId = Long.parseLong(matcher.group(1));
                logger.debug("Received request to delete card with ID: {}", cardId);
                
            boolean deleted = cardService.deleteCard(cardId);
            
            if (deleted) {
                    logger.debug("Card deleted successfully: {}", cardId);
                ApiResponse<Object> response = ApiResponse.success("Карта успешно удалена");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendSuccessResponse(ctx, jsonResponse);
            } else {
                    logger.error("Failed to delete card: {}", cardId);
                ApiResponse<Object> response = ApiResponse.error("Не удалось удалить карту");
                    String jsonResponse = objectMapper.writeValueAsString(response);
                    sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
                }
            } else {
                logger.error("Invalid card ID format in URL: {}", uri);
                ApiResponse<Object> response = ApiResponse.error("Некорректный формат ID карты");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, jsonResponse);
            }
        } catch (Exception e) {
            logger.error("Error during card deletion", e);
            ApiResponse<Object> response = ApiResponse.error("Ошибка при удалении карты: " + e.getMessage());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonResponse);
        }
    }
    
    private void handleGetCardByNumber(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        String cardNumber = uri.substring("/api/cards/number/".length());
        Optional<Card> cardOpt = cardService.getCardByNumber(cardNumber);
        
        if (cardOpt.isPresent()) {
            ApiResponse<Card> response = ApiResponse.success(cardOpt.get());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } else {
            ApiResponse<Object> response = ApiResponse.error("Карта не найдена");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
        }
    }
    
    private void handleGetCardsByUser(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        Long userId = Long.parseLong(uri.substring("/api/cards/user/".length()));
        List<Card> cards = cardService.getCardsByUserId(userId);
        
        ApiResponse<List<Card>> response = ApiResponse.success(cards);
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private void handleAddPoints(ChannelHandlerContext ctx, FullHttpRequest request, String uri) throws JsonProcessingException {
        Matcher matcher = POINTS_PATTERN.matcher(uri);
        if (matcher.matches()) {
            Long cardId = Long.parseLong(matcher.group(1));
            
            String content = getRequestContent(request);
            Map<String, Object> requestMap = objectMapper.readValue(content, HashMap.class);
            
            int points = ((Number) requestMap.get("points")).intValue();
            
            try {
                Optional<Card> cardOpt = cardService.addPoints(cardId, points);
                
                if (cardOpt.isPresent()) {
                    ApiResponse<Card> response = ApiResponse.success(cardOpt.get());
                    String jsonResponse = objectMapper.writeValueAsString(response);
                    sendSuccessResponse(ctx, jsonResponse);
                } else {
                    ApiResponse<Object> response = ApiResponse.error("Не удалось начислить баллы");
                    String jsonResponse = objectMapper.writeValueAsString(response);
                    sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
                }
            } catch (IllegalArgumentException e) {
                ApiResponse<Object> response = ApiResponse.error(e.getMessage());
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, jsonResponse);
            }
        }
    }
    
    private void handleDeductPoints(ChannelHandlerContext ctx, FullHttpRequest request, String uri) throws JsonProcessingException {
        Matcher matcher = POINTS_PATTERN.matcher(uri);
        if (matcher.matches()) {
            Long cardId = Long.parseLong(matcher.group(1));
            
            String content = getRequestContent(request);
            Map<String, Object> requestMap = objectMapper.readValue(content, HashMap.class);
            
            int points = ((Number) requestMap.get("points")).intValue();
            
            try {
                Optional<Card> cardOpt = cardService.deductPoints(cardId, points);
                
                if (cardOpt.isPresent()) {
                    ApiResponse<Card> response = ApiResponse.success(cardOpt.get());
                    String jsonResponse = objectMapper.writeValueAsString(response);
                    sendSuccessResponse(ctx, jsonResponse);
                } else {
                    ApiResponse<Object> response = ApiResponse.error("Не удалось списать баллы");
                    String jsonResponse = objectMapper.writeValueAsString(response);
                    sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
                }
            } catch (IllegalArgumentException e) {
                ApiResponse<Object> response = ApiResponse.error(e.getMessage());
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, jsonResponse);
            }
        }
    }
    
    private void handleUpdateTier(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        Long cardId = Long.parseLong(uri.substring("/api/cards/".length(), uri.indexOf("/tier")));
        
        Optional<Card> cardOpt = cardService.updateTierBasedOnPoints(cardId);
        
        if (cardOpt.isPresent()) {
            ApiResponse<Card> response = ApiResponse.success(cardOpt.get());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } else {
            ApiResponse<Object> response = ApiResponse.error("Не удалось обновить уровень карты");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
        }
    }
} 