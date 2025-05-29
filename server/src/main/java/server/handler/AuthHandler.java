package server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import common.dto.AuthRequest;
import common.dto.AuthResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import server.service.UserService;
import server.service.UserServiceImpl;

/**
 * Обработчик запросов на аутентификацию
 */
public class AuthHandler extends AbstractRequestHandler {
    private static final String URL_PATTERN = "/api/auth";
    private final UserService userService;
    private final ObjectMapper authObjectMapper;
    
    public AuthHandler() {
        super();
        this.userService = new UserServiceImpl();
        this.authObjectMapper = createObjectMapper();
    }
    
    public AuthHandler(UserService userService) {
        super();
        this.userService = userService;
        this.authObjectMapper = createObjectMapper();
    }
    
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Override
    public boolean handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String method = request.method().name();
        
        if (!uri.startsWith("/api/auth")) {
            return false;
        }
        
        logger.debug("Обработка запроса на аутентификацию");
        
        try {
            String content = getRequestContent(request);
            AuthRequest authRequest = authObjectMapper.readValue(content, AuthRequest.class);
            
            logger.debug("Запрос на аутентификацию пользователя: {}", authRequest.getLogin());
            
            AuthResponse authResponse = userService.authenticate(authRequest);
            String jsonResponse = authObjectMapper.writeValueAsString(authResponse);
            
            if (authResponse.isSuccess()) {
                sendSuccessResponse(ctx, jsonResponse);
                logger.info("Пользователь {} успешно аутентифицирован", authRequest.getLogin());
            } else {
                sendErrorResponse(ctx, HttpResponseStatus.UNAUTHORIZED, authResponse.getErrorMessage());
                logger.warn("Ошибка аутентификации пользователя {}: {}", 
                        authRequest.getLogin(), authResponse.getErrorMessage());
            }
            
            return true;
        } catch (JsonProcessingException e) {
            logger.error("Ошибка при обработке JSON", e);
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Неверный формат запроса");
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при обработке запроса аутентификации", e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
    }

    @Override
    protected String getUrlPattern() {
        return URL_PATTERN;
    }

    @Override
    protected String getMethod() {
        return HttpMethod.POST.name();
    }
} 