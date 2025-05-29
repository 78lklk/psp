package server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.ApiResponse;
import common.dto.PromotionStatisticsDTO;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.service.PromotionService;
import server.service.PromotionServiceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Обработчик для статистики и аналитических данных
 */
public class StatisticsHandler extends AbstractRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsHandler.class);
    private final PromotionService promotionService;
    private final Random random = new Random();
    
    public StatisticsHandler() {
        super();
        this.promotionService = new PromotionServiceImpl();
    }
    
    @Override
    protected String getUrlPattern() {
        return "/api/statistics.*";
    }
    
    @Override
    protected String getMethod() {
        return "GET";
    }
    
    @Override
    public boolean canHandle(String uri, String method) {
        return uri.startsWith("/api/statistics") && 
               method.matches(getMethod());
    }
    
    @Override
    public boolean handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String method = request.method().name();
        
        logger.debug("StatisticsHandler processing: {} {}", method, uri);
        
        if (!canHandle(uri, method)) {
            return false;
        }
        
        try {
            // GET /api/statistics/promotions - получить статистику по акциям
            if (uri.equals("/api/statistics/promotions") && method.equals(HttpMethod.GET.name())) {
                handleGetPromotionsStatistics(ctx);
                return true;
            }
            
            // Здесь могут быть другие эндпоинты для статистики
            
            return false;
        } catch (Exception e) {
            logger.error("Error processing statistics request", e);
            try {
                ApiResponse<String> response = ApiResponse.error("Error processing request: " + e.getMessage());
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, response);
            } catch (Exception ex) {
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
            }
            return true;
        }
    }
    
    private void handleGetPromotionsStatistics(ChannelHandlerContext ctx) throws JsonProcessingException {
        logger.debug("Getting promotions statistics");
        
        // Получаем все акции из сервиса
        List<PromotionStatisticsDTO> stats = generatePromotionsStatistics();
        ApiResponse<List<PromotionStatisticsDTO>> response = ApiResponse.success(stats);
        
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private List<PromotionStatisticsDTO> generatePromotionsStatistics() {
        // В реальном приложении эти данные должны приходить из базы данных
        // Здесь мы генерируем демонстрационные данные
        List<PromotionStatisticsDTO> statistics = new ArrayList<>();
        
        // Получаем акции из сервиса
        var promotions = promotionService.getAllPromotions();
        
        for (var promotion : promotions) {
            PromotionStatisticsDTO stat = new PromotionStatisticsDTO();
            stat.setPromotionId(promotion.getId());
            stat.setPromotionName(promotion.getName());
            stat.setUsageCount((int)(Math.random() * 100)); // Случайное число использований
            stat.setDiscountAmount((double)Math.round(Math.random() * 1000 * 100) / 100); // Случайная сумма скидки
            
            // Добавляем данные по дням
            Map<String, Integer> usageByDay = new HashMap<>();
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < 7; i++) {
                LocalDateTime day = now.minusDays(i);
                String dayStr = day.toLocalDate().toString();
                usageByDay.put(dayStr, (int)(Math.random() * 20)); // Случайное число использований в день
            }
            stat.setUsageByDay(usageByDay);
            
            // Добавляем дополнительные статистические данные
            stat.setTotalBonusPoints(random.nextInt(5000));
            stat.setAverageBonusPoints(50 + random.nextDouble() * 200);
            stat.setUsedPromoCodes(random.nextInt(100));
            stat.setPromoCodeConversion(0.1 + random.nextDouble() * 0.5);
            
            // Добавляем данные по использованию
            Map<String, Integer> promotionUsageData = new HashMap<>();
            promotionUsageData.put("Первичные клиенты", 10 + random.nextInt(50));
            promotionUsageData.put("Постоянные клиенты", 20 + random.nextInt(80));
            promotionUsageData.put("VIP-клиенты", 5 + random.nextInt(30));
            stat.setPromotionUsageData(promotionUsageData);
            
            // Добавляем данные по промокодам
            Map<String, Integer> promoCodeUsageData = new HashMap<>();
            promoCodeUsageData.put("Промокод 10%", 15 + random.nextInt(40));
            promoCodeUsageData.put("Промокод 15%", 10 + random.nextInt(30));
            promoCodeUsageData.put("Промокод 20%", 5 + random.nextInt(20));
            stat.setPromoCodeUsageData(promoCodeUsageData);
            
            // Добавляем данные по дневной активности
            Map<String, Integer> dailyActivityData = new HashMap<>();
            for (int i = 0; i < 7; i++) {
                LocalDateTime day = now.minusDays(i);
                String dayStr = day.toLocalDate().toString();
                dailyActivityData.put(dayStr, 30 + random.nextInt(100));
            }
            stat.setDailyActivityData(dailyActivityData);
            
            statistics.add(stat);
        }
        
        // Если акций нет или база данных недоступна, добавляем демо-данные
        if (statistics.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                PromotionStatisticsDTO stat = new PromotionStatisticsDTO();
                stat.setPromotionId((long)(i + 1));
                stat.setPromotionName("Промоакция " + (i + 1));
                stat.setUsageCount(50 + random.nextInt(100));
                stat.setDiscountAmount((double)Math.round(random.nextInt(2000) * 100) / 100);
                
                // Добавляем данные по дням
                Map<String, Integer> usageByDay = new HashMap<>();
                LocalDateTime now = LocalDateTime.now();
                for (int j = 0; j < 7; j++) {
                    LocalDateTime day = now.minusDays(j);
                    String dayStr = day.toLocalDate().toString();
                    usageByDay.put(dayStr, 5 + random.nextInt(25));
                }
                stat.setUsageByDay(usageByDay);
                
                // Аналогично добавляем остальные данные, как выше
                stat.setTotalBonusPoints(random.nextInt(5000));
                stat.setAverageBonusPoints(50 + random.nextDouble() * 200);
                stat.setUsedPromoCodes(random.nextInt(100));
                stat.setPromoCodeConversion(0.1 + random.nextDouble() * 0.5);
                
                Map<String, Integer> promotionUsageData = new HashMap<>();
                promotionUsageData.put("Первичные клиенты", 10 + random.nextInt(50));
                promotionUsageData.put("Постоянные клиенты", 20 + random.nextInt(80));
                promotionUsageData.put("VIP-клиенты", 5 + random.nextInt(30));
                stat.setPromotionUsageData(promotionUsageData);
                
                Map<String, Integer> promoCodeUsageData = new HashMap<>();
                promoCodeUsageData.put("Промокод 10%", 15 + random.nextInt(40));
                promoCodeUsageData.put("Промокод 15%", 10 + random.nextInt(30));
                promoCodeUsageData.put("Промокод 20%", 5 + random.nextInt(20));
                stat.setPromoCodeUsageData(promoCodeUsageData);
                
                Map<String, Integer> dailyActivityData = new HashMap<>();
                for (int j = 0; j < 7; j++) {
                    LocalDateTime day = now.minusDays(j);
                    String dayStr = day.toLocalDate().toString();
                    dailyActivityData.put(dayStr, 30 + random.nextInt(100));
                }
                stat.setDailyActivityData(dailyActivityData);
                
                statistics.add(stat);
            }
        }
        
        return statistics;
    }
} 