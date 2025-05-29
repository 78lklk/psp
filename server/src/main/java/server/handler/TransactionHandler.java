package server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import common.dto.ApiResponse;
import common.model.Transaction;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.service.TransactionService;
import server.service.TransactionServiceImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обработчик запросов к транзакциям
 */
public class TransactionHandler extends AbstractRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(TransactionHandler.class);
    private static final String URL_PATTERN = "/api/transactions(/\\d+)?";
    private static final Pattern ID_PATTERN = Pattern.compile("/api/transactions/(\\d+)");
    
    private final TransactionService transactionService;
    
    public TransactionHandler() {
        super();
        this.transactionService = new TransactionServiceImpl();
    }
    
    public TransactionHandler(TransactionService transactionService) {
        super();
        this.transactionService = transactionService;
    }

    @Override
    public boolean handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String method = request.method().name();
        
        if (!uri.startsWith("/api/transactions")) {
            return false;
        }
        
        try {
            if (uri.matches("/api/transactions") && method.equals(HttpMethod.GET.name())) {
                // GET /api/transactions - получить все транзакции
                handleGetAllTransactions(ctx);
                return true;
            } else if (uri.matches("/api/transactions") && method.equals(HttpMethod.POST.name())) {
                // POST /api/transactions - создать новую транзакцию
                handleCreateTransaction(ctx, request);
                return true;
            } else if (uri.matches("/api/transactions/\\d+") && method.equals(HttpMethod.GET.name())) {
                // GET /api/transactions/{id} - получить транзакцию по ID
                handleGetTransaction(ctx, uri);
                return true;
            } else if (uri.matches("/api/transactions/\\d+") && method.equals(HttpMethod.DELETE.name())) {
                // DELETE /api/transactions/{id} - удалить транзакцию
                handleDeleteTransaction(ctx, uri);
                return true;
            } else if (uri.matches("/api/transactions/card/\\d+") && method.equals(HttpMethod.GET.name())) {
                // GET /api/transactions/card/{cardId} - получить транзакции карты
                handleGetTransactionsByCard(ctx, uri);
                return true;
            } else if (uri.matches("/api/transactions/card/\\d+/period") && method.equals(HttpMethod.GET.name())) {
                // GET /api/transactions/card/{cardId}/period?from=...&to=... - получить транзакции карты за период
                handleGetTransactionsByCardAndPeriod(ctx, request);
                return true;
            } else if (uri.matches("/api/transactions/type/\\w+") && method.equals(HttpMethod.GET.name())) {
                // GET /api/transactions/type/{type} - получить транзакции по типу
                handleGetTransactionsByType(ctx, uri);
                return true;
            }
        } catch (Exception e) {
            logger.error("Ошибка при обработке запроса к транзакциям", e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
        
        return false;
    }
    
    private void handleGetAllTransactions(ChannelHandlerContext ctx) throws JsonProcessingException {
        List<Transaction> transactions = transactionService.getAllTransactions();
        ApiResponse<List<Transaction>> response = ApiResponse.success(transactions);
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private void handleCreateTransaction(ChannelHandlerContext ctx, FullHttpRequest request) throws JsonProcessingException {
        String content = getRequestContent(request);
        Map<String, Object> requestMap = objectMapper.readValue(content, HashMap.class);
        
        Long cardId = ((Number) requestMap.get("cardId")).longValue();
        Transaction.Type type = Transaction.Type.valueOf((String) requestMap.get("type"));
        int points = ((Number) requestMap.get("points")).intValue();
        String description = (String) requestMap.get("description");
        
        try {
            Transaction createdTransaction = transactionService.createTransaction(cardId, type, points, description);
            ApiResponse<Transaction> response = ApiResponse.success(createdTransaction);
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } catch (IllegalArgumentException e) {
            ApiResponse<Object> response = ApiResponse.error(e.getMessage());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, jsonResponse);
        }
    }
    
    private void handleGetTransaction(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        Matcher matcher = ID_PATTERN.matcher(uri);
        if (matcher.matches()) {
            Long transactionId = Long.parseLong(matcher.group(1));
            Optional<Transaction> transactionOpt = transactionService.getTransactionById(transactionId);
            
            if (transactionOpt.isPresent()) {
                ApiResponse<Transaction> response = ApiResponse.success(transactionOpt.get());
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendSuccessResponse(ctx, jsonResponse);
            } else {
                ApiResponse<Object> response = ApiResponse.error("Транзакция не найдена");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
            }
        }
    }
    
    private void handleDeleteTransaction(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        Matcher matcher = ID_PATTERN.matcher(uri);
        if (matcher.matches()) {
            Long transactionId = Long.parseLong(matcher.group(1));
            boolean deleted = transactionService.deleteTransaction(transactionId);
            
            if (deleted) {
                ApiResponse<Object> response = ApiResponse.success("Транзакция успешно удалена");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendSuccessResponse(ctx, jsonResponse);
            } else {
                ApiResponse<Object> response = ApiResponse.error("Не удалось удалить транзакцию");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
            }
        }
    }
    
    private void handleGetTransactionsByCard(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        Long cardId = Long.parseLong(uri.substring("/api/transactions/card/".length()));
        List<Transaction> transactions = transactionService.getTransactionsByCardId(cardId);
        
        ApiResponse<List<Transaction>> response = ApiResponse.success(transactions);
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private void handleGetTransactionsByCardAndPeriod(ChannelHandlerContext ctx, FullHttpRequest request) throws JsonProcessingException {
        String uri = request.uri();
        Long cardId = Long.parseLong(uri.substring("/api/transactions/card/".length(), uri.indexOf("/period")));
        
        // Получаем параметры from и to из строки запроса
        String queryString = uri.substring(uri.indexOf("?") + 1);
        Map<String, String> queryParams = parseQueryString(queryString);
        
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime from = LocalDateTime.parse(queryParams.get("from"), formatter);
        LocalDateTime to = LocalDateTime.parse(queryParams.get("to"), formatter);
        
        List<Transaction> transactions = transactionService.getTransactionsByCardIdAndPeriod(cardId, from, to);
        
        ApiResponse<List<Transaction>> response = ApiResponse.success(transactions);
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private void handleGetTransactionsByType(ChannelHandlerContext ctx, String uri) throws JsonProcessingException {
        String typeStr = uri.substring("/api/transactions/type/".length());
        Transaction.Type type = Transaction.Type.valueOf(typeStr);
        
        List<Transaction> transactions = transactionService.getTransactionsByType(type);
        
        ApiResponse<List<Transaction>> response = ApiResponse.success(transactions);
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

    @Override
    protected String getUrlPattern() {
        return URL_PATTERN;
    }

    @Override
    protected String getMethod() {
        return "GET|POST|PUT|DELETE";  // List supported methods explicitly
    }
} 