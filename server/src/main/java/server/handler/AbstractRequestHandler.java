package server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import common.dto.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Abstract class for HTTP request handlers
 */
public abstract class AbstractRequestHandler implements RequestHandler {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper objectMapper;
    
    public AbstractRequestHandler() {
        this.objectMapper = createObjectMapper();
    }
    
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Ensure proper UTF-8 handling
        mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        mapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET, true);
        return mapper;
    }
    
    @Override
    public boolean canHandle(String path, String method) {
        String urlPattern = getUrlPattern();
        String methodPattern = getMethod();
        
        boolean pathMatches = Pattern.matches(urlPattern, path);
        boolean methodMatches = Pattern.matches(methodPattern, method);
        
        return pathMatches && methodMatches;
    }
    
    /**
     * Handles HTTP request
     */
    @Override
    public abstract boolean handle(ChannelHandlerContext ctx, FullHttpRequest request);
    
    /**
     * Gets request content as string
     * @param request HTTP request
     * @return request content
     */
    protected String getRequestContent(FullHttpRequest request) {
        return request.content().toString(StandardCharsets.UTF_8);
    }
    
    @Override
    public void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        try {
            // Format JSON response with error message
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("success", false);
            responseMap.put("message", message);
            
            String jsonResponse = objectMapper.writeValueAsString(responseMap);
            
            FullHttpResponse httpResponse = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, 
                    status,
                    Unpooled.copiedBuffer(jsonResponse, StandardCharsets.UTF_8));
            
            httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
            
            ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
            
            logger.debug("Sent error response: {}, {}", status, message);
        } catch (Exception e) {
            logger.error("Error while sending error response", e);
        }
    }
    
    /**
     * Sends error response using ApiResponse object
     * @param ctx channel context
     * @param status HTTP status
     * @param errorResponse error response object
     */
    public <T> void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, ApiResponse<T> errorResponse) {
        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            
            FullHttpResponse httpResponse = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, 
                    status,
                    Unpooled.copiedBuffer(jsonResponse, StandardCharsets.UTF_8));
            
            httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
            
            ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
            
            logger.debug("Sent error response: {}, {}", status, errorResponse.getErrorMessage());
        } catch (Exception e) {
            logger.error("Error while sending error response", e);
        }
    }
    
    @Override
    public void sendSuccessResponse(ChannelHandlerContext ctx, String content) {
        try {
            FullHttpResponse httpResponse = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, 
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(content, StandardCharsets.UTF_8));
            
            httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
            
            ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
            
            logger.debug("Sent success response");
        } catch (Exception e) {
            logger.error("Error while sending success response", e);
        }
    }
    
    /**
     * Sends success response using ApiResponse object
     * @param ctx channel context
     * @param response success response object
     */
    public <T> void sendSuccessResponse(ChannelHandlerContext ctx, ApiResponse<T> response) {
        try {
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } catch (Exception e) {
            logger.error("Error while sending success response", e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    /**
     * Отправляет успешный ответ с указанной кодировкой
     * @param ctx контекст канала
     * @param content содержимое ответа
     * @param charset кодировка ответа
     */
    public void sendSuccessResponse(ChannelHandlerContext ctx, String content, java.nio.charset.Charset charset) {
        try {
            FullHttpResponse httpResponse = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, 
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(content, charset));
            
            httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=" + charset.name());
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
            
            ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
            
            logger.debug("Sent success response with charset: {}", charset.name());
        } catch (Exception e) {
            logger.error("Error while sending success response", e);
        }
    }
    
    /**
     * Gets URL pattern that this handler processes
     * @return regular expression for URL
     */
    protected abstract String getUrlPattern();
    
    /**
     * Gets HTTP method that this handler processes
     * @return HTTP method (GET, POST, PUT, DELETE etc.)
     */
    protected abstract String getMethod();
} 