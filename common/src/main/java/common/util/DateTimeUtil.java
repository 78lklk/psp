package common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Утилиты для работы с датами и временем
 */
public class DateTimeUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final DateTimeFormatter FILE_NAME_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * Форматирует дату в строку
     * @param date дата
     * @return строка с датой в формате "DD.MM.YYYY"
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return DATE_FORMATTER.format(date);
    }
    
    /**
     * Форматирует дату и время в строку
     * @param dateTime дата и время
     * @return строка с датой и временем в формате "DD.MM.YYYY HH:MM:SS"
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return DATE_TIME_FORMATTER.format(dateTime);
    }
    
    /**
     * Форматирует дату и время для имени файла
     * @param dateTime дата и время
     * @return строка с датой и временем в формате "YYYYMMDD_HHMMSS"
     */
    public static String formatDateTimeForFileName(LocalDateTime dateTime) {
        if (dateTime == null) {
            dateTime = LocalDateTime.now();
        }
        return FILE_NAME_DATE_FORMATTER.format(dateTime);
    }
    
    /**
     * Парсит строку в дату
     * @param dateStr строка с датой в формате "DD.MM.YYYY"
     * @return объект LocalDate
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
    
    /**
     * Парсит строку в дату и время
     * @param dateTimeStr строка с датой и временем в формате "DD.MM.YYYY HH:MM:SS"
     * @return объект LocalDateTime
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
    }
} 