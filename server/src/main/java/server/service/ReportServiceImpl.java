package server.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.db.DatabaseConfig;
import server.handler.ReportHandler.PointsRecord;
import server.handler.ReportHandler.PointsReportData;

/**
 * Реализация сервиса для работы с отчетами
 */
public class ReportServiceImpl implements ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);

    @Override
    public PointsReportData generatePointsReport(LocalDate fromDate, LocalDate toDate) {
        logger.debug("Generating points report from {} to {}", fromDate, toDate);
        
        List<PointsRecord> records = new ArrayList<>();
        Map<String, Integer> pointsByDay = new HashMap<>();
        
        String query = """
            SELECT 
                t.timestamp,
                c.number as card_number,
                u.login as user_name,
                t.points,
                t.description,
                t.type
            FROM transactions t
            JOIN cards c ON t.card_id = c.id
            JOIN users u ON c.user_id = u.id
            WHERE t.timestamp BETWEEN ? AND ?
            ORDER BY t.timestamp DESC
        """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setObject(1, fromDate.atStartOfDay());
            stmt.setObject(2, toDate.plusDays(1).atStartOfDay());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
                    String date = timestamp.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                    String cardNumber = rs.getString("card_number");
                    String userName = rs.getString("user_name");
                    int points = rs.getInt("points");
                    String description = rs.getString("description");
                    String type = rs.getString("type");
                    
                    PointsRecord record = new PointsRecord(date, cardNumber, userName, points, description, type, description);
                    records.add(record);
                    
                    // Суммируем баллы по дням
                    pointsByDay.merge(date, Math.abs(points), Integer::sum);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Ошибка при получении данных для отчета о баллах", e);
        }
        
        return new PointsReportData(records, pointsByDay);
    }

    @Override
    public Object generateUserActivityReport(LocalDate fromDate, LocalDate toDate) {
        logger.debug("Generating user activity report from {} to {}", fromDate, toDate);
        
        Map<String, Object> report = new HashMap<>();
        List<Map<String, Object>> userRecords = new ArrayList<>();
        Map<String, Integer> activityByDay = new HashMap<>();
        Map<String, Integer> sessionsByUser = new HashMap<>();
        
        // Получаем активность пользователей
        String userQuery = """
            SELECT 
                u.login as username,
                u.registration_date as last_login,
                COUNT(s.id) as sessions_count,
                COALESCE(SUM(s.minutes), 0) as total_minutes,
                COALESCE(SUM(CASE WHEN t.points > 0 THEN t.points ELSE 0 END), 0) as points_earned,
                COALESCE(SUM(CASE WHEN t.points < 0 THEN ABS(t.points) ELSE 0 END), 0) as points_spent
            FROM users u
            LEFT JOIN cards c ON c.user_id = u.id
            LEFT JOIN sessions s ON s.card_id = c.id AND s.start_time BETWEEN ? AND ?
            LEFT JOIN transactions t ON t.card_id = c.id AND t.timestamp BETWEEN ? AND ?
            GROUP BY u.id, u.login, u.registration_date
            ORDER BY sessions_count DESC
        """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(userQuery)) {
            
            stmt.setObject(1, fromDate.atStartOfDay());
            stmt.setObject(2, toDate.plusDays(1).atStartOfDay());
            stmt.setObject(3, fromDate.atStartOfDay());
            stmt.setObject(4, toDate.plusDays(1).atStartOfDay());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> userRecord = new HashMap<>();
                    String username = rs.getString("username");
                    int sessionsCount = rs.getInt("sessions_count");
                    
                    userRecord.put("username", username);
                    userRecord.put("lastLogin", rs.getTimestamp("last_login").toLocalDateTime().toString());
                    userRecord.put("sessionsCount", sessionsCount);
                    userRecord.put("totalMinutes", rs.getInt("total_minutes"));
                    userRecord.put("pointsEarned", rs.getInt("points_earned"));
                    userRecord.put("pointsSpent", rs.getInt("points_spent"));
                    
                    userRecords.add(userRecord);
                    sessionsByUser.put(username, sessionsCount);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении данных активности пользователей", e);
        }
        
        // Получаем активность по дням
        String dailyQuery = """
            SELECT 
                DATE(s.start_time) as session_date,
                COUNT(*) as session_count
            FROM sessions s
            WHERE s.start_time BETWEEN ? AND ?
            GROUP BY DATE(s.start_time)
            ORDER BY session_date
        """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(dailyQuery)) {
            
            stmt.setObject(1, fromDate.atStartOfDay());
            stmt.setObject(2, toDate.plusDays(1).atStartOfDay());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String date = rs.getDate("session_date").toString();
                    int count = rs.getInt("session_count");
                    activityByDay.put(date, count);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении ежедневной активности", e);
        }
        
        report.put("records", userRecords);
        report.put("activityByDay", activityByDay);
        report.put("sessionsByUser", sessionsByUser);
        
        return report;
    }

    @Override
    public Object generatePromotionsReport(LocalDate fromDate, LocalDate toDate) {
        logger.debug("Generating promotions report from {} to {}", fromDate, toDate);
        
        Map<String, Object> report = new HashMap<>();
        List<Map<String, Object>> promotionRecords = new ArrayList<>();
        Map<String, Integer> popularityByPromotion = new HashMap<>();
        Map<String, Double> effectivenessByType = new HashMap<>();
        
        String query = """
            SELECT 
                p.name,
                p.description,
                p.start_date,
                p.end_date,
                p.bonus_pct,
                COUNT(cp.id) as participants_count
            FROM promotions p
            LEFT JOIN card_promotions cp ON cp.promotion_id = p.id
            WHERE p.start_date <= ? AND p.end_date >= ?
            GROUP BY p.id, p.name, p.description, p.start_date, p.end_date, p.bonus_pct
            ORDER BY participants_count DESC
        """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setObject(1, toDate);
            stmt.setObject(2, fromDate);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    String name = rs.getString("name");
                    int participants = rs.getInt("participants_count");
                    int bonusPct = rs.getInt("bonus_pct");
                    
                    // Расчет эффективности на основе бонусного процента и участников
                    double effectiveness = participants > 0 ? Math.min(1.0, participants / 100.0 * bonusPct / 10.0) : 0.0;
                    
                    record.put("name", name);
                    record.put("description", rs.getString("description"));
                    record.put("startDate", rs.getDate("start_date").toString());
                    record.put("endDate", rs.getDate("end_date").toString());
            record.put("participantsCount", participants);
            record.put("effectiveness", effectiveness);
            
            promotionRecords.add(record);
                    popularityByPromotion.put(name, participants);
                    
                    // Группируем по типу бонуса
                    String promotionType = bonusPct >= 20 ? "Высокий бонус" : 
                                         bonusPct >= 10 ? "Средний бонус" : "Низкий бонус";
                    effectivenessByType.merge(promotionType, effectiveness, (old, val) -> (old + val) / 2);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении данных отчета по акциям", e);
        }
        
        report.put("records", promotionRecords);
        report.put("popularityByPromotion", popularityByPromotion);
        report.put("effectivenessByType", effectivenessByType);
        
        return report;
    }

    @Override
    public Object generatePromoCodesReport(LocalDate fromDate, LocalDate toDate) {
        logger.debug("Generating promo codes report from {} to {}", fromDate, toDate);
        
        Map<String, Object> report = new HashMap<>();
        List<Map<String, Object>> promoCodeRecords = new ArrayList<>();
        Map<String, Integer> usageByCode = new HashMap<>();
        Map<String, Integer> distributionByType = new HashMap<>();
        
        int totalPromos = 0;
        int activePromos = 0;
        int usedPromos = 0;
        
        String query = """
            SELECT 
                pc.code,
                p.name as promotion_name,
                pc.expiry_date,
                pc.is_used,
                pc.used_date,
                p.bonus_pct
            FROM promo_codes pc
            JOIN promotions p ON pc.promotion_id = p.id
            WHERE pc.expiry_date >= ? OR pc.used_date BETWEEN ? AND ?
            ORDER BY pc.used_date DESC NULLS LAST
        """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setObject(1, fromDate);
            stmt.setObject(2, fromDate.atStartOfDay());
            stmt.setObject(3, toDate.plusDays(1).atStartOfDay());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("code");
                    boolean isUsed = rs.getBoolean("is_used");
                    int bonusPct = rs.getInt("bonus_pct");
                    
                    totalPromos++;
                    if (!isUsed && rs.getDate("expiry_date").toLocalDate().isAfter(LocalDate.now())) {
                        activePromos++;
                    }
                    if (isUsed) {
                        usedPromos++;
                    }
                    
                    Map<String, Object> record = new HashMap<>();
            record.put("code", code);
                    record.put("description", "Промокод для акции: " + rs.getString("promotion_name"));
            record.put("validFrom", fromDate.toString());
                    record.put("validTo", rs.getDate("expiry_date").toString());
                    record.put("usageCount", isUsed ? 1 : 0);
                    record.put("discountValue", (double) bonusPct);
            
            promoCodeRecords.add(record);
                    usageByCode.put(code, isUsed ? 1 : 0);
                    
                    // Группируем по типу скидки
                    String type = bonusPct >= 20 ? "Высокая скидка" : 
                                 bonusPct >= 10 ? "Средняя скидка" : "Низкая скидка";
                    distributionByType.merge(type, 1, Integer::sum);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении данных отчета по промокодам", e);
        }
        
        double conversionRate = activePromos > 0 ? (double) usedPromos / activePromos : 0.0;
        
        report.put("records", promoCodeRecords);
        report.put("usageByCode", usageByCode);
        report.put("distributionByType", distributionByType);
        report.put("totalPromoCodes", totalPromos);
        report.put("activePromoCodes", activePromos);
        report.put("usedPromoCodes", usedPromos);
        report.put("conversionRate", conversionRate);
        
        return report;
    }

    @Override
    public Object generateFinancialReport(LocalDate fromDate, LocalDate toDate) {
        logger.debug("Generating financial report from {} to {}", fromDate, toDate);
        
        Map<String, Object> report = new HashMap<>();
        List<Map<String, Object>> financialRecords = new ArrayList<>();
        Map<String, Double> revenueByDay = new HashMap<>();
        Map<String, Double> revenueByCategory = new HashMap<>();
        
        double totalRevenue = 0.0;
        int totalTransactions = 0;
        
        // Получаем данные по сессиям (основной доход)
        String sessionQuery = """
            SELECT 
                DATE(s.start_time) as session_date,
                COUNT(*) as session_count,
                SUM(s.price) as daily_revenue
            FROM sessions s
            WHERE s.start_time BETWEEN ? AND ? AND s.price IS NOT NULL
            GROUP BY DATE(s.start_time)
            ORDER BY session_date
        """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sessionQuery)) {
            
            stmt.setObject(1, fromDate.atStartOfDay());
            stmt.setObject(2, toDate.plusDays(1).atStartOfDay());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String date = rs.getDate("session_date").toString();
                    double revenue = rs.getDouble("daily_revenue");
                    int transactions = rs.getInt("session_count");
                
                Map<String, Object> record = new HashMap<>();
                    record.put("date", date);
                    record.put("category", "Игровые сессии");
                record.put("revenue", revenue);
                    record.put("expenses", revenue * 0.3); // Предполагаем 30% расходов
                    record.put("profit", revenue * 0.7);
                
                financialRecords.add(record);
                    revenueByDay.merge(date, revenue, Double::sum);
                    revenueByCategory.merge("Игровые сессии", revenue, Double::sum);
                
                totalRevenue += revenue;
                totalTransactions += transactions;
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении финансовых данных по сессиям", e);
        }
        
        double averageTransaction = totalTransactions > 0 ? totalRevenue / totalTransactions : 0.0;
        double netProfit = totalRevenue * 0.7; // 70% прибыль
        
        report.put("records", financialRecords);
        report.put("revenueByDay", revenueByDay);
        report.put("revenueByCategory", revenueByCategory);
        report.put("totalRevenue", totalRevenue);
        report.put("totalTransactions", totalTransactions);
        report.put("averageTransactionAmount", averageTransaction);
        report.put("netProfit", netProfit);
        
        return report;
    }
} 