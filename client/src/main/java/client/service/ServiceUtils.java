package client.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import common.util.Constants;

/**
 * Утилитарный класс для сервисных классов
 */
public class ServiceUtils {
    /**
     * ObjectMapper для JSON-сериализации/десериализации
     */
    public static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
    
    /**
     * Приватный конструктор для запрета инстанцирования
     */
    private ServiceUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Создает экземпляр ObjectMapper с настройками
     * @return настроенный ObjectMapper
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // Add support for proper UTF-8 encoding
        mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        mapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET, true);
        // Add JavaTimeModule for proper date handling
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return mapper;
    }
    
    /**
     * Возвращает базовый URL API
     * @return базовый URL API
     */
    public static String getApiUrl() {
        return "http://" + Constants.SERVER_HOST + ":" + Constants.SERVER_PORT + "/api";
    }
    
    /**
     * Проверяет, доступен ли сервер
     * @return true, если сервер доступен
     */
    public static boolean isServerAvailable() {
        try {
            java.net.Socket socket = new java.net.Socket();
            java.net.InetSocketAddress address = new java.net.InetSocketAddress(
                    Constants.SERVER_HOST, Constants.SERVER_PORT);
            socket.connect(address, 3000); // 3 секунды таймаут
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Логирует детали ответа сервера для отладки
     * @param statusCode HTTP статус код
     * @param responseBody тело ответа
     * @param logger логгер для вывода информации
     */
    public static void logResponseDetails(int statusCode, String responseBody, org.slf4j.Logger logger) {
        if (statusCode >= 400) {
            logger.error("Ошибка сервера: {} - {}", statusCode, getStatusCodeDescription(statusCode));
            logger.error("Тело ответа: {}", responseBody);
        } else {
            logger.debug("Статус ответа: {} - {}", statusCode, getStatusCodeDescription(statusCode));
            logger.debug("Тело ответа: {}", responseBody);
        }
    }
    
    /**
     * Возвращает описание HTTP статус кода
     * @param statusCode HTTP статус код
     * @return описание статус кода
     */
    public static String getStatusCodeDescription(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 409: return "Conflict";
            case 500: return "Internal Server Error";
            case 503: return "Service Unavailable";
            default: return "Unknown Status";
        }
    }
    
    /**
     * Возвращает читаемое сообщение об ошибке на основе статус кода
     * @param statusCode HTTP статус код
     * @return сообщение об ошибке
     */
    public static String getErrorMessage(int statusCode) {
        switch (statusCode) {
            case 400: return "Неверный запрос. Проверьте правильность введенных данных.";
            case 401: return "Требуется авторизация. Пожалуйста, войдите в систему.";
            case 403: return "Доступ запрещен. У вас нет прав для выполнения этой операции.";
            case 404: return "Запрашиваемый ресурс не найден.";
            case 409: return "Конфликт. Действие невозможно выполнить из-за конфликта в текущем состоянии.";
            case 500: return "Внутренняя ошибка сервера. Пожалуйста, попробуйте позже.";
            case 503: return "Сервис временно недоступен. Пожалуйста, попробуйте позже.";
            default: return "Ошибка соединения с сервером. Код: " + statusCode;
        }
    }
} 