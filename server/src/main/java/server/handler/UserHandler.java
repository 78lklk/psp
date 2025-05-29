package server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.ApiResponse;
import common.model.User;
import common.model.Role;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import server.service.UserService;
import server.service.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.DefaultFullHttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обработчик запросов для управления пользователями
 */
public class UserHandler extends AbstractRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(UserHandler.class);
    private static final String URL_PATTERN = "/api/users.*";
    private static final Pattern ID_PATTERN = Pattern.compile("/api/users/(\\d+)");
    
    private final UserService userService;
    
    public UserHandler() {
        super();
        this.userService = new UserServiceImpl();
        logger.info("UserHandler initialized");
    }
    
    public UserHandler(UserService userService) {
        super();
        this.userService = userService;
        logger.info("UserHandler initialized with custom UserService");
    }
    
    @Override
    protected String getUrlPattern() {
        return URL_PATTERN;
    }
    
    @Override
    protected String getMethod() {
        return "GET|POST|PUT|DELETE";
    }
    
    @Override
    public boolean handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String method = request.method().name();
        
        logger.debug("UserHandler processing request: {} {}", method, uri);
        
        if (!uri.startsWith("/api/users")) {
            logger.debug("Not a user API request: {}", uri);
            return false;
        }
        
        try {
            if (uri.equals("/api/users") && method.equals(HttpMethod.GET.name())) {
                logger.debug("Handling GET all users request");
                // GET /api/users - get all users
                handleGetAllUsers(ctx);
                return true;
            } else if (uri.matches("/api/users") && method.equals(HttpMethod.POST.name())) {
                // POST /api/users - create new user
                handleCreateUser(ctx, request);
                return true;
            } else if (uri.matches("/api/users/\\d+") && method.equals(HttpMethod.GET.name())) {
                // GET /api/users/{id} - get user by ID
                handleGetUser(ctx, uri);
                return true;
            } else if (uri.matches("/api/users/\\d+") && method.equals(HttpMethod.PUT.name())) {
                // PUT /api/users/{id} - update user
                handleUpdateUser(ctx, uri, request);
                return true;
            } else if (uri.matches("/api/users/\\d+") && method.equals(HttpMethod.DELETE.name())) {
                // DELETE /api/users/{id} - delete user
                handleDeleteUser(ctx, uri);
                return true;
            } else if (uri.matches("/api/users/\\d+/password") && method.equals(HttpMethod.POST.name())) {
                // POST /api/users/{id}/password - change password
                handleChangePassword(ctx, request, uri);
                return true;
            } else if (uri.matches("/api/users/login/[^/]+") && method.equals(HttpMethod.GET.name())) {
                // GET /api/users/login/{login} - get user by login
                handleGetUserByLogin(ctx, uri);
                return true;
            }
            
            logger.debug("Request didn't match any user handler pattern: {} {}", method, uri);
            return false;
        } catch (Exception e) {
            logger.error("Error processing user request", e);
            try {
                ApiResponse<Object> response = ApiResponse.error("Error processing request: " + e.getMessage());
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonResponse);
            } catch (JsonProcessingException ex) {
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
            }
            return true;
        }
    }
    
    /**
     * Обрабатывает запрос на получение всех пользователей
     */
    private void handleGetAllUsers(ChannelHandlerContext ctx) throws JsonProcessingException {
        try {
            logger.debug("Getting all users from service");
            List<User> users = userService.getAllUsers();
            logger.debug("Retrieved {} users from database", users.size());
            
            ApiResponse<List<User>> response = ApiResponse.success(users);
            String jsonResponse = objectMapper.writeValueAsString(response);
            logger.debug("Sending response with {} users", users.size());
            sendSuccessResponse(ctx, jsonResponse);
        } catch (Exception e) {
            logger.error("Error retrieving all users", e);
            ApiResponse<Object> response = ApiResponse.error("Ошибка при получении списка пользователей: " + e.getMessage());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonResponse);
        }
    }
    
    /**
     * Обрабатывает запрос на получение пользователя по ID
     */
    private void handleGetUser(ChannelHandlerContext ctx, String uri) {
        Matcher matcher = ID_PATTERN.matcher(uri);
        if (matcher.find()) {
            String idStr = matcher.group(1);
            try {
                Long userId = Long.parseLong(idStr);
                handleGetUserById(ctx, userId);
            } catch (NumberFormatException e) {
                logger.error("Некорректный формат ID пользователя: {}", idStr);
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Некорректный формат ID пользователя");
            }
        } else {
            logger.error("Некорректный формат запроса: {}", uri);
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Некорректный формат запроса");
        }
    }
    
    /**
     * Обрабатывает запрос на создание пользователя
     */
    private void handleCreateUser(ChannelHandlerContext ctx, FullHttpRequest request) throws JsonProcessingException {
        try {
            String requestBody = getRequestContent(request);
            logger.debug("Received create user request body: {}", requestBody);
            
            User user = objectMapper.readValue(requestBody, User.class);
            
            logger.debug("Creating new user with login: {}", user.getLogin());
            
            // Validate required fields
            if (user.getLogin() == null || user.getPassword() == null) {
                logger.error("Missing required fields (login or password) for user creation");
                ApiResponse<String> errorResponse = ApiResponse.error("Login and password are required");
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, errorResponse);
                return;
            }
            
            // Process firstName and lastName
            if (user.getFullName() != null && !user.getFullName().isEmpty()) {
                // If fullName is provided directly, it will be parsed by the User class
                // to extract firstName and lastName automatically
                logger.debug("Setting fullName: {}", user.getFullName());
            } else {
                // Ensure first/last name are properly set if provided
                String firstName = user.getFirstName();
                String lastName = user.getLastName();
                if ((firstName != null && !firstName.isEmpty()) || 
                    (lastName != null && !lastName.isEmpty())) {
                    logger.debug("Setting firstName: {} and lastName: {}", firstName, lastName);
                }
            }
            
            // Handle user role
            if (user.getRole() == null) {
                Role defaultRole = new Role();
                defaultRole.setId(4L); // Default to CLIENT role
                defaultRole.setName("CLIENT");
                user.setRole(defaultRole);
                logger.debug("Setting default role: CLIENT");
            }
            
            // Set active status
            user.setActive(true);
            
            // Create user
            User createdUser = userService.createUser(user);
            if (createdUser != null && createdUser.getId() != null) {
                logger.debug("User created successfully with ID: {}", createdUser.getId());
                ApiResponse<User> response = ApiResponse.success(createdUser);
                String jsonResponse = objectMapper.writeValueAsString(response);
                
                FullHttpResponse httpResponse = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, 
                        HttpResponseStatus.CREATED,
                        Unpooled.copiedBuffer(jsonResponse, StandardCharsets.UTF_8));
                
                httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
                httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
                
                ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
                
                logger.debug("Sent success response with status 201 Created");
            } else {
                logger.error("Failed to create user");
                ApiResponse<String> errorResponse = ApiResponse.error("Failed to create user");
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorResponse);
            }
        } catch (Exception e) {
            logger.error("Error creating user", e);
            ApiResponse<String> errorResponse = ApiResponse.error("Error creating user: " + e.getMessage());
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorResponse);
        }
    }
    
    /**
     * Обрабатывает запрос на обновление пользователя
     */
    private void handleUpdateUser(ChannelHandlerContext ctx, String uri, FullHttpRequest request) throws JsonProcessingException {
        try {
            long userId = extractIdFromPath(uri);
            String requestBody = getRequestContent(request);
            User user = objectMapper.readValue(requestBody, User.class);
            
            // Ensure ID is set correctly
            user.setId(userId);
            
            logger.debug("Updating user with ID: {}", userId);
            
            // Check if user exists before updating
            Optional<User> existingUser = userService.getUserById(userId);
            if (existingUser.isEmpty()) {
                logger.error("Cannot update user with ID {} - user not found", userId);
                ApiResponse<Object> response = ApiResponse.error("Пользователь с ID " + userId + " не найден");
                String responseJson = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, responseJson);
                return;
            }
            
            // Process firstName and lastName
            if (user.getFullName() != null && !user.getFullName().isEmpty()) {
                // If fullName is provided directly, it will be parsed by the User class
                // to extract firstName and lastName automatically
                logger.debug("Setting fullName: {}", user.getFullName());
            } else {
                // Ensure first/last name are properly set if provided
                String firstName = user.getFirstName();
                String lastName = user.getLastName();
                if ((firstName != null && !firstName.isEmpty()) || 
                    (lastName != null && !lastName.isEmpty())) {
                    logger.debug("Setting firstName: {} and lastName: {}", firstName, lastName);
                }
            }
            
            // If login is changed, update the username field as well
            if (user.getLogin() != null && user.getUsername() == null) {
                user.setUsername(user.getLogin());
            } else if (user.getUsername() != null && user.getLogin() == null) {
                user.setLogin(user.getUsername());
            }
            
            // Update user
            boolean updated = userService.updateUser(user);
            if (updated) {
                logger.debug("User updated successfully");
                Optional<User> updatedUserOpt = userService.getUserById(userId);
                if (updatedUserOpt.isPresent()) {
                    ApiResponse<User> response = ApiResponse.success(updatedUserOpt.get());
                    String jsonResponse = objectMapper.writeValueAsString(response);
                    sendSuccessResponse(ctx, jsonResponse, StandardCharsets.UTF_8);
                } else {
                    logger.error("Updated user not found with ID: {}", userId);
                    ApiResponse<String> errorResponse = ApiResponse.error("User updated but could not be retrieved");
                    sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorResponse);
                }
            } else {
                logger.error("Failed to update user with ID: {}", userId);
                ApiResponse<String> errorResponse = ApiResponse.error("Failed to update user. Username/login may already be in use.");
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, errorResponse);
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid user ID format in path: {}", uri);
            ApiResponse<String> errorResponse = ApiResponse.error("Invalid user ID format");
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, errorResponse);
        } catch (Exception e) {
            logger.error("Error updating user", e);
            ApiResponse<String> errorResponse = ApiResponse.error("Error updating user: " + e.getMessage());
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorResponse);
        }
    }
    
    /**
     * Обрабатывает запрос на удаление пользователя
     */
    private void handleDeleteUser(ChannelHandlerContext ctx, String uri) {
        Matcher matcher = ID_PATTERN.matcher(uri);
        if (matcher.find()) {
            String idStr = matcher.group(1);
            try {
                Long userId = Long.parseLong(idStr);
                handleDeleteUser(ctx, userId);
            } catch (NumberFormatException e) {
                logger.error("Некорректный формат ID пользователя: {}", idStr);
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Некорректный формат ID пользователя");
            }
        } else {
            logger.error("Некорректный формат запроса: {}", uri);
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Некорректный формат запроса");
        }
    }
    
    /**
     * Обрабатывает запрос на получение пользователя по ID
     */
    private boolean handleGetUserById(ChannelHandlerContext ctx, Long userId) {
        try {
            Optional<User> userOptional = userService.getUserById(userId);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                // Скрываем пароль перед отправкой клиенту
                user.setPassword(null);
                
                ApiResponse<User> response = ApiResponse.success(user);
                String responseJson = objectMapper.writeValueAsString(response);
                
                sendSuccessResponse(ctx, responseJson);
            } else {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Пользователь не найден");
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при получении пользователя с ID: {}", userId, e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            return true;
        }
    }
    
    /**
     * Обрабатывает запрос на удаление пользователя
     */
    private boolean handleDeleteUser(ChannelHandlerContext ctx, Long userId) {
        try {
            logger.debug("Handling delete user request for userId: {}", userId);
            
            // Check if user exists before attempting to delete
            Optional<User> userOptional = userService.getUserById(userId);
            if (userOptional.isEmpty()) {
                logger.error("Cannot delete user with ID {} - user not found", userId);
                ApiResponse<Object> response = ApiResponse.error("Пользователь с ID " + userId + " не найден");
                String responseJson = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, responseJson);
                return true;
            }
            
            boolean success = userService.deleteUser(userId);
            
            if (success) {
                logger.info("User with ID {} successfully deleted", userId);
                ApiResponse<String> response = ApiResponse.success("Пользователь успешно удален");
                String responseJson = objectMapper.writeValueAsString(response);
                sendSuccessResponse(ctx, responseJson);
            } else {
                logger.error("Failed to delete user with ID {}", userId);
                ApiResponse<Object> response = ApiResponse.error("Не удалось удалить пользователя");
                String responseJson = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, responseJson);
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Error while deleting user with ID: {}", userId, e);
            try {
                ApiResponse<Object> response = ApiResponse.error("Ошибка при удалении пользователя: " + e.getMessage());
                String responseJson = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, responseJson);
            } catch (JsonProcessingException ex) {
                logger.error("Error creating error response JSON", ex);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
            }
            return true;
        }
    }

    /**
     * Handles password change for a user
     */
    private void handleChangePassword(ChannelHandlerContext ctx, FullHttpRequest request, String uri) throws JsonProcessingException {
        try {
            long userId = extractIdFromPath(uri.replace("/password", ""));
            String requestBody = getRequestContent(request);
            Map<String, String> passwordMap = objectMapper.readValue(requestBody, new TypeReference<Map<String, String>>() {});
            
            String oldPassword = passwordMap.get("oldPassword");
            String newPassword = passwordMap.get("newPassword");
            
            if (oldPassword == null || newPassword == null) {
                logger.error("Missing required fields for password change");
                ApiResponse<String> errorResponse = ApiResponse.error("Old password and new password are required");
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, errorResponse);
                return;
            }
            
            boolean changed = userService.changePassword(userId, oldPassword, newPassword);
            
            if (changed) {
                logger.info("Password changed successfully for user ID: {}", userId);
                ApiResponse<String> response = ApiResponse.success("Password changed successfully");
                sendSuccessResponse(ctx, response);
            } else {
                logger.error("Failed to change password for user ID: {}", userId);
                ApiResponse<String> errorResponse = ApiResponse.error("Failed to change password. Check that the old password is correct.");
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, errorResponse);
            }
        } catch (Exception e) {
            logger.error("Error changing password", e);
            ApiResponse<String> errorResponse = ApiResponse.error("Error changing password: " + e.getMessage());
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorResponse);
        }
    }

    /**
     * Handles request to get user by login
     */
    private void handleGetUserByLogin(ChannelHandlerContext ctx, String uri) {
        try {
            // Extract login from the URI
            // The pattern is /api/users/login/{login}
            Pattern loginPattern = Pattern.compile("/api/users/login/([^/]+)");
            Matcher matcher = loginPattern.matcher(uri);
            
            if (matcher.find()) {
                String login = matcher.group(1);
                logger.debug("Getting user by login: {}", login);
                
                Optional<User> userOptional = userService.getUserByLogin(login);
                
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    // Hide password before sending to client
                    user.setPassword(null);
                    
                    ApiResponse<User> response = ApiResponse.success(user);
                    String responseJson = objectMapper.writeValueAsString(response);
                    
                    sendSuccessResponse(ctx, responseJson);
                } else {
                    logger.debug("User not found with login: {}", login);
                    ApiResponse<String> errorResponse = ApiResponse.error("User not found");
                    sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, errorResponse);
                }
            } else {
                logger.error("Invalid format for get user by login: {}", uri);
                ApiResponse<String> errorResponse = ApiResponse.error("Invalid request format");
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, errorResponse);
            }
        } catch (Exception e) {
            logger.error("Error getting user by login", e);
            try {
                ApiResponse<String> errorResponse = ApiResponse.error("Error getting user: " + e.getMessage());
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, errorResponse);
            } catch (Exception ex) {
                logger.error("Error creating error response", ex);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
            }
        }
    }

    // Helper method to extract ID from path
    private long extractIdFromPath(String path) {
        Matcher matcher = ID_PATTERN.matcher(path);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        throw new NumberFormatException("Invalid path format: " + path);
    }
} 