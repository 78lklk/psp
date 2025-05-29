package server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Интерфейс для обработчиков HTTP запросов
 */
public interface RequestHandler {
    /**
     * Обрабатывает HTTP запрос
     * @param ctx контекст канала
     * @param request HTTP запрос
     * @return true, если запрос обработан, false - если не может быть обработан этим обработчиком
     */
    boolean handle(ChannelHandlerContext ctx, FullHttpRequest request);
    
    /**
     * Проверяет, может ли данный обработчик обработать указанный запрос
     * @param path путь запроса
     * @param method HTTP метод
     * @return true, если может обработать, иначе false
     */
    boolean canHandle(String path, String method);
    
    /**
     * Отправляет ответ с ошибкой
     * @param ctx контекст канала
     * @param status статус HTTP ответа
     * @param message сообщение об ошибке
     */
    void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message);
    
    /**
     * Отправляет успешный ответ
     * @param ctx контекст канала
     * @param content содержимое ответа
     */
    void sendSuccessResponse(ChannelHandlerContext ctx, String content);
} 