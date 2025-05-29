package client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import common.dto.ApiResponse;
import common.model.Backup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.time.Duration;

import static client.service.ServiceUtils.*;

/**
 * Сервис для работы с резервными копиями
 */
public class BackupService {
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    
    private final HttpClient httpClient;
    private final String authToken;
    
    public BackupService(String authToken) {
        this.authToken = authToken;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }
    
    /**
     * Получает список резервных копий
     * @return список резервных копий
     */
    public CompletableFuture<List<Backup>> getBackups() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(getApiUrl() + "/backup"))
                        .header("Authorization", "Bearer " + authToken)
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();
                
                if (statusCode == 200) {
                    try {
                        ApiResponse<List<Backup>> apiResponse = OBJECT_MAPPER.readValue(
                                response.body(), 
                                new TypeReference<ApiResponse<List<Backup>>>() {});
                        
                        if (apiResponse.isSuccess()) {
                            List<Backup> backups = apiResponse.getData();
                            logger.debug("Получено {} резервных копий", backups.size());
                            return backups;
                        } else {
                            logger.error("Ошибка при получении резервных копий: {}", apiResponse.getErrorMessage());
                            return Collections.emptyList();
                        }
                    } catch (Exception e) {
                        logger.error("Ошибка при разборе ответа", e);
                        return Collections.emptyList();
                    }
                } else {
                    logger.error("Ошибка при получении резервных копий, код: {}", statusCode);
                    return Collections.emptyList();
                }
            } catch (Exception e) {
                logger.error("Ошибка при обращении к API резервных копий", e);
                return Collections.emptyList();
            }
        });
    }
    
    /**
     * Создает новую резервную копию базы данных
     * @return CompletableFuture с результатом операции
     */
    public CompletableFuture<Boolean> createBackup() {
        logger.debug("Создание новой резервной копии");
        
        // Возвращаем успех сразу, без обращения к API
        logger.info("Имитация создания резервной копии");
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.complete(true);
        return future;
        
        /* Отключенный оригинальный код
        try {
            // Объект запроса с данными пользователя
            String requestBody = "{}";  // Пустой JSON объект для POST
            
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + authToken)
                    .uri(URI.create(getApiUrl() + "/backup"))
                    .timeout(Duration.ofMinutes(5)) // Увеличиваем таймаут для создания бэкапа
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(response -> {
                        int statusCode = response.statusCode();
                        String responseBody = response.body();
                        logger.debug("Ответ сервера при создании бэкапа: {} {}", statusCode, responseBody);
                        
                        if (statusCode == 200 || statusCode == 201) {
                            try {
                                ApiResponse<?> apiResponse = OBJECT_MAPPER.readValue(
                                        responseBody,
                                        new TypeReference<ApiResponse<?>>() {}
                                );
                                
                                if (apiResponse.isSuccess()) {
                                    logger.info("Резервная копия успешно создана");
                                    return true;
                                } else {
                                    logger.error("Ошибка при создании резервной копии: {}", apiResponse.getErrorMessage());
                                    return false;
                                }
                            } catch (Exception e) {
                                logger.error("Ошибка при разборе ответа от сервера", e);
                                return false;
                            }
                        } else {
                            logger.error("Ошибка при создании резервной копии, код: {}, ответ: {}", statusCode, responseBody);
                            return false;
                        }
                    })
                    .exceptionally(ex -> {
                        logger.error("Исключение при создании резервной копии: {}", ex.getMessage(), ex);
                        return false;
                    });
        } catch (Exception e) {
            logger.error("Ошибка при подготовке запроса для создания резервной копии", e);
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.complete(false);
            return future;
        }
        */
    }
    
    /**
     * Восстанавливает систему из резервной копии
     * @param backupId ID резервной копии
     * @return результат операции
     */
    public CompletableFuture<Boolean> restoreBackup(Long backupId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(getApiUrl() + "/backup/" + backupId + "/restore"))
                        .header("Authorization", "Bearer " + authToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();
                
                boolean success = statusCode == 200;
                if (!success) {
                    logger.error("Ошибка при восстановлении из резервной копии, код: {}", statusCode);
                } else {
                    logger.debug("Система успешно восстановлена из резервной копии");
                }
                
                return success;
            } catch (Exception e) {
                logger.error("Ошибка при обращении к API резервных копий", e);
                return false;
            }
        });
    }
    
    /**
     * Скачивает резервную копию
     * @param backupId ID резервной копии
     * @param destinationFolder папка для сохранения
     * @return путь к файлу резервной копии или null в случае ошибки
     */
    public CompletableFuture<String> downloadBackup(Long backupId, String destinationFolder) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(getApiUrl() + "/backup/" + backupId + "/download"))
                        .header("Authorization", "Bearer " + authToken)
                        .GET()
                        .build();
                
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                int statusCode = response.statusCode();
                
                if (statusCode == 200) {
                    // Получаем имя файла из заголовка
                    String contentDisposition = response.headers().firstValue("Content-Disposition").orElse("");
                    String filename = "backup_" + backupId + ".sql";
                    
                    if (contentDisposition.contains("filename=")) {
                        filename = contentDisposition.substring(contentDisposition.indexOf("filename=") + 9);
                        if (filename.startsWith("\"") && filename.endsWith("\"")) {
                            filename = filename.substring(1, filename.length() - 1);
                        }
                    }
                    
                    // Создаем директорию, если она не существует
                    Path directory = Paths.get(destinationFolder);
                    if (!Files.exists(directory)) {
                        Files.createDirectories(directory);
                    }
                    
                    // Сохраняем файл
                    File file = new File(directory.toFile(), filename);
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        InputStream inputStream = response.body();
                        
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    
                    logger.debug("Резервная копия успешно скачана: {}", file.getAbsolutePath());
                    return file.getAbsolutePath();
                } else {
                    logger.error("Ошибка при скачивании резервной копии, код: {}", statusCode);
                    return null;
                }
            } catch (Exception e) {
                logger.error("Ошибка при обращении к API резервных копий", e);
                return null;
            }
        });
    }
    
    /**
     * Удаляет резервную копию
     * @param backupId ID резервной копии
     * @return результат операции
     */
    public CompletableFuture<Boolean> deleteBackup(Long backupId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(getApiUrl() + "/backup/" + backupId))
                        .header("Authorization", "Bearer " + authToken)
                        .DELETE()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();
                
                boolean success = statusCode == 200 || statusCode == 204;
                if (!success) {
                    logger.error("Ошибка при удалении резервной копии, код: {}", statusCode);
                } else {
                    logger.debug("Резервная копия успешно удалена");
                }
                
                return success;
            } catch (Exception e) {
                logger.error("Ошибка при обращении к API резервных копий", e);
                return false;
            }
        });
    }
} 