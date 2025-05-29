package client.ui.admin;

import client.service.BackupService;
import client.service.SettingsService;
import common.model.Backup;
import common.model.Setting;
import common.util.Constants;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Контроллер для управления резервными копиями
 */
public class BackupViewController {
    private static final Logger logger = LoggerFactory.getLogger(BackupViewController.class);
    
    @FXML
    private TableView<Backup> backupsTable;
    
    @FXML
    private TableColumn<Backup, Long> idColumn;
    
    @FXML
    private TableColumn<Backup, String> fileNameColumn;
    
    @FXML
    private TableColumn<Backup, String> fileSizeColumn;
    
    @FXML
    private TableColumn<Backup, String> createdAtColumn;
    
    @FXML
    private TableColumn<Backup, String> createdByColumn;
    
    @FXML
    private TableColumn<Backup, String> descriptionColumn;
    
    @FXML
    private TableColumn<Backup, String> statusColumn;
    
    @FXML
    private Button createBackupButton;
    
    @FXML
    private Button restoreBackupButton;
    
    @FXML
    private Button downloadBackupButton;
    
    @FXML
    private Button deleteBackupButton;
    
    @FXML
    private Label statusLabel;
    
    private BackupService backupService;
    private SettingsService settingsService;
    private ObservableList<Backup> backupsList = FXCollections.observableArrayList();
    private String authToken;
    private String backupDirectory;
    
    /**
     * Устанавливает токен авторизации и инициализирует сервисы
     * @param authToken токен авторизации
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
        backupService = new BackupService(authToken);
        settingsService = new SettingsService(authToken);
        
        // Получаем директорию для резервных копий из настроек
        settingsService.getSettingByKey(Constants.SETTING_BACKUP_DIR)
                .thenAccept(setting -> {
                    if (setting != null) {
                        backupDirectory = setting.getValue();
                    } else {
                        backupDirectory = Constants.BACKUP_DIR;
                    }
                })
                .exceptionally(e -> {
                    logger.error("Ошибка при получении директории для резервных копий", e);
                    backupDirectory = Constants.BACKUP_DIR;
                    return null;
                });
        
        loadBackups();
    }
    
    /**
     * Инициализирует контроллер
     */
    @FXML
    private void initialize() {
        // Настройка таблицы
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        
        fileSizeColumn.setCellValueFactory(cellData -> {
            Backup backup = cellData.getValue();
            double sizeMb = backup.getSizeMb();
            return new SimpleStringProperty(String.format("%.2f МБ", sizeMb));
        });
        
        createdAtColumn.setCellValueFactory(cellData -> {
            Backup backup = cellData.getValue();
            if (backup.getCreatedAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
                return new SimpleStringProperty(backup.getCreatedAt().format(formatter));
            }
            return new SimpleStringProperty("");
        });
        
        createdByColumn.setCellValueFactory(cellData -> {
            Backup backup = cellData.getValue();
            return new SimpleStringProperty(backup.getCreatedBy() != null ? 
                    backup.getCreatedBy().getLogin() : "");
        });
        
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        statusColumn.setCellValueFactory(cellData -> {
            Backup backup = cellData.getValue();
            return new SimpleStringProperty(backup.getIsValid() != null && backup.getIsValid() ? 
                    "Валидный" : "Невалидный");
        });
        
        backupsTable.setItems(backupsList);
        
        // Настройка кнопок
        restoreBackupButton.disableProperty().bind(backupsTable.getSelectionModel().selectedItemProperty().isNull());
        downloadBackupButton.disableProperty().bind(backupsTable.getSelectionModel().selectedItemProperty().isNull());
        deleteBackupButton.disableProperty().bind(backupsTable.getSelectionModel().selectedItemProperty().isNull());
        
        // Обработчики кнопок
        createBackupButton.setOnAction(event -> handleCreateBackup());
        restoreBackupButton.setOnAction(event -> handleRestoreBackup());
        downloadBackupButton.setOnAction(event -> handleDownloadBackup());
        deleteBackupButton.setOnAction(event -> handleDeleteBackup());
    }
    
    /**
     * Загружает список резервных копий
     */
    private void loadBackups() {
        backupService.getBackups()
                .thenAccept(backups -> {
                    Platform.runLater(() -> {
                        backupsList.clear();
                        backupsList.addAll(backups);
                        logger.debug("Загружено {} резервных копий", backups.size());
                        statusLabel.setText("Загружено " + backups.size() + " резервных копий");
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось загрузить резервные копии");
                        statusLabel.setText("Ошибка загрузки данных");
                    });
                    return null;
                });
    }
    
    /**
     * Обрабатывает нажатие кнопки создания резервной копии
     */
    @FXML
    private void handleCreateBackup() {
        statusLabel.setText("Создание резервной копии...");
        createBackupButton.setDisable(true);
        
        try {
            backupService.createBackup()
                    .thenAccept(success -> {
                        Platform.runLater(() -> {
                            if (success) {
                                statusLabel.setText("Резервная копия успешно создана");
                                // Обновляем список резервных копий
                                loadBackups();
                            } else {
                                statusLabel.setText("Ошибка создания резервной копии");
                                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                        "Не удалось создать резервную копию");
                            }
                            createBackupButton.setDisable(false);
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            statusLabel.setText("Ошибка создания резервной копии: " + e.getMessage());
                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                    "Не удалось создать резервную копию: " + e.getMessage());
                            createBackupButton.setDisable(false);
                        });
                        return null;
                    });
        } catch (Exception e) {
            statusLabel.setText("Ошибка создания резервной копии: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                    "Не удалось создать резервную копию: " + e.getMessage());
            createBackupButton.setDisable(false);
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки "Восстановить из резервной копии"
     */
    private void handleRestoreBackup() {
        Backup selectedBackup = backupsTable.getSelectionModel().getSelectedItem();
        if (selectedBackup == null) {
            return;
        }
        
        // Подтверждение восстановления
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение");
        confirmAlert.setHeaderText("Восстановление из резервной копии");
        confirmAlert.setContentText("Вы уверены, что хотите восстановить систему из резервной копии?\n\n" +
                "ID: " + selectedBackup.getId() + "\n" +
                "Файл: " + selectedBackup.getFileName() + "\n" +
                "Описание: " + (selectedBackup.getDescription() != null ? selectedBackup.getDescription() : "Нет") + "\n\n" +
                "ВНИМАНИЕ: Все текущие данные будут заменены данными из резервной копии!");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Показываем индикатор прогресса
            statusLabel.setText("Восстановление из резервной копии...");
            restoreBackupButton.setDisable(true);
            
            backupService.restoreBackup(selectedBackup.getId())
                    .thenAccept(success -> {
                        Platform.runLater(() -> {
                            restoreBackupButton.setDisable(false);
                            if (success) {
                                statusLabel.setText("Система успешно восстановлена из резервной копии");
                                showAlert(Alert.AlertType.INFORMATION, "Успех", 
                                        "Система успешно восстановлена из резервной копии. " +
                                        "Перезапустите приложение для применения изменений.");
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось восстановить систему из резервной копии");
                                statusLabel.setText("Ошибка восстановления");
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            restoreBackupButton.setDisable(false);
                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                    "Ошибка при восстановлении из резервной копии: " + e.getMessage());
                            statusLabel.setText("Ошибка восстановления");
                        });
                        return null;
                    });
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки "Скачать резервную копию"
     */
    private void handleDownloadBackup() {
        Backup selectedBackup = backupsTable.getSelectionModel().getSelectedItem();
        if (selectedBackup == null) {
            return;
        }
        
        // Предлагаем выбрать директорию для сохранения, если backupDirectory не существует
        String destinationDir = backupDirectory;
        File directory = new File(destinationDir);
        if (!directory.exists() || !directory.isDirectory()) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Выберите директорию для сохранения резервной копии");
            File selectedDirectory = directoryChooser.showDialog(downloadBackupButton.getScene().getWindow());
            if (selectedDirectory != null) {
                destinationDir = selectedDirectory.getAbsolutePath();
            } else {
                return; // Пользователь отменил выбор директории
            }
        }
        
        // Показываем индикатор прогресса
        statusLabel.setText("Скачивание резервной копии...");
        downloadBackupButton.setDisable(true);
        
        final String finalDestinationDir = destinationDir;
        backupService.downloadBackup(selectedBackup.getId(), destinationDir)
                .thenAccept(filePath -> {
                    Platform.runLater(() -> {
                        downloadBackupButton.setDisable(false);
                        if (filePath != null) {
                            statusLabel.setText("Резервная копия скачана: " + filePath);
                            showAlert(Alert.AlertType.INFORMATION, "Успех", 
                                    "Резервная копия успешно скачана в:\n" + filePath);
                            
                            // Сохраняем директорию в настройках, если она отличается от текущей
                            if (!finalDestinationDir.equals(backupDirectory)) {
                                settingsService.getSettingByKey(Constants.SETTING_BACKUP_DIR)
                                        .thenCompose(setting -> {
                                            if (setting != null) {
                                                setting.setValue(finalDestinationDir);
                                                return settingsService.updateSetting(setting);
                                            } else {
                                                return settingsService.createSetting(
                                                        Constants.SETTING_BACKUP_DIR, 
                                                        finalDestinationDir, 
                                                        "Директория для хранения резервных копий");
                                            }
                                        })
                                        .exceptionally(e -> {
                                            logger.error("Ошибка при сохранении директории для резервных копий", e);
                                            return null;
                                        });
                                
                                backupDirectory = finalDestinationDir;
                            }
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось скачать резервную копию");
                            statusLabel.setText("Ошибка скачивания резервной копии");
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        downloadBackupButton.setDisable(false);
                        showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                "Ошибка при скачивании резервной копии: " + e.getMessage());
                        statusLabel.setText("Ошибка скачивания резервной копии");
                    });
                    return null;
                });
    }
    
    /**
     * Обрабатывает нажатие кнопки "Удалить резервную копию"
     */
    private void handleDeleteBackup() {
        Backup selectedBackup = backupsTable.getSelectionModel().getSelectedItem();
        if (selectedBackup == null) {
            return;
        }
        
        // Подтверждение удаления
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение");
        confirmAlert.setHeaderText("Удаление резервной копии");
        confirmAlert.setContentText("Вы уверены, что хотите удалить резервную копию?\n\n" +
                "ID: " + selectedBackup.getId() + "\n" +
                "Файл: " + selectedBackup.getFileName() + "\n" +
                "Описание: " + (selectedBackup.getDescription() != null ? selectedBackup.getDescription() : "Нет"));
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Показываем индикатор прогресса
            statusLabel.setText("Удаление резервной копии...");
            deleteBackupButton.setDisable(true);
            
            backupService.deleteBackup(selectedBackup.getId())
                    .thenAccept(success -> {
                        Platform.runLater(() -> {
                            deleteBackupButton.setDisable(false);
                            if (success) {
                                statusLabel.setText("Резервная копия успешно удалена");
                                backupsList.remove(selectedBackup);
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось удалить резервную копию");
                                statusLabel.setText("Ошибка удаления резервной копии");
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            deleteBackupButton.setDisable(false);
                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                    "Ошибка при удалении резервной копии: " + e.getMessage());
                            statusLabel.setText("Ошибка удаления резервной копии");
                        });
                        return null;
                    });
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