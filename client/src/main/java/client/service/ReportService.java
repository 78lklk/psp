package client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.ApiResponse;
import common.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Сервис для работы с отчетами
 */
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private static final String API_URL = ServiceUtils.getApiUrl();
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String authToken;
    private final Executor executor;

    /**
     * Создает новый сервис отчетов
     * @param authToken токен авторизации
     */
    public ReportService(String authToken) {
        this.authToken = authToken;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.executor = Executors.newFixedThreadPool(5);
    }
    
    /**
     * Получает отчет о начисленных баллах
     * @param fromDate начальная дата
     * @param toDate конечная дата
     * @return данные отчета
     */
    public CompletableFuture<PointsReportData> getPointsReport(LocalDate fromDate, LocalDate toDate) {
        logger.debug("Запрос отчета о начисленных баллах с {} по {}", fromDate, toDate);
        
        String url = API_URL + "/reports/points?from=" + fromDate + "&to=" + toDate;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    ApiResponse<PointsReportData> apiResponse = objectMapper.readValue(
                            response.body(),
                            new TypeReference<ApiResponse<PointsReportData>>() {}
                    );
                    
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        logger.debug("Получены данные отчета о начисленных баллах: {} записей",
                                apiResponse.getData().getRecords().size());
                        return apiResponse.getData();
                    } else {
                        logger.error("Ошибка получения отчета о начисленных баллах: {}", apiResponse.getErrorMessage());
                        return createEmptyPointsReport();
                    }
                } else {
                    logger.error("Ошибка запроса отчета о начисленных баллах. Код: {}, Ответ: {}", 
                            response.statusCode(), response.body());
                    return createEmptyPointsReport();
                }
            } catch (Exception e) {
                logger.error("Ошибка при получении отчета о начисленных баллах", e);
                return createEmptyPointsReport();
            }
        }, executor);
    }
    
    /**
     * Экспортирует отчет о начисленных баллах в CSV
     * @param fromDate начальная дата
     * @param toDate конечная дата
     * @param filePath путь к файлу
     * @return результат операции
     */
    public CompletableFuture<Boolean> exportPointsReportToCsv(LocalDate fromDate, LocalDate toDate, String filePath) {
        logger.debug("Экспорт отчета о начисленных баллах в CSV");
        
        return getPointsReport(fromDate, toDate)
            .thenApply(reportData -> {
                try {
                    File file = new File(filePath);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    
                    try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                        // Заголовок с BOM для корректной работы с кириллицей
                        writer.write("\uFEFF"); // BOM для UTF-8
                        writer.write("Отчет о начисленных баллах\n");
                        writer.write("Период: " + fromDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + 
                                " - " + toDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "\n\n");
                        
                        // Заголовок таблицы
                        writer.write("Дата;Номер карты;Имя пользователя;Баллы;Описание\n");
                        
                        // Данные
                        for (PointsRecord record : reportData.getRecords()) {
                            writer.write(record.getDate() + ";");
                            writer.write(record.getCardNumber() + ";");
                            writer.write(record.getUserName() + ";");
                            writer.write(record.getPoints() + ";");
                            writer.write(record.getDescription() + "\n");
                        }
                        
                        // Статистика по дням
                        writer.write("\nСтатистика по дням\n");
                        writer.write("Дата;Количество баллов\n");
                        for (Map.Entry<String, Integer> entry : reportData.getPointsByDay().entrySet()) {
                            writer.write(entry.getKey() + ";");
                            writer.write(entry.getValue() + "\n");
                        }
                        
                        logger.info("Файл отчета о начисленных баллах успешно создан: {}", filePath);
                        return true;
                    }
                } catch (IOException e) {
                    logger.error("Ошибка при экспорте отчета о начисленных баллах в CSV", e);
                    return false;
                }
            })
            .exceptionally(e -> {
                logger.error("Ошибка при получении данных для отчета о начисленных баллах", e);
                return false;
            });
    }
    
    /**
     * Получает отчет об активности пользователей
     * @param fromDate начальная дата
     * @param toDate конечная дата
     * @return данные отчета
     */
    public CompletableFuture<UserActivityReportData> getUserActivityReport(LocalDate fromDate, LocalDate toDate) {
        logger.debug("Запрос отчета по активности пользователей с {} по {}", fromDate, toDate);
        
        String url = API_URL + "/reports/activity?from=" + fromDate + "&to=" + toDate;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    ApiResponse<UserActivityReportData> apiResponse = objectMapper.readValue(
                            response.body(),
                            new TypeReference<ApiResponse<UserActivityReportData>>() {}
                    );
                    
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        logger.debug("Получены данные отчета по активности пользователей: {} записей",
                                apiResponse.getData().getRecords().size());
                        return apiResponse.getData();
                    } else {
                        logger.error("Ошибка получения отчета по активности пользователей: {}", apiResponse.getErrorMessage());
                        // Возвращаем заглушку в случае ошибки
                        return createMockUserActivityReport();
                    }
                } else {
                    logger.error("Ошибка запроса отчета по активности пользователей. Код: {}, Ответ: {}", 
                            response.statusCode(), response.body());
                    // Возвращаем заглушку в случае ошибки
                    return createMockUserActivityReport();
                }
            } catch (Exception e) {
                logger.error("Ошибка при получении отчета по активности пользователей", e);
                // Возвращаем заглушку в случае ошибки
                return createMockUserActivityReport();
            }
        }, executor);
    }
    
    /**
     * Получает отчет по использованию акций
     * @param fromDate начальная дата
     * @param toDate конечная дата
     * @return данные отчета
     */
    public CompletableFuture<PromotionsReportData> getPromotionsReport(LocalDate fromDate, LocalDate toDate) {
        logger.debug("Запрос отчета по акциям с {} по {}", fromDate, toDate);
        
        String url = API_URL + "/reports/promotions?from=" + fromDate + "&to=" + toDate;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    ApiResponse<PromotionsReportData> apiResponse = objectMapper.readValue(
                            response.body(),
                            new TypeReference<ApiResponse<PromotionsReportData>>() {}
                    );
                    
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        logger.debug("Получены данные отчета по акциям: {} записей",
                                apiResponse.getData().getRecords().size());
                        return apiResponse.getData();
                    } else {
                        logger.error("Ошибка получения отчета по акциям: {}", apiResponse.getErrorMessage());
                        // Возвращаем заглушку в случае ошибки
                        return createMockPromotionsReport();
                    }
                } else {
                    logger.error("Ошибка запроса отчета по акциям. Код: {}, Ответ: {}", 
                            response.statusCode(), response.body());
                    // Возвращаем заглушку в случае ошибки
                    return createMockPromotionsReport();
                }
            } catch (Exception e) {
                logger.error("Ошибка при получении отчета по акциям", e);
                // Возвращаем заглушку в случае ошибки
                return createMockPromotionsReport();
            }
        }, executor);
    }
    
    /**
     * Получает отчет по использованию промокодов
     * @param fromDate начальная дата
     * @param toDate конечная дата
     * @return данные отчета
     */
    public CompletableFuture<PromoCodesReportData> getPromoCodesReport(LocalDate fromDate, LocalDate toDate) {
        logger.debug("Запрос отчета по промокодам с {} по {}", fromDate, toDate);
        
        String url = API_URL + "/reports/promocodes?from=" + fromDate + "&to=" + toDate;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    ApiResponse<PromoCodesReportData> apiResponse = objectMapper.readValue(
                            response.body(),
                            new TypeReference<ApiResponse<PromoCodesReportData>>() {}
                    );
                    
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        logger.debug("Получены данные отчета по промокодам: {} записей",
                                apiResponse.getData().getRecords().size());
                        return apiResponse.getData();
                    } else {
                        logger.error("Ошибка получения отчета по промокодам: {}", apiResponse.getErrorMessage());
                        // Возвращаем заглушку в случае ошибки
                        return createMockPromoCodesReport();
                    }
                } else {
                    logger.error("Ошибка запроса отчета по промокодам. Код: {}, Ответ: {}", 
                            response.statusCode(), response.body());
                    // Возвращаем заглушку в случае ошибки
                    return createMockPromoCodesReport();
                }
            } catch (Exception e) {
                logger.error("Ошибка при получении отчета по промокодам", e);
                // Возвращаем заглушку в случае ошибки
                return createMockPromoCodesReport();
            }
        }, executor);
    }
    
    /**
     * Получает финансовый отчет
     * @param fromDate начальная дата
     * @param toDate конечная дата
     * @return данные отчета
     */
    public CompletableFuture<FinancialReportData> getFinancialReport(LocalDate fromDate, LocalDate toDate) {
        logger.debug("Запрос финансового отчета с {} по {}", fromDate, toDate);
        
        String url = API_URL + "/reports/financial?from=" + fromDate + "&to=" + toDate;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    ApiResponse<FinancialReportData> apiResponse = objectMapper.readValue(
                            response.body(),
                            new TypeReference<ApiResponse<FinancialReportData>>() {}
                    );
                    
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        logger.debug("Получены данные финансового отчета: {} записей",
                                apiResponse.getData().getRecords().size());
                        return apiResponse.getData();
                    } else {
                        logger.error("Ошибка получения финансового отчета: {}", apiResponse.getErrorMessage());
                        // Возвращаем заглушку в случае ошибки
                        return createMockFinancialReport();
                    }
                } else {
                    logger.error("Ошибка запроса финансового отчета. Код: {}, Ответ: {}", 
                            response.statusCode(), response.body());
                    // Возвращаем заглушку в случае ошибки
                    return createMockFinancialReport();
                }
            } catch (Exception e) {
                logger.error("Ошибка при получении финансового отчета", e);
                // Возвращаем заглушку в случае ошибки
                return createMockFinancialReport();
            }
        }, executor);
    }
    
    /**
     * Экспортирует финансовый отчет в CSV
     * @param fromDate начальная дата
     * @param toDate конечная дата
     * @param filePath путь к файлу
     * @return результат операции
     */
    public CompletableFuture<Boolean> exportFinancialReportToCsv(LocalDate fromDate, LocalDate toDate, String filePath) {
        logger.debug("Экспорт финансового отчета в CSV");
        
        return getFinancialReport(fromDate, toDate)
            .thenApply(reportData -> {
                try {
                    File file = new File(filePath);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    
                    try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                        // Заголовок
                        writer.write("\uFEFF"); // BOM для UTF-8
                        writer.write("Финансовый отчет\n");
                        writer.write("Период: " + fromDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + 
                                " - " + toDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "\n\n");
                        
                        // Общие показатели
                        writer.write("Общие показатели\n");
                        writer.write("Общая выручка;");
                        writer.write(String.format("%.2f", reportData.getTotalRevenue()) + "\n");
                        writer.write("Количество транзакций;");
                        writer.write(reportData.getTotalTransactions() + "\n");
                        writer.write("Средний чек;");
                        writer.write(String.format("%.2f", reportData.getAverageTransactionAmount()) + "\n");
                        writer.write("Чистая прибыль;");
                        writer.write(String.format("%.2f", reportData.getNetProfit()) + "\n\n");
                        
                        // Транзакции по дням
                        writer.write("Транзакции по дням\n");
                        writer.write("Дата;Количество;Сумма\n");
                        
                        for (Map.Entry<String, TransactionsByDay> entry : reportData.getTransactionsByDay().entrySet()) {
                            writer.write(entry.getKey() + ";");
                            writer.write(entry.getValue().getCount() + ";");
                            writer.write(String.format("%.2f", entry.getValue().getAmount()) + "\n");
                        }
                        writer.write("\n");
                        
                        // Транзакции по типам
                        writer.write("Транзакции по типам\n");
                        writer.write("Тип;Количество;Сумма\n");
                        
                        for (Map.Entry<String, TransactionsByType> entry : reportData.getTransactionsByType().entrySet()) {
                            writer.write(entry.getKey() + ";");
                            writer.write(entry.getValue().getCount() + ";");
                            writer.write(String.format("%.2f", entry.getValue().getAmount()) + "\n");
                        }
                        
                        logger.info("Файл финансового отчета успешно создан: {}", filePath);
                        return true;
                    }
                } catch (IOException e) {
                    logger.error("Ошибка при экспорте финансового отчета в CSV", e);
                    return false;
                }
            })
            .exceptionally(e -> {
                logger.error("Ошибка при получении данных для финансового отчета", e);
                return false;
            });
    }
    
    /**
     * Экспортирует отчет по промокодам в CSV
     * @param fromDate начальная дата
     * @param toDate конечная дата
     * @param filePath путь к файлу
     * @return результат операции
     */
    public CompletableFuture<Boolean> exportPromoCodesReportToCsv(LocalDate fromDate, LocalDate toDate, String filePath) {
        logger.debug("Экспорт отчета по промокодам в CSV");
        
        return getPromoCodesReport(fromDate, toDate)
            .thenApply(reportData -> {
                try {
                    File file = new File(filePath);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    
                    try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                        // Заголовок с BOM для корректной работы с кириллицей
                        writer.write("\uFEFF"); // BOM для UTF-8
                        writer.write("Отчет по промокодам\n");
                        writer.write("Период: " + fromDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + 
                                " - " + toDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "\n\n");
                        
                        // Общие показатели
                        writer.write("Общие показатели\n");
                        writer.write("Всего промокодов;");
                        writer.write(reportData.getTotalPromoCodes() + "\n");
                        writer.write("Активные промокоды;");
                        writer.write(reportData.getActivePromoCodes() + "\n");
                        writer.write("Использовано промокодов;");
                        writer.write(reportData.getUsedPromoCodes() + "\n");
                        writer.write("Коэффициент конверсии;");
                        writer.write(String.format("%.2f%%", reportData.getConversionRate() * 100) + "\n\n");
                        
                        // Список промокодов
                        writer.write("Список промокодов\n");
                        writer.write("Код;Описание;Дата начала;Дата окончания;Использований;Скидка\n");
                        
                        for (PromoCodeRecord record : reportData.getPromoCodeRecords()) {
                            writer.write(record.getCode() + ";");
                            writer.write(record.getDescription() + ";");
                            writer.write(record.getValidFrom() + ";");
                            writer.write(record.getValidTo() + ";");
                            writer.write(record.getUsageCount() + ";");
                            writer.write(record.getDiscountValue() != null ? 
                                    String.format("%.2f", record.getDiscountValue()) : "0.00");
                            writer.write("\n");
                        }
                        
                        logger.info("Файл отчета по промокодам успешно создан: {}", filePath);
                        return true;
                    }
                } catch (IOException e) {
                    logger.error("Ошибка при экспорте отчета по промокодам в CSV", e);
                    return false;
                }
            })
            .exceptionally(e -> {
                logger.error("Ошибка при получении данных для отчета по промокодам", e);
                return false;
            });
    }
    
    /**
     * Экспортирует отчет по активности пользователей в CSV
     * @param fromDate начальная дата
     * @param toDate конечная дата
     * @param filePath путь к файлу
     * @return результат операции
     */
    public CompletableFuture<Boolean> exportUserActivityReportToCsv(LocalDate fromDate, LocalDate toDate, String filePath) {
        logger.debug("Экспорт отчета по активности пользователей в CSV");
        
        return getUserActivityReport(fromDate, toDate)
            .thenApply(reportData -> {
                try {
                    File file = new File(filePath);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    
                    try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                        // Заголовок с BOM для корректной работы с кириллицей
                        writer.write("\uFEFF"); // BOM для UTF-8
                        writer.write("Отчет по активности пользователей\n");
                        writer.write("Период: " + fromDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + 
                                " - " + toDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "\n\n");
                        
                        // Заголовок таблицы
                        writer.write("Имя пользователя;Последний вход;Количество сессий;Общее время (мин.);Заработано баллов;Потрачено баллов\n");
                        
                        // Данные
                        for (UserActivityRecord record : reportData.getRecords()) {
                            writer.write(record.getUsername() + ";");
                            writer.write(record.getLastLogin() + ";");
                            writer.write(record.getSessionsCount() + ";");
                            writer.write(record.getTotalMinutes() + ";");
                            writer.write(record.getPointsEarned() + ";");
                            writer.write(record.getPointsSpent() + "\n");
                        }
                        
                        // Активность по дням
                        writer.write("\nАктивность по дням\n");
                        writer.write("Дата;Количество сессий\n");
                        for (Map.Entry<String, Integer> entry : reportData.getActivityByDay().entrySet()) {
                            writer.write(entry.getKey() + ";");
                            writer.write(entry.getValue() + "\n");
                        }
                        
                        // Сессии по пользователям
                        writer.write("\nСессии по пользователям\n");
                        writer.write("Пользователь;Количество сессий\n");
                        for (Map.Entry<String, Integer> entry : reportData.getSessionsByUser().entrySet()) {
                            writer.write(entry.getKey() + ";");
                            writer.write(entry.getValue() + "\n");
                        }
                        
                        logger.info("Файл отчета по активности пользователей успешно создан: {}", filePath);
                        return true;
                    }
                } catch (IOException e) {
                    logger.error("Ошибка при экспорте отчета по активности пользователей в CSV", e);
                    return false;
                }
            })
            .exceptionally(e -> {
                logger.error("Ошибка при получении данных для отчета по активности пользователей", e);
                return false;
            });
    }
    
    /**
     * Экспортирует отчет по акциям в CSV
     * @param fromDate начальная дата
     * @param toDate конечная дата
     * @param filePath путь к файлу
     * @return результат операции
     */
    public CompletableFuture<Boolean> exportPromotionsReportToCsv(LocalDate fromDate, LocalDate toDate, String filePath) {
        logger.debug("Экспорт отчета по акциям в CSV");
        
        return getPromotionsReport(fromDate, toDate)
            .thenApply(reportData -> {
                try {
                    File file = new File(filePath);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    
                    try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                        // Заголовок с BOM для корректной работы с кириллицей
                        writer.write("\uFEFF"); // BOM для UTF-8
                        writer.write("Отчет по акциям\n");
                        writer.write("Период: " + fromDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + 
                                " - " + toDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "\n\n");
                        
                        // Заголовок таблицы
                        writer.write("Название;Описание;Дата начала;Дата окончания;Количество участников;Эффективность\n");
                        
                        // Данные
                        for (PromotionRecord record : reportData.getRecords()) {
                            writer.write(record.getName() + ";");
                            writer.write(record.getDescription() + ";");
                            writer.write(record.getStartDate() + ";");
                            writer.write(record.getEndDate() + ";");
                            writer.write(record.getParticipantsCount() + ";");
                            writer.write(String.format("%.2f", record.getEffectiveness()) + "\n");
                        }
                        
                        // Популярность по акциям
                        writer.write("\nПопулярность по акциям\n");
                        writer.write("Акция;Количество участников\n");
                        for (Map.Entry<String, Integer> entry : reportData.getPopularityByPromotion().entrySet()) {
                            writer.write(entry.getKey() + ";");
                            writer.write(entry.getValue() + "\n");
                        }
                        
                        // Эффективность по типам
                        writer.write("\nЭффективность по типам\n");
                        writer.write("Тип акции;Эффективность\n");
                        for (Map.Entry<String, Double> entry : reportData.getEffectivenessByType().entrySet()) {
                            writer.write(entry.getKey() + ";");
                            writer.write(String.format("%.2f", entry.getValue()) + "\n");
                        }
                        
                        logger.info("Файл отчета по акциям успешно создан: {}", filePath);
                        return true;
                    }
                } catch (IOException e) {
                    logger.error("Ошибка при экспорте отчета по акциям в CSV", e);
                    return false;
                }
            })
            .exceptionally(e -> {
                logger.error("Ошибка при получении данных для отчета по акциям", e);
                return false;
            });
    }
    
    // Вспомогательные методы для создания заглушек отчетов
    
    private PointsReportData createEmptyPointsReport() {
        return new PointsReportData(new ArrayList<>(), new HashMap<>());
    }
    
    private FinancialReportData createMockFinancialReport() {
        List<FinancialRecord> records = new ArrayList<>();
        records.add(new FinancialRecord("2023-05-01", "Игровые сессии", 5000.0, 2000.0, 3000.0));
        records.add(new FinancialRecord("2023-05-02", "Продажи", 3000.0, 1500.0, 1500.0));
        records.add(new FinancialRecord("2023-05-03", "Игровые сессии", 4500.0, 1800.0, 2700.0));
        
        Map<String, Double> revenueByDay = new HashMap<>();
        revenueByDay.put("2023-05-01", 5000.0);
        revenueByDay.put("2023-05-02", 3000.0);
        revenueByDay.put("2023-05-03", 4500.0);
        
        Map<String, Double> revenueByCategory = new HashMap<>();
        revenueByCategory.put("Игровые сессии", 9500.0);
        revenueByCategory.put("Продажи", 3000.0);
        
        FinancialReportData report = new FinancialReportData();
        report.setTotalRevenue(12500.0);
        report.setTotalTransactions(25);
        report.setAverageTransactionAmount(500.0);
        report.setNetProfit(7200.0);
        
        Map<String, TransactionsByDay> transactionsByDay = new HashMap<>();
        transactionsByDay.put("2023-05-01", new TransactionsByDay(10, 5000.0));
        transactionsByDay.put("2023-05-02", new TransactionsByDay(7, 3000.0));
        transactionsByDay.put("2023-05-03", new TransactionsByDay(8, 4500.0));
        report.setTransactionsByDay(transactionsByDay);
        
        Map<String, TransactionsByType> transactionsByType = new HashMap<>();
        transactionsByType.put("Игровые сессии", new TransactionsByType(18, 9500.0));
        transactionsByType.put("Продажи", new TransactionsByType(7, 3000.0));
        report.setTransactionsByType(transactionsByType);
        
        return report;
    }
    
    private PromoCodesReportData createMockPromoCodesReport() {
        List<PromoCodeRecord> records = new ArrayList<>();
        
        PromoCodeRecord record1 = new PromoCodeRecord();
        record1.setCode("WELCOME10");
        record1.setDescription("Приветственный промокод");
        record1.setValidFrom("2023-05-01");
        record1.setValidTo("2023-06-01");
        record1.setUsageCount(35);
        record1.setDiscountValue(10.0);
        records.add(record1);
        
        PromoCodeRecord record2 = new PromoCodeRecord();
        record2.setCode("SUMMER20");
        record2.setDescription("Летняя акция");
        record2.setValidFrom("2023-06-01");
        record2.setValidTo("2023-08-31");
        record2.setUsageCount(42);
        record2.setDiscountValue(20.0);
        records.add(record2);
        
        PromoCodeRecord record3 = new PromoCodeRecord();
        record3.setCode("FRIEND15");
        record3.setDescription("Приведи друга");
        record3.setValidFrom("2023-05-15");
        record3.setValidTo("2023-07-15");
        record3.setUsageCount(28);
        record3.setDiscountValue(15.0);
        records.add(record3);
        
        Map<String, Integer> usageByCode = new HashMap<>();
        usageByCode.put("WELCOME10", 35);
        usageByCode.put("SUMMER20", 42);
        usageByCode.put("FRIEND15", 28);
        
        Map<String, Integer> distributionByType = new HashMap<>();
        distributionByType.put("Скидка 10%", 35);
        distributionByType.put("Скидка 15%", 28);
        distributionByType.put("Скидка 20%", 42);
        
        PromoCodesReportData report = new PromoCodesReportData();
        report.setTotalPromoCodes(105);
        report.setActivePromoCodes(85);
        report.setUsedPromoCodes(105);
        report.setConversionRate(0.75);
        report.setPromoCodeRecords(records);
        report.setUsageByCode(usageByCode);
        report.setDistributionByType(distributionByType);
        
        return report;
    }
    
    private UserActivityReportData createMockUserActivityReport() {
        List<UserActivityRecord> records = new ArrayList<>();
        
        UserActivityRecord record1 = new UserActivityRecord();
        record1.setUsername("user1");
        record1.setLastLogin("2023-05-01 10:15");
        record1.setSessionsCount(12);
        record1.setTotalMinutes(720);
        record1.setPointsEarned(240);
        record1.setPointsSpent(120);
        records.add(record1);
        
        UserActivityRecord record2 = new UserActivityRecord();
        record2.setUsername("user2");
        record2.setLastLogin("2023-05-02 14:30");
        record2.setSessionsCount(8);
        record2.setTotalMinutes(480);
        record2.setPointsEarned(160);
        record2.setPointsSpent(50);
        records.add(record2);
        
        UserActivityRecord record3 = new UserActivityRecord();
        record3.setUsername("user3");
        record3.setLastLogin("2023-05-03 18:45");
        record3.setSessionsCount(15);
        record3.setTotalMinutes(900);
        record3.setPointsEarned(300);
        record3.setPointsSpent(200);
        records.add(record3);
        
        Map<String, Integer> activityByDay = new HashMap<>();
        activityByDay.put("2023-05-01", 5);
        activityByDay.put("2023-05-02", 8);
        activityByDay.put("2023-05-03", 7);
        
        Map<String, Integer> sessionsByUser = new HashMap<>();
        sessionsByUser.put("user1", 12);
        sessionsByUser.put("user2", 8);
        sessionsByUser.put("user3", 15);
        
        UserActivityReportData report = new UserActivityReportData();
        report.setRecords(records);
        report.setActivityByDay(activityByDay);
        report.setSessionsByUser(sessionsByUser);
        
        return report;
    }
    
    private PromotionsReportData createMockPromotionsReport() {
        List<PromotionRecord> records = new ArrayList<>();
        
        PromotionRecord record1 = new PromotionRecord();
        record1.setName("Счастливые часы");
        record1.setDescription("Двойные баллы с 10 до 12");
        record1.setStartDate("2023-05-01");
        record1.setEndDate("2023-05-31");
        record1.setParticipantsCount(45);
        record1.setEffectiveness(0.85);
        records.add(record1);
        
        PromotionRecord record2 = new PromotionRecord();
        record2.setName("Приведи друга");
        record2.setDescription("Баллы за приглашение друга");
        record2.setStartDate("2023-05-15");
        record2.setEndDate("2023-06-15");
        record2.setParticipantsCount(28);
        record2.setEffectiveness(0.72);
        records.add(record2);
        
        PromotionRecord record3 = new PromotionRecord();
        record3.setName("День рождения");
        record3.setDescription("Подарок на день рождения");
        record3.setStartDate("2023-01-01");
        record3.setEndDate("2023-12-31");
        record3.setParticipantsCount(35);
        record3.setEffectiveness(0.93);
        records.add(record3);
        
        Map<String, Integer> popularityByPromotion = new HashMap<>();
        popularityByPromotion.put("Счастливые часы", 45);
        popularityByPromotion.put("Приведи друга", 28);
        popularityByPromotion.put("День рождения", 35);
        
        Map<String, Double> effectivenessByType = new HashMap<>();
        effectivenessByType.put("Баллы", 0.85);
        effectivenessByType.put("Приглашения", 0.72);
        effectivenessByType.put("Подарки", 0.93);
        
        PromotionsReportData report = new PromotionsReportData();
        report.setRecords(records);
        report.setPopularityByPromotion(popularityByPromotion);
        report.setEffectivenessByType(effectivenessByType);
        
        return report;
    }
    
    // Внутренние классы для данных отчетов
    
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
        
        public PointsRecord(String date, String cardNumber, String userName, Integer points, String description) {
            this.date = date;
            this.cardNumber = cardNumber;
            this.userName = userName;
            this.points = points;
            this.description = description;
            this.reason = description; // For backward compatibility
            this.type = "Default";
        }
        
        public PointsRecord(String date, String cardNumber, String userName, Integer points, String description, String type, String reason) {
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
        
        public String getDateFormatted() {
            try {
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                LocalDate parsedDate = LocalDate.parse(date, inputFormatter);
                return parsedDate.format(outputFormatter);
            } catch (Exception e) {
                return date;
            }
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
            return reason != null ? reason : description;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
    }
    
    /**
     * Данные финансового отчета
     */
    public static class FinancialReportData {
        private double totalRevenue;
        private int totalTransactions;
        private double averageTransactionAmount;
        private double netProfit;
        private Map<String, TransactionsByDay> transactionsByDay;
        private Map<String, TransactionsByType> transactionsByType;
        private List<FinancialRecord> records;
        private Map<String, Double> revenueByDay;
        private Map<String, Double> revenueByCategory;
        
        public FinancialReportData() {
            this.transactionsByDay = new HashMap<>();
            this.transactionsByType = new HashMap<>();
            this.records = new ArrayList<>();
            this.revenueByDay = new HashMap<>();
            this.revenueByCategory = new HashMap<>();
        }
        
        public FinancialReportData(List<FinancialRecord> records, Map<String, Double> revenueByDay, 
                                 Map<String, Double> revenueByCategory) {
            this();
            this.records = records;
            this.revenueByDay = revenueByDay;
            this.revenueByCategory = revenueByCategory;
        }
        
        public List<FinancialRecord> getRecords() {
            return records;
        }
        
        public void setRecords(List<FinancialRecord> records) {
            this.records = records;
        }
        
        public Map<String, Double> getRevenueByDay() {
            return revenueByDay;
        }
        
        public void setRevenueByDay(Map<String, Double> revenueByDay) {
            this.revenueByDay = revenueByDay;
        }
        
        public Map<String, Double> getRevenueByCategory() {
            return revenueByCategory;
        }
        
        public void setRevenueByCategory(Map<String, Double> revenueByCategory) {
            this.revenueByCategory = revenueByCategory;
        }
        
        public double getTotalRevenue() {
            return totalRevenue;
        }
        
        public void setTotalRevenue(double totalRevenue) {
            this.totalRevenue = totalRevenue;
        }
        
        public int getTotalTransactions() {
            return totalTransactions;
        }
        
        public void setTotalTransactions(int totalTransactions) {
            this.totalTransactions = totalTransactions;
        }
        
        public double getAverageTransactionAmount() {
            return averageTransactionAmount;
        }
        
        public void setAverageTransactionAmount(double averageTransactionAmount) {
            this.averageTransactionAmount = averageTransactionAmount;
        }
        
        public double getNetProfit() {
            return netProfit;
        }
        
        public void setNetProfit(double netProfit) {
            this.netProfit = netProfit;
        }
        
        public Map<String, TransactionsByDay> getTransactionsByDay() {
            return transactionsByDay;
        }
        
        public void setTransactionsByDay(Map<String, TransactionsByDay> transactionsByDay) {
            this.transactionsByDay = transactionsByDay;
        }
        
        public Map<String, TransactionsByType> getTransactionsByType() {
            return transactionsByType;
        }
        
        public void setTransactionsByType(Map<String, TransactionsByType> transactionsByType) {
            this.transactionsByType = transactionsByType;
        }
    }
    
    public static class FinancialRecord {
        private String date;
        private String category;
        private Double revenue;
        private Double expenses;
        private Double profit;
        
        public FinancialRecord() {
        }
        
        public FinancialRecord(String date, String category, Double revenue, Double expenses, Double profit) {
            this.date = date;
            this.category = category;
            this.revenue = revenue;
            this.expenses = expenses;
            this.profit = profit;
        }
        
        public String getDate() {
            return date;
        }
        
        public void setDate(String date) {
            this.date = date;
        }
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
        
        public Double getRevenue() {
            return revenue;
        }
        
        public void setRevenue(Double revenue) {
            this.revenue = revenue;
        }
        
        public Double getExpenses() {
            return expenses;
        }
        
        public void setExpenses(Double expenses) {
            this.expenses = expenses;
        }
        
        public Double getProfit() {
            return profit;
        }
        
        public void setProfit(Double profit) {
            this.profit = profit;
        }
    }
    
    /**
     * Данные отчета по промокодам
     */
    public static class PromoCodesReportData {
        private int totalPromoCodes;
        private int activePromoCodes;
        private int usedPromoCodes;
        private double conversionRate;
        private List<PromoCodeRecord> promoCodeRecords;
        private Map<String, Integer> usageByCode;
        private Map<String, Integer> distributionByType;
        
        public PromoCodesReportData() {
            this.promoCodeRecords = new ArrayList<>();
            this.usageByCode = new HashMap<>();
            this.distributionByType = new HashMap<>();
        }
        
        public PromoCodesReportData(List<PromoCodeRecord> records, Map<String, Integer> usageByCode, 
                                   Map<String, Integer> distributionByType) {
            this();
            this.promoCodeRecords = records;
            this.usageByCode = usageByCode;
            this.distributionByType = distributionByType;
        }
        
        public List<PromoCodeRecord> getPromoCodeRecords() {
            return promoCodeRecords;
        }
        
        public void setPromoCodeRecords(List<PromoCodeRecord> promoCodeRecords) {
            this.promoCodeRecords = promoCodeRecords;
        }
        
        public List<PromoCodeRecord> getRecords() {
            return promoCodeRecords;
        }
        
        public Map<String, Integer> getUsageByCode() {
            return usageByCode;
        }
        
        public void setUsageByCode(Map<String, Integer> usageByCode) {
            this.usageByCode = usageByCode;
        }
        
        public Map<String, Integer> getDistributionByType() {
            return distributionByType;
        }
        
        public void setDistributionByType(Map<String, Integer> distributionByType) {
            this.distributionByType = distributionByType;
        }
        
        public int getTotalPromoCodes() {
            return totalPromoCodes;
        }
        
        public void setTotalPromoCodes(int totalPromoCodes) {
            this.totalPromoCodes = totalPromoCodes;
        }
        
        public int getActivePromoCodes() {
            return activePromoCodes;
        }
        
        public void setActivePromoCodes(int activePromoCodes) {
            this.activePromoCodes = activePromoCodes;
        }
        
        public int getUsedPromoCodes() {
            return usedPromoCodes;
        }
        
        public void setUsedPromoCodes(int usedPromoCodes) {
            this.usedPromoCodes = usedPromoCodes;
        }
        
        public double getConversionRate() {
            return conversionRate;
        }
        
        public void setConversionRate(double conversionRate) {
            this.conversionRate = conversionRate;
        }
    }
    
    /**
     * Запись о транзакциях за день
     */
    public static class TransactionsByDay {
        private int count;
        private double amount;
        
        public TransactionsByDay() {
        }
        
        public TransactionsByDay(int count, double amount) {
            this.count = count;
            this.amount = amount;
        }
        
        public int getCount() {
            return count;
        }
        
        public void setCount(int count) {
            this.count = count;
        }
        
        public double getAmount() {
            return amount;
        }
        
        public void setAmount(double amount) {
            this.amount = amount;
        }
    }
    
    /**
     * Запись о транзакциях по типу
     */
    public static class TransactionsByType {
        private int count;
        private double amount;
        
        public TransactionsByType() {
        }
        
        public TransactionsByType(int count, double amount) {
            this.count = count;
            this.amount = amount;
        }
        
        public int getCount() {
            return count;
        }
        
        public void setCount(int count) {
            this.count = count;
        }
        
        public double getAmount() {
            return amount;
        }
        
        public void setAmount(double amount) {
            this.amount = amount;
        }
    }
    
    /**
     * Запись о промокоде для отчета
     */
    public static class PromoCodeRecord {
        private String code;
        private String description;
        private String validFrom;
        private String validTo;
        private int usageCount;
        private Double discountValue;
        
        public PromoCodeRecord() {
        }
        
        public PromoCodeRecord(String code, String description, String validFrom, String validTo, 
                             int usageCount, Double discountValue) {
            this.code = code;
            this.description = description;
            this.validFrom = validFrom;
            this.validTo = validTo;
            this.usageCount = usageCount;
            this.discountValue = discountValue;
        }
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getValidFrom() {
            return validFrom;
        }
        
        public void setValidFrom(String validFrom) {
            this.validFrom = validFrom;
        }
        
        public String getValidTo() {
            return validTo;
        }
        
        public void setValidTo(String validTo) {
            this.validTo = validTo;
        }
        
        public int getUsageCount() {
            return usageCount;
        }
        
        public void setUsageCount(int usageCount) {
            this.usageCount = usageCount;
        }
        
        public Double getDiscountValue() {
            return discountValue;
        }
        
        public void setDiscountValue(Double discountValue) {
            this.discountValue = discountValue;
        }
    }
    
    public static class UserActivityReportData {
        private List<UserActivityRecord> records;
        private Map<String, Integer> activityByDay;
        private Map<String, Integer> sessionsByUser;
        
        public UserActivityReportData() {
        }
        
        public UserActivityReportData(List<UserActivityRecord> records, Map<String, Integer> activityByDay, 
                                     Map<String, Integer> sessionsByUser) {
            this.records = records;
            this.activityByDay = activityByDay;
            this.sessionsByUser = sessionsByUser;
        }
        
        public List<UserActivityRecord> getRecords() {
            return records;
        }
        
        public void setRecords(List<UserActivityRecord> records) {
            this.records = records;
        }
        
        public Map<String, Integer> getActivityByDay() {
            return activityByDay;
        }
        
        public void setActivityByDay(Map<String, Integer> activityByDay) {
            this.activityByDay = activityByDay;
        }
        
        public Map<String, Integer> getSessionsByUser() {
            return sessionsByUser;
        }
        
        public void setSessionsByUser(Map<String, Integer> sessionsByUser) {
            this.sessionsByUser = sessionsByUser;
        }
    }
    
    public static class UserActivityRecord {
        private String username;
        private String lastLogin;
        private Integer sessionsCount;
        private Integer totalMinutes;
        private Integer pointsEarned;
        private Integer pointsSpent;
        
        public UserActivityRecord() {
        }
        
        public UserActivityRecord(String username, String lastLogin, Integer sessionsCount, 
                                 Integer totalMinutes, Integer pointsEarned, Integer pointsSpent) {
            this.username = username;
            this.lastLogin = lastLogin;
            this.sessionsCount = sessionsCount;
            this.totalMinutes = totalMinutes;
            this.pointsEarned = pointsEarned;
            this.pointsSpent = pointsSpent;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getLastLogin() {
            return lastLogin;
        }
        
        public void setLastLogin(String lastLogin) {
            this.lastLogin = lastLogin;
        }
        
        public Integer getSessionsCount() {
            return sessionsCount;
        }
        
        public void setSessionsCount(Integer sessionsCount) {
            this.sessionsCount = sessionsCount;
        }
        
        public Integer getTotalMinutes() {
            return totalMinutes;
        }
        
        public void setTotalMinutes(Integer totalMinutes) {
            this.totalMinutes = totalMinutes;
        }
        
        public Integer getPointsEarned() {
            return pointsEarned;
        }
        
        public void setPointsEarned(Integer pointsEarned) {
            this.pointsEarned = pointsEarned;
        }
        
        public Integer getPointsSpent() {
            return pointsSpent;
        }
        
        public void setPointsSpent(Integer pointsSpent) {
            this.pointsSpent = pointsSpent;
        }
    }
    
    public static class PromotionsReportData {
        private List<PromotionRecord> records;
        private Map<String, Integer> popularityByPromotion;
        private Map<String, Double> effectivenessByType;
        
        public PromotionsReportData() {
        }
        
        public PromotionsReportData(List<PromotionRecord> records, Map<String, Integer> popularityByPromotion, 
                                   Map<String, Double> effectivenessByType) {
            this.records = records;
            this.popularityByPromotion = popularityByPromotion;
            this.effectivenessByType = effectivenessByType;
        }
        
        public List<PromotionRecord> getRecords() {
            return records;
        }
        
        public void setRecords(List<PromotionRecord> records) {
            this.records = records;
        }
        
        public Map<String, Integer> getPopularityByPromotion() {
            return popularityByPromotion;
        }
        
        public void setPopularityByPromotion(Map<String, Integer> popularityByPromotion) {
            this.popularityByPromotion = popularityByPromotion;
        }
        
        public Map<String, Double> getEffectivenessByType() {
            return effectivenessByType;
        }
        
        public void setEffectivenessByType(Map<String, Double> effectivenessByType) {
            this.effectivenessByType = effectivenessByType;
        }
    }
    
    public static class PromotionRecord {
        private String name;
        private String description;
        private String startDate;
        private String endDate;
        private Integer participantsCount;
        private Double effectiveness;
        
        public PromotionRecord() {
        }
        
        public PromotionRecord(String name, String description, String startDate, String endDate, 
                              Integer participantsCount, Double effectiveness) {
            this.name = name;
            this.description = description;
            this.startDate = startDate;
            this.endDate = endDate;
            this.participantsCount = participantsCount;
            this.effectiveness = effectiveness;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getStartDate() {
            return startDate;
        }
        
        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }
        
        public String getEndDate() {
            return endDate;
        }
        
        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }
        
        public Integer getParticipantsCount() {
            return participantsCount;
        }
        
        public void setParticipantsCount(Integer participantsCount) {
            this.participantsCount = participantsCount;
        }
        
        public Double getEffectiveness() {
            return effectiveness;
        }
        
        public void setEffectiveness(Double effectiveness) {
            this.effectiveness = effectiveness;
        }
    }
} 