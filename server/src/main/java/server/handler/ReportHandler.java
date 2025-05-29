package server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.ApiResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.service.ReportService;
import server.service.ReportServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обработчик для отчетов
 */
public class ReportHandler extends AbstractRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReportHandler.class);
    private final ReportService reportService;
    
    public ReportHandler() {
        super();
        this.reportService = new ReportServiceImpl();
    }
    
    @Override
    protected String getUrlPattern() {
        return "/api(/api)?/reports.*";
    }
    
    @Override
    protected String getMethod() {
        return "GET";
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return (uri.startsWith("/api/reports") || uri.startsWith("/api/api/reports")) && 
               method.matches(getMethod());
    }
    
    @Override
    public boolean handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String method = request.method().name();
        
        logger.debug("ReportHandler processing: {} {}", method, uri);
        
        if (!canHandle(uri, method)) {
            return false;
        }
        
        try {
            // GET /api/reports/points or /api/api/reports/points - получить отчет о начисленных баллах
            if ((uri.startsWith("/api/reports/points") || uri.startsWith("/api/api/reports/points")) && method.equals(HttpMethod.GET.name())) {
                // Parse date parameters
                Map<String, String> queryParams = parseQueryParams(uri);
                
                LocalDate fromDate = LocalDate.now().minusDays(30);
                LocalDate toDate = LocalDate.now();
                
                try {
                    if (queryParams.containsKey("from")) {
                        fromDate = LocalDate.parse(queryParams.get("from"));
                    }
                    if (queryParams.containsKey("to")) {
                        toDate = LocalDate.parse(queryParams.get("to"));
                    }
                } catch (Exception e) {
                    logger.error("Error parsing date parameters", e);
                }
                
                handlePointsReport(ctx, fromDate, toDate);
                return true;
            }
            
            // GET /api/reports/activity - получить отчет по активности пользователей
            if ((uri.startsWith("/api/reports/activity") || uri.startsWith("/api/api/reports/activity")) && method.equals(HttpMethod.GET.name())) {
                Map<String, String> queryParams = parseQueryParams(uri);
                
                LocalDate fromDate = LocalDate.now().minusDays(30);
                LocalDate toDate = LocalDate.now();
                
                try {
                    if (queryParams.containsKey("from")) {
                        fromDate = LocalDate.parse(queryParams.get("from"));
                    }
                    if (queryParams.containsKey("to")) {
                        toDate = LocalDate.parse(queryParams.get("to"));
                    }
                } catch (Exception e) {
                    logger.error("Error parsing date parameters", e);
                }
                
                handleUserActivityReport(ctx, fromDate, toDate);
                return true;
            }
            
            // GET /api/reports/promotions - получить отчет по акциям
            if ((uri.startsWith("/api/reports/promotions") || uri.startsWith("/api/api/reports/promotions")) && method.equals(HttpMethod.GET.name())) {
                Map<String, String> queryParams = parseQueryParams(uri);
                
                LocalDate fromDate = LocalDate.now().minusDays(30);
                LocalDate toDate = LocalDate.now();
                
                try {
                    if (queryParams.containsKey("from")) {
                        fromDate = LocalDate.parse(queryParams.get("from"));
                    }
                    if (queryParams.containsKey("to")) {
                        toDate = LocalDate.parse(queryParams.get("to"));
                    }
                } catch (Exception e) {
                    logger.error("Error parsing date parameters", e);
                }
                
                handlePromotionsReport(ctx, fromDate, toDate);
                return true;
            }
            
            // GET /api/reports/promocodes - получить отчет по промокодам
            if ((uri.startsWith("/api/reports/promocodes") || uri.startsWith("/api/api/reports/promocodes")) && method.equals(HttpMethod.GET.name())) {
                Map<String, String> queryParams = parseQueryParams(uri);
                
                LocalDate fromDate = LocalDate.now().minusDays(30);
                LocalDate toDate = LocalDate.now();
                
                try {
                    if (queryParams.containsKey("from")) {
                        fromDate = LocalDate.parse(queryParams.get("from"));
                    }
                    if (queryParams.containsKey("to")) {
                        toDate = LocalDate.parse(queryParams.get("to"));
                    }
                } catch (Exception e) {
                    logger.error("Error parsing date parameters", e);
                }
                
                handlePromoCodesReport(ctx, fromDate, toDate);
                return true;
            }
            
            // GET /api/reports/financial - получить финансовый отчет
            if ((uri.startsWith("/api/reports/financial") || uri.startsWith("/api/api/reports/financial")) && method.equals(HttpMethod.GET.name())) {
                Map<String, String> queryParams = parseQueryParams(uri);
                
                LocalDate fromDate = LocalDate.now().minusDays(30);
                LocalDate toDate = LocalDate.now();
                
                try {
                    if (queryParams.containsKey("from")) {
                        fromDate = LocalDate.parse(queryParams.get("from"));
                    }
                    if (queryParams.containsKey("to")) {
                        toDate = LocalDate.parse(queryParams.get("to"));
                    }
                } catch (Exception e) {
                    logger.error("Error parsing date parameters", e);
                }
                
                handleFinancialReport(ctx, fromDate, toDate);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error processing report request", e);
            try {
                ApiResponse<String> response = ApiResponse.error("Error processing request: " + e.getMessage());
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, response);
            } catch (Exception ex) {
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
            }
            return true;
        }
    }
    
    private void handlePointsReport(ChannelHandlerContext ctx, LocalDate fromDate, LocalDate toDate) throws JsonProcessingException {
        logger.debug("Generating points report from {} to {}", fromDate, toDate);
        
        // Получаем реальные данные из сервиса
        PointsReportData reportData = reportService.generatePointsReport(fromDate, toDate);
        
        ApiResponse<PointsReportData> response = ApiResponse.success(reportData);
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private void handleUserActivityReport(ChannelHandlerContext ctx, LocalDate fromDate, LocalDate toDate) throws JsonProcessingException {
        logger.debug("Generating user activity report from {} to {}", fromDate, toDate);
        
        Object reportData = reportService.generateUserActivityReport(fromDate, toDate);
        
        ApiResponse<Object> response = ApiResponse.success(reportData);
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private void handlePromotionsReport(ChannelHandlerContext ctx, LocalDate fromDate, LocalDate toDate) throws JsonProcessingException {
        logger.debug("Generating promotions report from {} to {}", fromDate, toDate);
        
        Object reportData = reportService.generatePromotionsReport(fromDate, toDate);
        
        ApiResponse<Object> response = ApiResponse.success(reportData);
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private void handlePromoCodesReport(ChannelHandlerContext ctx, LocalDate fromDate, LocalDate toDate) throws JsonProcessingException {
        logger.debug("Generating promo codes report from {} to {}", fromDate, toDate);
        
        Object reportData = reportService.generatePromoCodesReport(fromDate, toDate);
        
        ApiResponse<Object> response = ApiResponse.success(reportData);
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private void handleFinancialReport(ChannelHandlerContext ctx, LocalDate fromDate, LocalDate toDate) throws JsonProcessingException {
        logger.debug("Generating financial report from {} to {}", fromDate, toDate);
        
        Object reportData = reportService.generateFinancialReport(fromDate, toDate);
        
        ApiResponse<Object> response = ApiResponse.success(reportData);
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private Map<String, String> parseQueryParams(String uri) {
        Map<String, String> queryParams = new HashMap<>();
        
        int queryStartPos = uri.indexOf('?');
        if (queryStartPos >= 0 && queryStartPos < uri.length() - 1) {
            String queryString = uri.substring(queryStartPos + 1);
            String[] params = queryString.split("&");
            
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    queryParams.put(keyValue[0], keyValue[1]);
                }
            }
        }
        
        return queryParams;
    }
    
    // Data classes for reports
    
    public static class PointsReportData {
        private List<PointsRecord> records;
        private Map<String, Integer> pointsByDay;
        
        public PointsReportData() {
        }
        
        public PointsReportData(List<PointsRecord> records, Map<String, Integer> pointsByDay) {
            this.records = records;
            this.pointsByDay = pointsByDay;
        }
        
        public List<PointsRecord> getRecords() {
            return records;
        }
        
        public void setRecords(List<PointsRecord> records) {
            this.records = records;
        }
        
        public Map<String, Integer> getPointsByDay() {
            return pointsByDay;
        }
        
        public void setPointsByDay(Map<String, Integer> pointsByDay) {
            this.pointsByDay = pointsByDay;
        }
    }
    
    public static class PointsRecord {
        private String date;
        private String cardNumber;
        private String userName;
        private Integer points;
        private String description;
        private String type;
        private String reason;
        
        public PointsRecord() {
        }
        
        public PointsRecord(String date, String cardNumber, String userName, Integer points, String description,
                          String type, String reason) {
            this.date = date;
            this.cardNumber = cardNumber;
            this.userName = userName;
            this.points = points;
            this.description = description;
            this.type = type;
            this.reason = reason;
        }
        
        public String getDate() {
            return date;
        }
        
        public void setDate(String date) {
            this.date = date;
        }
        
        public String getCardNumber() {
            return cardNumber;
        }
        
        public void setCardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
        }
        
        public String getUserName() {
            return userName;
        }
        
        public void setUserName(String userName) {
            this.userName = userName;
        }
        
        public Integer getPoints() {
            return points;
        }
        
        public void setPoints(Integer points) {
            this.points = points;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
    }
} 