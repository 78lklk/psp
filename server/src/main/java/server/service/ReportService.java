package server.service;

import server.handler.ReportHandler.PointsReportData;
import java.time.LocalDate;

/**
 * Сервис для работы с отчетами
 */
public interface ReportService {
    /**
     * Генерирует отчет о начисленных баллах
     * @param fromDate начальная дата периода
     * @param toDate конечная дата периода
     * @return отчет о начисленных баллах
     */
    PointsReportData generatePointsReport(LocalDate fromDate, LocalDate toDate);

    /**
     * Генерирует отчет об активности пользователей
     * @param fromDate начальная дата периода
     * @param toDate конечная дата периода
     * @return отчет об активности пользователей
     */
    Object generateUserActivityReport(LocalDate fromDate, LocalDate toDate);

    /**
     * Генерирует отчет по акциям
     * @param fromDate начальная дата периода
     * @param toDate конечная дата периода
     * @return отчет по акциям
     */
    Object generatePromotionsReport(LocalDate fromDate, LocalDate toDate);

    /**
     * Генерирует отчет по промокодам
     * @param fromDate начальная дата периода
     * @param toDate конечная дата периода
     * @return отчет по промокодам
     */
    Object generatePromoCodesReport(LocalDate fromDate, LocalDate toDate);

    /**
     * Генерирует финансовый отчет
     * @param fromDate начальная дата периода
     * @param toDate конечная дата периода
     * @return финансовый отчет
     */
    Object generateFinancialReport(LocalDate fromDate, LocalDate toDate);
} 