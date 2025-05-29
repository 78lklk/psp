package server.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.ApiResponse;
import common.model.Backup;
import common.model.User;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.service.BackupService;
import server.service.BackupServiceImpl;
import server.service.UserService;
import server.service.UserServiceImpl;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обработчик запросов для резервных копий
 */
public class BackupHandler extends AbstractRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(BackupHandler.class);
    private static final Pattern ID_PATTERN = Pattern.compile("/api/backup/(\\d+)");
    private static final Pattern RESTORE_PATTERN = Pattern.compile("/api/backup/(\\d+)/restore");
    
    private final BackupService backupService;
    private final UserService userService;
    
    public BackupHandler() {
        super();
        this.backupService = new BackupServiceImpl();
        this.userService = new UserServiceImpl();
    }
    
    public BackupHandler(BackupService backupService, UserService userService) {
        super();
        this.backupService = backupService;
        this.userService = userService;
    }
    
    @Override
    protected String getUrlPattern() {
        return "/api/backup.*";
    }
    
    @Override
    protected String getMethod() {
        return "GET|POST|PUT|DELETE";
    }
    
    @Override
    public boolean handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String method = request.method().name();
        
        logger.debug("BackupHandler processing: {} {}", method, uri);
        
        if (!uri.startsWith("/api/backup")) {
            return false;
        }
        
        try {
            // GET /api/backup - get all backups
            if (uri.equals("/api/backup") && method.equals(HttpMethod.GET.name())) {
                handleGetAllBackups(ctx);
                return true;
            }
            
            // POST /api/backup - create new backup
            if (uri.equals("/api/backup") && method.equals(HttpMethod.POST.name())) {
                handleCreateBackup(ctx, request);
                return true;
            }
            
            // GET /api/backup/info - get backup directory info
            if (uri.equals("/api/backup/info") && method.equals(HttpMethod.GET.name())) {
                handleGetBackupInfo(ctx);
                return true;
            }
            
            // GET /api/backup/{id} - get backup by id
            Matcher idMatcher = ID_PATTERN.matcher(uri);
            if (idMatcher.matches() && method.equals(HttpMethod.GET.name())) {
                Long id = Long.parseLong(idMatcher.group(1));
                handleGetBackupById(ctx, id);
                return true;
            }
            
            // DELETE /api/backup/{id} - delete backup
            if (idMatcher.matches() && method.equals(HttpMethod.DELETE.name())) {
                Long id = Long.parseLong(idMatcher.group(1));
                handleDeleteBackup(ctx, id);
                return true;
            }
            
            // POST /api/backup/{id}/restore - restore from backup
            Matcher restoreMatcher = RESTORE_PATTERN.matcher(uri);
            if (restoreMatcher.matches() && method.equals(HttpMethod.POST.name())) {
                Long id = Long.parseLong(restoreMatcher.group(1));
                handleRestoreBackup(ctx, id, request);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error processing backup request", e);
            try {
                ApiResponse<String> response = ApiResponse.error("Error processing request: " + e.getMessage());
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, response);
            } catch (Exception ex) {
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
            }
            return true;
        }
    }
    
    private void handleGetAllBackups(ChannelHandlerContext ctx) throws JsonProcessingException {
        logger.debug("Getting all backups");
        
        List<Backup> backups = backupService.getAllBackups();
        ApiResponse<List<Backup>> response = ApiResponse.success(backups);
        
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
    
    private void handleGetBackupById(ChannelHandlerContext ctx, Long id) throws JsonProcessingException {
        logger.debug("Getting backup by id: {}", id);
        
        Optional<Backup> backupOpt = backupService.getBackupById(id);
        
        if (backupOpt.isPresent()) {
            ApiResponse<Backup> response = ApiResponse.success(backupOpt.get());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } else {
            ApiResponse<String> response = ApiResponse.error("Backup not found");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
        }
    }
    
    private void handleCreateBackup(ChannelHandlerContext ctx, FullHttpRequest request) throws JsonProcessingException {
        logger.debug("Creating new backup");
        
        // Default values
        Long userId = 1L;
        String description = "Automatic backup";
        
        // Try to get values from request body if present
        try {
            String content = request.content().toString(CharsetUtil.UTF_8);
            logger.debug("Request body: {}", content);
            
            if (content != null && !content.isEmpty() && !content.equals("{}")) {
                Map<String, Object> requestBody = objectMapper.readValue(content, HashMap.class);
                
                if (requestBody != null) {
                    if (requestBody.containsKey("userId") && requestBody.get("userId") != null) {
                        userId = ((Number) requestBody.get("userId")).longValue();
                    }
                    
                    if (requestBody.containsKey("description") && requestBody.get("description") != null) {
                        description = (String) requestBody.get("description");
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse request body, using default values. Error: {}", e.getMessage());
            // Continue with default values
        }
        
        // Get user from database
        Optional<User> userOpt = userService.getUserById(userId);
        User user;
        
        if (userOpt.isEmpty()) {
            logger.warn("User with ID {} not found, using default admin user", userId);
            // Get first admin user as fallback
            userOpt = userService.getUserByRole("ADMIN");
            if (userOpt.isEmpty()) {
                ApiResponse<String> response = ApiResponse.error("No admin user found in system");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonResponse);
                return;
            }
        }
        
        user = userOpt.get();
        
        // Create backup
        try {
            Backup backup = backupService.createBackup(user, description);
            
            if (backup != null) {
                ApiResponse<Backup> response = ApiResponse.success(backup);
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendSuccessResponse(ctx, jsonResponse);
            } else {
                ApiResponse<String> response = ApiResponse.error("Failed to create backup");
                String jsonResponse = objectMapper.writeValueAsString(response);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonResponse);
            }
        } catch (Exception e) {
            logger.error("Error creating backup", e);
            ApiResponse<String> response = ApiResponse.error("Error creating backup: " + e.getMessage());
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonResponse);
        }
    }
    
    private void handleDeleteBackup(ChannelHandlerContext ctx, Long id) throws JsonProcessingException {
        logger.debug("Deleting backup with id: {}", id);
        
        // Check if backup exists
        Optional<Backup> backupOpt = backupService.getBackupById(id);
        
        if (backupOpt.isEmpty()) {
            ApiResponse<String> response = ApiResponse.error("Backup not found");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
            return;
        }
        
        // Delete backup
        boolean deleted = backupService.deleteBackup(id);
        
        if (deleted) {
            ApiResponse<String> response = ApiResponse.success("Backup deleted successfully");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } else {
            ApiResponse<String> response = ApiResponse.error("Failed to delete backup");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonResponse);
        }
    }
    
    private void handleRestoreBackup(ChannelHandlerContext ctx, Long id, FullHttpRequest request) throws JsonProcessingException {
        logger.debug("Restoring from backup with id: {}", id);
        
        // Check if backup exists
        Optional<Backup> backupOpt = backupService.getBackupById(id);
        
        if (backupOpt.isEmpty()) {
            ApiResponse<String> response = ApiResponse.error("Backup not found");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, jsonResponse);
            return;
        }
        
        // Get user ID from request
        String content = request.content().toString(CharsetUtil.UTF_8);
        Map<String, Object> requestBody = new HashMap<>();
        
        if (content != null && !content.isEmpty()) {
            requestBody = objectMapper.readValue(content, HashMap.class);
        }
        
        Long userId = ((Number) requestBody.getOrDefault("userId", 1L)).longValue();
        
        // Get user from database
        Optional<User> userOpt = userService.getUserById(userId);
        if (userOpt.isEmpty()) {
            ApiResponse<String> response = ApiResponse.error("User not found");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, jsonResponse);
            return;
        }
        
        User user = userOpt.get();
        
        // Restore from backup
        boolean restored = backupService.restoreFromBackup(id, user);
        
        if (restored) {
            ApiResponse<String> response = ApiResponse.success("System restored successfully from backup");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendSuccessResponse(ctx, jsonResponse);
        } else {
            ApiResponse<String> response = ApiResponse.error("Failed to restore from backup");
            String jsonResponse = objectMapper.writeValueAsString(response);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonResponse);
        }
    }
    
    private void handleGetBackupInfo(ChannelHandlerContext ctx) throws JsonProcessingException {
        logger.debug("Getting backup directory info");
        
        BackupService.BackupDirectoryInfo info = backupService.getBackupDirectoryInfo();
        ApiResponse<BackupService.BackupDirectoryInfo> response = ApiResponse.success(info);
        
        String jsonResponse = objectMapper.writeValueAsString(response);
        sendSuccessResponse(ctx, jsonResponse);
    }
} 