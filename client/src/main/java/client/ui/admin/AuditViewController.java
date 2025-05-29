package client.ui.admin;

import client.service.AuditService;
import client.service.UserService;
import common.model.AuditLog;
import common.model.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Контроллер для просмотра журнала аудита
 */
public class AuditViewController {
    private static final Logger logger = LoggerFactory.getLogger(AuditViewController.class);
    
    @FXML
    private TableView<AuditLog> auditTable;
    
    @FXML
    private TableColumn<AuditLog, Long> idColumn;
    
    @FXML
    private TableColumn<AuditLog, String> userColumn;
    
    @FXML
    private TableColumn<AuditLog, String> actionTypeColumn;
    
    @FXML
    private TableColumn<AuditLog, String> actionDetailsColumn;
    
    @FXML
    private TableColumn<AuditLog, String> timestampColumn;
    
    @FXML
    private TableColumn<AuditLog, String> ipAddressColumn;
    
    @FXML
    private DatePicker fromDatePicker;
    
    @FXML
    private DatePicker toDatePicker;
    
    @FXML
    private ComboBox<User> userFilterComboBox;
    
    @FXML
    private ComboBox<String> actionTypeFilterComboBox;
    
    @FXML
    private Button applyFiltersButton;
    
    @FXML
    private Button resetFiltersButton;
    
    @FXML
    private Button exportButton;
    
    @FXML
    private Label statusLabel;
    
    private AuditService auditService;
    private UserService userService;
    private ObservableList<AuditLog> auditLogsList = FXCollections.observableArrayList();
    private ObservableList<User> usersList = FXCollections.observableArrayList();
    private ObservableList<String> actionTypesList = FXCollections.observableArrayList();
    private String authToken;
    
    /**
     * Устанавливает токен авторизации и инициализирует сервисы
     * @param authToken токен авторизации
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
        auditService = new AuditService(authToken);
        userService = new UserService(authToken);
        
        // Устанавливаем текущую дату в фильтры
        fromDatePicker.setValue(LocalDate.now().minusWeeks(1));
        toDatePicker.setValue(LocalDate.now());
        
        loadAuditLogs();
        loadUsers();
    }
    
    /**
     * Инициализирует контроллер
     */
    @FXML
    private void initialize() {
        // Настройка таблицы
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        userColumn.setCellValueFactory(cellData -> {
            AuditLog log = cellData.getValue();
            return new SimpleStringProperty(log.getUser() != null ? log.getUser().getLogin() : "");
        });
        
        actionTypeColumn.setCellValueFactory(new PropertyValueFactory<>("actionType"));
        actionDetailsColumn.setCellValueFactory(new PropertyValueFactory<>("actionDetails"));
        ipAddressColumn.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        
        timestampColumn.setCellValueFactory(cellData -> {
            AuditLog log = cellData.getValue();
            if (log.getTimestamp() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
                return new SimpleStringProperty(log.getTimestamp().format(formatter));
            }
            return new SimpleStringProperty("");
        });
        
        auditTable.setItems(auditLogsList);
        
        // Настройка выпадающих списков
        userFilterComboBox.setItems(usersList);
        actionTypeFilterComboBox.setItems(actionTypesList);
        
        // Обработчики кнопок
        applyFiltersButton.setOnAction(event -> handleApplyFilters());
        resetFiltersButton.setOnAction(event -> handleResetFilters());
        exportButton.setOnAction(event -> handleExport());
    }
    
    /**
     * Загружает записи аудита
     */
    private void loadAuditLogs() {
        LocalDateTime fromDateTime = fromDatePicker.getValue() != null 
                ? LocalDateTime.of(fromDatePicker.getValue(), LocalTime.MIN)
                : LocalDateTime.now().minusWeeks(1);
                
        LocalDateTime toDateTime = toDatePicker.getValue() != null
                ? LocalDateTime.of(toDatePicker.getValue(), LocalTime.MAX)
                : LocalDateTime.now();
        
        auditService.getAuditLogsByPeriod(fromDateTime, toDateTime)
                .thenAccept(logs -> {
                    Platform.runLater(() -> {
                        auditLogsList.clear();
                        auditLogsList.addAll(logs);
                        logger.debug("Загружено {} записей аудита", logs.size());
                        
                        // Обновляем список типов действий
                        updateActionTypes(logs);
                        
                        statusLabel.setText("Загружено " + logs.size() + " записей");
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось загрузить записи аудита");
                        statusLabel.setText("Ошибка загрузки данных");
                    });
                    return null;
                });
    }
    
    /**
     * Загружает пользователей для фильтрации
     */
    private void loadUsers() {
        userService.getAllUsers()
                .thenAccept(users -> {
                    Platform.runLater(() -> {
                        usersList.clear();
                        usersList.add(null); // Опция "Все пользователи"
                        usersList.addAll(users);
                        
                        // Настройка отображения в комбобоксе
                        userFilterComboBox.setConverter(new javafx.util.StringConverter<User>() {
                            @Override
                            public String toString(User user) {
                                return user == null ? "Все пользователи" : user.getLogin();
                            }
                            
                            @Override
                            public User fromString(String string) {
                                return null;
                            }
                        });
                        
                        // Выбираем "Все пользователи" по умолчанию
                        userFilterComboBox.getSelectionModel().selectFirst();
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        logger.error("Ошибка при загрузке пользователей", e);
                    });
                    return null;
                });
    }
    
    /**
     * Обновляет список типов действий на основе записей аудита
     * @param logs записи аудита
     */
    private void updateActionTypes(List<AuditLog> logs) {
        // Собираем уникальные типы действий
        List<String> types = logs.stream()
                .map(AuditLog::getActionType)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        actionTypesList.clear();
        actionTypesList.add(null); // Опция "Все типы"
        actionTypesList.addAll(types);
        
        // Настройка отображения в комбобоксе
        actionTypeFilterComboBox.setConverter(new javafx.util.StringConverter<String>() {
            @Override
            public String toString(String type) {
                return type == null ? "Все типы" : type;
            }
            
            @Override
            public String fromString(String string) {
                return string;
            }
        });
        
        // Выбираем "Все типы" по умолчанию
        actionTypeFilterComboBox.getSelectionModel().selectFirst();
    }
    
    /**
     * Обрабатывает нажатие кнопки "Применить фильтры"
     */
    private void handleApplyFilters() {
        statusLabel.setText("Применение фильтров...");
        logger.debug("Применение фильтров для аудита");
        
        try {
            LocalDateTime fromDateTime = fromDatePicker.getValue() != null 
                    ? LocalDateTime.of(fromDatePicker.getValue(), LocalTime.MIN)
                    : LocalDateTime.now().minusWeeks(1);
                    
            LocalDateTime toDateTime = toDatePicker.getValue() != null
                    ? LocalDateTime.of(toDatePicker.getValue(), LocalTime.MAX)
                    : LocalDateTime.now();
            
            User selectedUser = userFilterComboBox.getValue();
            String selectedActionType = actionTypeFilterComboBox.getValue();
            
            logger.debug("Параметры фильтрации: с {} по {}, пользователь: {}, тип действия: {}", 
                    fromDateTime, toDateTime, 
                    selectedUser != null ? selectedUser.getLogin() : "все", 
                    selectedActionType != null ? selectedActionType : "все");
            
            // Сначала получаем все логи за период
            auditService.getAuditLogsByPeriod(fromDateTime, toDateTime)
                    .thenAccept(logs -> {
                        if (logs == null || logs.isEmpty()) {
                            logger.debug("Не найдено записей аудита за указанный период");
                            Platform.runLater(() -> {
                                auditLogsList.clear();
                                statusLabel.setText("Не найдено записей за указанный период");
                            });
                            return;
                        }
                        
                        logger.debug("Получено {} записей аудита за период", logs.size());
                        
                        // Применяем дополнительные фильтры
                        List<AuditLog> filteredLogs = logs;
                        
                        if (selectedUser != null) {
                            Long userId = selectedUser.getId();
                            filteredLogs = filteredLogs.stream()
                                    .filter(log -> log.getUser() != null && 
                                                  userId.equals(log.getUser().getId()))
                                    .collect(Collectors.toList());
                            logger.debug("После фильтрации по пользователю осталось {} записей", filteredLogs.size());
                        }
                        
                        if (selectedActionType != null) {
                            filteredLogs = filteredLogs.stream()
                                    .filter(log -> selectedActionType.equals(log.getActionType()))
                                    .collect(Collectors.toList());
                            logger.debug("После фильтрации по типу действия осталось {} записей", filteredLogs.size());
                        }
                        
                        final List<AuditLog> finalFilteredLogs = filteredLogs;
                        Platform.runLater(() -> {
                            auditLogsList.clear();
                            auditLogsList.addAll(finalFilteredLogs);
                            if (finalFilteredLogs.isEmpty()) {
                                statusLabel.setText("Не найдено записей, соответствующих фильтрам");
                            } else {
                                statusLabel.setText("Найдено " + finalFilteredLogs.size() + " записей");
                            }
                        });
                    })
                    .exceptionally(e -> {
                        logger.error("Ошибка при применении фильтров аудита", e);
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                    "Не удалось применить фильтры: " + e.getMessage());
                            statusLabel.setText("Ошибка фильтрации");
                        });
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Необработанная ошибка при применении фильтров", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                    "Произошла ошибка при применении фильтров: " + e.getMessage());
            statusLabel.setText("Ошибка фильтрации");
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки "Сбросить фильтры"
     */
    private void handleResetFilters() {
        fromDatePicker.setValue(LocalDate.now().minusWeeks(1));
        toDatePicker.setValue(LocalDate.now());
        userFilterComboBox.getSelectionModel().selectFirst();
        actionTypeFilterComboBox.getSelectionModel().selectFirst();
        
        loadAuditLogs();
    }
    
    /**
     * Обрабатывает нажатие кнопки "Экспорт"
     */
    private void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Экспорт журнала аудита");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV файлы", "*.csv"),
                new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt")
        );
        
        // Предлагаем имя файла на основе текущей даты
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        fileChooser.setInitialFileName("audit_log_" + now.format(formatter) + ".csv");
        
        File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Заголовок CSV
                writer.write("ID;Пользователь;Тип действия;Детали;Дата и время;IP-адрес\n");
                
                // Строки данных
                DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
                for (AuditLog log : auditLogsList) {
                    StringBuilder line = new StringBuilder();
                    line.append(log.getId()).append(";");
                    line.append(log.getUser() != null ? log.getUser().getLogin() : "").append(";");
                    line.append(log.getActionType()).append(";");
                    
                    // Экранируем кавычки в деталях действия
                    String details = log.getActionDetails();
                    if (details != null) {
                        details = details.replace("\"", "\"\"");
                        if (details.contains(";") || details.contains("\"") || details.contains("\n")) {
                            details = "\"" + details + "\"";
                        }
                    }
                    line.append(details).append(";");
                    
                    line.append(log.getTimestamp() != null ? log.getTimestamp().format(timestampFormatter) : "").append(";");
                    line.append(log.getIpAddress());
                    line.append("\n");
                    
                    writer.write(line.toString());
                }
                
                statusLabel.setText("Экспортировано " + auditLogsList.size() + " записей в " + file.getName());
            } catch (IOException e) {
                logger.error("Ошибка при экспорте журнала аудита", e);
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось экспортировать журнал аудита: " + e.getMessage());
            }
        }
    }
    
    /**
     * Отображает диалоговое окно с сообщением
     * @param type тип диалога
     * @param title заголовок
     * @param message сообщение
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 