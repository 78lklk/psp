package server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Обработчик HTTP запросов для сервера
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(HttpServerHandler.class);
    private final List<RequestHandler> handlers = new ArrayList<>();
    
    public HttpServerHandler() {
        // Регистрируем обработчики запросов
        handlers.add(new AuthHandler());
        handlers.add(new CardHandler());
        handlers.add(new TierHandler());
        handlers.add(new SessionHandler());
        handlers.add(new TransactionHandler());
        // Добавляем необходимые обработчики
        handlers.add(new SettingsHandler());
        handlers.add(new UserHandler());
        handlers.add(new PromotionHandler());
        handlers.add(new AuditHandler());
        // Восстанавливаем удаленные обработчики
        handlers.add(new PromoCodeHandler());
        // Добавляем StatisticsHandler для обработки запросов статистики
        handlers.add(new StatisticsHandler());
        // Добавляем ReportHandler для обработки отчетов
        handlers.add(new ReportHandler());
        // Добавляем ScheduleHandler для работы с расписанием
        handlers.add(new ScheduleHandler());
        // Удаляем BackupHandler по требованию
        // handlers.add(new BackupHandler());
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        logger.debug("Получен запрос: {} {}", request.method(), request.uri());
        
        try {
            boolean handled = false;
            
            // Пытаемся найти подходящий обработчик
            for (RequestHandler handler : handlers) {
                if (handler.handle(ctx, request)) {
                    handled = true;
                    break;
                }
            }
            
            // Если ни один обработчик не подошел
            if (!handled) {
                logger.warn("Не найден обработчик для запроса: {} {}", request.method(), request.uri());
                sendErrorResponse(ctx, request, HttpResponseStatus.NOT_FOUND, "Ресурс не найден");
            }
        } catch (Exception e) {
            logger.error("Ошибка при обработке запроса", e);
            sendErrorResponse(ctx, request, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
        }
    }
    
    private void sendErrorResponse(ChannelHandlerContext ctx, FullHttpRequest request, 
                                  HttpResponseStatus status, String message) {
        // Используем первый обработчик для отправки ответа с ошибкой
        if (!handlers.isEmpty()) {
            handlers.get(0).sendErrorResponse(ctx, status, message);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Ошибка в обработчике канала", cause);
        if (ctx.channel().isActive()) {
            sendErrorResponse(ctx, null, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
        }
    }
} 