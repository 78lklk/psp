package client.ui.admin;

import client.service.SettingsService;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Контроллер для управления настройками системы
 */
public class SettingsViewController {
    private static final Logger logger = LoggerFactory.getLogger(SettingsViewController.class);
    
    @FXML
    private TableView<Setting> settingsTable;
    
    @FXML
    private TableColumn<Setting, String> keyColumn;
    
    @FXML
    private TableColumn<Setting, String> valueColumn;
    
    @FXML
    private TableColumn<Setting, String> descriptionColumn;
    
    @FXML
    private TableColumn<Setting, String> lastUpdatedColumn;
    
    @FXML
    private Button addSettingButton;
    
    @FXML
    private Button editSettingButton;
    
    @FXML
    private Button deleteSettingButton;
    
    @FXML
    private ComboBox<String> themeComboBox;
    
    @FXML
    private TextField backupDirField;
    
    @FXML
    private Button browseBackupDirButton;
    
    @FXML
    private TextField reportsDirField;
    
    @FXML
    private Button browseReportsDirButton;
    
    @FXML
    private Button saveChangesButton;
    
    @FXML
    private Label statusLabel;
    
    private SettingsService settingsService;
    private ObservableList<Setting> settingsList = FXCollections.observableArrayList();
    private String authToken;
    
    /**
     * Устанавливает токен авторизации и инициализирует сервис
     * @param authToken токен авторизации
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
        settingsService = new SettingsService(authToken);
        loadSettings();
        initializeQuickSettings();
    }
    
    /**
     * Инициализирует контроллер
     */
    @FXML
    private void initialize() {
        // Настройка таблицы
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        lastUpdatedColumn.setCellValueFactory(cellData -> {
            Setting setting = cellData.getValue();
            if (setting.getLastUpdated() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
                return new SimpleStringProperty(setting.getLastUpdated().format(formatter));
            } else {
                return new SimpleStringProperty("");
            }
        });
        
        settingsTable.setItems(settingsList);
        
        // Настройка кнопок редактирования
        editSettingButton.disableProperty().bind(settingsTable.getSelectionModel().selectedItemProperty().isNull());
        deleteSettingButton.disableProperty().bind(settingsTable.getSelectionModel().selectedItemProperty().isNull());
        
        // Настройка выпадающего списка тем
        themeComboBox.getItems().addAll("Светлая", "Темная", "Системная");
        
        // Обработчики кнопок
        addSettingButton.setOnAction(event -> handleAddSetting());
        editSettingButton.setOnAction(event -> handleEditSetting());
        deleteSettingButton.setOnAction(event -> handleDeleteSetting());
        browseBackupDirButton.setOnAction(event -> handleBrowseBackupDir());
        browseReportsDirButton.setOnAction(event -> handleBrowseReportsDir());
        saveChangesButton.setOnAction(event -> handleSaveChanges());
    }
    
    /**
     * Инициализирует быстрые настройки из полученных настроек системы
     */
    private void initializeQuickSettings() {
        // Настройка темы
        settingsService.getSettingByKey(Constants.SETTING_THEME)
                .thenAccept(setting -> {
                    if (setting != null) {
                        Platform.runLater(() -> {
                            String theme = setting.getValue();
                            if (Constants.THEME_LIGHT.equals(theme)) {
                                themeComboBox.getSelectionModel().select("Светлая");
                            } else if (Constants.THEME_DARK.equals(theme)) {
                                themeComboBox.getSelectionModel().select("Темная");
                            } else {
                                themeComboBox.getSelectionModel().select("Системная");
                            }
                        });
                    }
                }).exceptionally(e -> {
                    logger.error("Ошибка при получении настройки темы", e);
                    return null;
                });
        
        // Настройка директории для резервных копий
        settingsService.getSettingByKey(Constants.SETTING_BACKUP_DIR)
                .thenAccept(setting -> {
                    if (setting != null) {
                        Platform.runLater(() -> backupDirField.setText(setting.getValue()));
                    } else {
                        Platform.runLater(() -> backupDirField.setText(Constants.BACKUP_DIR));
                    }
                }).exceptionally(e -> {
                    logger.error("Ошибка при получении настройки директории резервных копий", e);
                    return null;
                });
        
        // Настройка директории для отчетов
        settingsService.getSettingByKey(Constants.SETTING_REPORTS_DIR)
                .thenAccept(setting -> {
                    if (setting != null) {
                        Platform.runLater(() -> reportsDirField.setText(setting.getValue()));
                    } else {
                        Platform.runLater(() -> reportsDirField.setText(Constants.REPORTS_DIR));
                    }
                }).exceptionally(e -> {
                    logger.error("Ошибка при получении настройки директории отчетов", e);
                    return null;
                });
    }
    
    /**
     * Загружает настройки системы
     */
    private void loadSettings() {
        statusLabel.setText("Загрузка настроек...");
        
        settingsService.getAllSettings()
                .thenAccept(settings -> {
                    Platform.runLater(() -> {
                        settingsList.clear();
                        
                        settings.forEach((key, value) -> {
                            settingsList.add(new Setting(
                                    key, 
                                    value.getValue(), 
                                    value.getDescription()
                            ));
                        });
                        
                        statusLabel.setText("Загружено настроек: " + settingsList.size());
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        logger.error("Ошибка при загрузке настроек", e);
                        statusLabel.setText("Ошибка загрузки настроек");
                        
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Ошибка");
                        alert.setHeaderText("Ошибка загрузки настроек");
                        alert.setContentText("Не удалось загрузить настройки: " + e.getMessage());
                        alert.showAndWait();
                    });
                    return null;
                });
    }
    
    /**
     * Обрабатывает нажатие кнопки "Добавить настройку"
     */
    private void handleAddSetting() {
        // Создаем диалог
        Dialog<Setting> dialog = new Dialog<>();
        dialog.setTitle("Новая настройка");
        dialog.setHeaderText("Добавление новой настройки");
        
        // Кнопки диалога
        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Контент диалога
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField keyField = new TextField();
        keyField.setPromptText("Ключ настройки");
        TextField valueField = new TextField();
        valueField.setPromptText("Значение");
        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Описание");
        
        grid.add(new Label("Ключ:"), 0, 0);
        grid.add(keyField, 1, 0);
        grid.add(new Label("Значение:"), 0, 1);
        grid.add(valueField, 1, 1);
        grid.add(new Label("Описание:"), 0, 2);
        grid.add(descriptionField, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        // Фокус на первом поле
        Platform.runLater(keyField::requestFocus);
        
        // Конвертация результата
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String key = keyField.getText().trim();
                String value = valueField.getText().trim();
                String description = descriptionField.getText().trim();
                
                if (key.isEmpty() || value.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Ошибка", "Ключ и значение должны быть заполнены");
                    return null;
                }
                
                return new Setting(key, value, description);
            }
            return null;
        });
        
        Optional<Setting> result = dialog.showAndWait();
        result.ifPresent(setting -> {
            settingsService.createSetting(setting.getKey(), setting.getValue(), setting.getDescription())
                    .thenAccept(createdSetting -> {
                        if (createdSetting != null) {
                            Platform.runLater(() -> {
                                settingsList.add(createdSetting);
                                statusLabel.setText("Настройка добавлена: " + createdSetting.getKey());
                            });
                        } else {
                            Platform.runLater(() -> 
                                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                        "Не удалось добавить настройку")
                            );
                        }
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> 
                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                    "Ошибка при добавлении настройки: " + e.getMessage())
                        );
                        return null;
                    });
        });
    }
    
    /**
     * Обрабатывает нажатие кнопки "Редактировать настройку"
     */
    private void handleEditSetting() {
        Setting selectedSetting = settingsTable.getSelectionModel().getSelectedItem();
        if (selectedSetting == null) {
            return;
        }
        
        // Создаем диалог
        Dialog<Setting> dialog = new Dialog<>();
        dialog.setTitle("Редактирование настройки");
        dialog.setHeaderText("Редактирование настройки: " + selectedSetting.getKey());
        
        // Кнопки диалога
        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Контент диалога
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField keyField = new TextField(selectedSetting.getKey());
        keyField.setDisable(true); // Ключ нельзя менять
        TextField valueField = new TextField(selectedSetting.getValue());
        TextField descriptionField = new TextField(selectedSetting.getDescription());
        
        grid.add(new Label("Ключ:"), 0, 0);
        grid.add(keyField, 1, 0);
        grid.add(new Label("Значение:"), 0, 1);
        grid.add(valueField, 1, 1);
        grid.add(new Label("Описание:"), 0, 2);
        grid.add(descriptionField, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        // Фокус на поле значения
        Platform.runLater(valueField::requestFocus);
        
        // Конвертация результата
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String value = valueField.getText().trim();
                String description = descriptionField.getText().trim();
                
                if (value.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Ошибка", "Значение должно быть заполнено");
                    return null;
                }
                
                selectedSetting.setValue(value);
                selectedSetting.setDescription(description);
                return selectedSetting;
            }
            return null;
        });
        
        Optional<Setting> result = dialog.showAndWait();
        result.ifPresent(setting -> {
            settingsService.updateSetting(setting)
                    .thenAccept(updatedSetting -> {
                        if (updatedSetting != null) {
                            Platform.runLater(() -> {
                                int index = settingsList.indexOf(selectedSetting);
                                if (index >= 0) {
                                    settingsList.set(index, updatedSetting);
                                }
                                statusLabel.setText("Настройка обновлена: " + updatedSetting.getKey());
                                
                                // Обновляем быстрые настройки, если обновилась одна из них
                                if (Constants.SETTING_THEME.equals(updatedSetting.getKey()) ||
                                        Constants.SETTING_BACKUP_DIR.equals(updatedSetting.getKey()) ||
                                        Constants.SETTING_REPORTS_DIR.equals(updatedSetting.getKey())) {
                                    initializeQuickSettings();
                                }
                            });
                        } else {
                            Platform.runLater(() -> 
                                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                        "Не удалось обновить настройку")
                            );
                        }
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> 
                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                    "Ошибка при обновлении настройки: " + e.getMessage())
                        );
                        return null;
                    });
        });
    }
    
    /**
     * Обрабатывает нажатие кнопки "Удалить настройку"
     */
    private void handleDeleteSetting() {
        Setting selectedSetting = settingsTable.getSelectionModel().getSelectedItem();
        if (selectedSetting == null) {
            return;
        }
        
        // Предупреждение о системных настройках
        List<String> systemSettings = Arrays.asList(
                Constants.SETTING_THEME,
                Constants.SETTING_BACKUP_DIR,
                Constants.SETTING_REPORTS_DIR,
                Constants.SETTING_PDF_DIR
        );
        
        if (systemSettings.contains(selectedSetting.getKey())) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Предупреждение");
            alert.setHeaderText("Удаление системной настройки");
            alert.setContentText("Вы собираетесь удалить системную настройку. " +
                    "Это может привести к нестабильной работе приложения.\n\n" +
                    "Вы уверены, что хотите продолжить?");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение");
        confirmAlert.setHeaderText("Удаление настройки");
        confirmAlert.setContentText("Вы уверены, что хотите удалить настройку: " + selectedSetting.getKey() + "?");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            settingsService.deleteSetting(selectedSetting.getKey())
                    .thenAccept(success -> {
                        if (success) {
                            Platform.runLater(() -> {
                                settingsList.remove(selectedSetting);
                                statusLabel.setText("Настройка удалена: " + selectedSetting.getKey());
                            });
                        } else {
                            Platform.runLater(() -> 
                                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                        "Не удалось удалить настройку")
                            );
                        }
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> 
                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                    "Ошибка при удалении настройки: " + e.getMessage())
                        );
                        return null;
                    });
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки выбора директории для резервных копий
     */
    private void handleBrowseBackupDir() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберите директорию для резервных копий");
        
        // Установка начальной директории
        File initialDirectory = new File(backupDirField.getText());
        if (initialDirectory.exists() && initialDirectory.isDirectory()) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }
        
        File selectedDirectory = directoryChooser.showDialog(browseBackupDirButton.getScene().getWindow());
        if (selectedDirectory != null) {
            backupDirField.setText(selectedDirectory.getAbsolutePath());
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки выбора директории для отчетов
     */
    private void handleBrowseReportsDir() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберите директорию для отчетов");
        
        // Установка начальной директории
        File initialDirectory = new File(reportsDirField.getText());
        if (initialDirectory.exists() && initialDirectory.isDirectory()) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }
        
        File selectedDirectory = directoryChooser.showDialog(browseReportsDirButton.getScene().getWindow());
        if (selectedDirectory != null) {
            reportsDirField.setText(selectedDirectory.getAbsolutePath());
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки "Сохранить изменения"
     */
    private void handleSaveChanges() {
        statusLabel.setText("Сохранение настроек...");
        logger.debug("Сохранение настроек");
        
        // Деактивируем кнопку на время сохранения
        saveChangesButton.setDisable(true);
        
        // Сохранение настройки темы
        String themeValue;
        String selectedTheme = themeComboBox.getSelectionModel().getSelectedItem();
        if ("Светлая".equals(selectedTheme)) {
            themeValue = Constants.THEME_LIGHT;
        } else if ("Темная".equals(selectedTheme)) {
            themeValue = Constants.THEME_DARK;
        } else {
            themeValue = "SYSTEM";
        }
        
        // Получаем директории для сохранения
        String backupDir = backupDirField.getText().trim();
        String reportsDir = reportsDirField.getText().trim();
        
        // Проверяем и создаем директории если они не существуют
        boolean directoriesOk = true;
        
        File backupDirFile = new File(backupDir);
        if (!backupDirFile.exists()) {
            try {
                if (!backupDirFile.mkdirs()) {
                    logger.warn("Не удалось создать директорию для резервных копий: {}", backupDir);
                    showAlert(Alert.AlertType.WARNING, "Предупреждение", 
                            "Не удалось создать директорию для резервных копий: " + backupDir);
                    directoriesOk = false;
                } else {
                    logger.debug("Создана директория для резервных копий: {}", backupDir);
                }
            } catch (Exception e) {
                logger.error("Ошибка при создании директории для резервных копий", e);
                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                        "Ошибка при создании директории для резервных копий: " + e.getMessage());
                directoriesOk = false;
            }
        } else if (!backupDirFile.isDirectory()) {
            logger.error("Указанный путь для резервных копий не является директорией: {}", backupDir);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                    "Указанный путь не является директорией: " + backupDir);
            directoriesOk = false;
        } else if (!backupDirFile.canWrite()) {
            logger.error("Нет прав на запись в директорию резервных копий: {}", backupDir);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                    "Нет прав на запись в директорию: " + backupDir);
            directoriesOk = false;
        }
        
        File reportsDirFile = new File(reportsDir);
        if (!reportsDirFile.exists()) {
            try {
                if (!reportsDirFile.mkdirs()) {
                    logger.warn("Не удалось создать директорию для отчетов: {}", reportsDir);
                    showAlert(Alert.AlertType.WARNING, "Предупреждение", 
                            "Не удалось создать директорию для отчетов: " + reportsDir);
                    directoriesOk = false;
                } else {
                    logger.debug("Создана директория для отчетов: {}", reportsDir);
                }
            } catch (Exception e) {
                logger.error("Ошибка при создании директории для отчетов", e);
                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                        "Ошибка при создании директории для отчетов: " + e.getMessage());
                directoriesOk = false;
            }
        } else if (!reportsDirFile.isDirectory()) {
            logger.error("Указанный путь для отчетов не является директорией: {}", reportsDir);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                    "Указанный путь не является директорией: " + reportsDir);
            directoriesOk = false;
        } else if (!reportsDirFile.canWrite()) {
            logger.error("Нет прав на запись в директорию отчетов: {}", reportsDir);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                    "Нет прав на запись в директорию: " + reportsDir);
            directoriesOk = false;
        }
        
        // Если проблемы с директориями, прекращаем сохранение
        if (!directoriesOk) {
            statusLabel.setText("Ошибка при проверке директорий");
            saveChangesButton.setDisable(false);
            return;
        }

        // Счетчик для отслеживания завершения всех операций
        final int[] completedOperations = {0};
        final int totalOperations = 3; // Тема, директория бэкапов, директория отчетов
        final boolean[] hasErrors = {false};
        
        final String finalThemeValue = themeValue;
        
        // Сохраняем настройку темы
        settingsService.getSettingByKey(Constants.SETTING_THEME)
                .thenCompose(setting -> {
                    if (setting != null) {
                        logger.debug("Обновляем настройку темы: {}", finalThemeValue);
                        setting.setValue(finalThemeValue);
                        return settingsService.updateSetting(setting);
                    } else {
                        logger.debug("Создаем новую настройку темы: {}", finalThemeValue);
                        return settingsService.createSetting(
                                Constants.SETTING_THEME, 
                                finalThemeValue, 
                                "Тема оформления приложения");
                    }
                })
                .thenAccept(result -> {
                    Platform.runLater(() -> {
                        if (result != null) {
                            logger.debug("Настройка темы успешно сохранена");
                        } else {
                            logger.error("Ошибка при сохранении настройки темы");
                            hasErrors[0] = true;
                        }
                        
                        // Увеличиваем счетчик завершенных операций
                        completedOperations[0]++;
                        checkAllOperationsComplete(completedOperations[0], totalOperations, hasErrors[0]);
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        logger.error("Исключение при сохранении настройки темы", e);
                        hasErrors[0] = true;
                        
                        // Увеличиваем счетчик завершенных операций
                        completedOperations[0]++;
                        checkAllOperationsComplete(completedOperations[0], totalOperations, hasErrors[0]);
                    });
                    return null;
                });
        
        // Сохранение директории для резервных копий
        settingsService.getSettingByKey(Constants.SETTING_BACKUP_DIR)
                .thenCompose(setting -> {
                    if (setting != null) {
                        logger.debug("Обновляем настройку директории резервных копий: {}", backupDir);
                        setting.setValue(backupDir);
                        return settingsService.updateSetting(setting);
                    } else {
                        logger.debug("Создаем новую настройку директории резервных копий: {}", backupDir);
                        return settingsService.createSetting(
                                Constants.SETTING_BACKUP_DIR, 
                                backupDir, 
                                "Директория для хранения резервных копий");
                    }
                })
                .thenAccept(result -> {
                    Platform.runLater(() -> {
                        if (result != null) {
                            logger.debug("Настройка директории резервных копий успешно сохранена");
                        } else {
                            logger.error("Ошибка при сохранении настройки директории резервных копий");
                            hasErrors[0] = true;
                        }
                        
                        // Увеличиваем счетчик завершенных операций
                        completedOperations[0]++;
                        checkAllOperationsComplete(completedOperations[0], totalOperations, hasErrors[0]);
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        logger.error("Исключение при сохранении настройки директории резервных копий", e);
                        hasErrors[0] = true;
                        
                        // Увеличиваем счетчик завершенных операций
                        completedOperations[0]++;
                        checkAllOperationsComplete(completedOperations[0], totalOperations, hasErrors[0]);
                    });
                    return null;
                });
        
        // Сохранение директории для отчетов
        settingsService.getSettingByKey(Constants.SETTING_REPORTS_DIR)
                .thenCompose(setting -> {
                    if (setting != null) {
                        logger.debug("Обновляем настройку директории отчетов: {}", reportsDir);
                        setting.setValue(reportsDir);
                        return settingsService.updateSetting(setting);
                    } else {
                        logger.debug("Создаем новую настройку директории отчетов: {}", reportsDir);
                        return settingsService.createSetting(
                                Constants.SETTING_REPORTS_DIR, 
                                reportsDir, 
                                "Директория для хранения отчетов");
                    }
                })
                .thenAccept(result -> {
                    Platform.runLater(() -> {
                        if (result != null) {
                            logger.debug("Настройка директории отчетов успешно сохранена");
                        } else {
                            logger.error("Ошибка при сохранении настройки директории отчетов");
                            hasErrors[0] = true;
                        }
                        
                        // Увеличиваем счетчик завершенных операций
                        completedOperations[0]++;
                        checkAllOperationsComplete(completedOperations[0], totalOperations, hasErrors[0]);
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        logger.error("Исключение при сохранении настройки директории отчетов", e);
                        hasErrors[0] = true;
                        
                        // Увеличиваем счетчик завершенных операций
                        completedOperations[0]++;
                        checkAllOperationsComplete(completedOperations[0], totalOperations, hasErrors[0]);
                    });
                    return null;
                });
    }
    
    /**
     * Проверяет, завершены ли все операции сохранения
     */
    private void checkAllOperationsComplete(int completed, int total, boolean hasErrors) {
        if (completed >= total) {
            if (hasErrors) {
                statusLabel.setText("Ошибка при сохранении некоторых настроек");
                showAlert(Alert.AlertType.WARNING, "Предупреждение", 
                        "Не все настройки были успешно сохранены. Проверьте журнал для получения подробной информации.");
            } else {
                statusLabel.setText("Настройки успешно сохранены");
            }
            
            // Активируем кнопку снова
            saveChangesButton.setDisable(false);
            
            // Перезагрузка списка настроек
            loadSettings();
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