package client.ui.admin.reports;

import client.service.ReportService;
import client.service.ReportService.UserActivityRecord;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Контроллер для отчета по активности пользователей
 */
public class UserActivityReportController {
    private static final Logger logger = LoggerFactory.getLogger(UserActivityReportController.class);
    
    // Компоненты UI
    @FXML private TableView<UserActivityRecord> activityTable;
    @FXML private LineChart<String, Number> activityChart;
    @FXML private BarChart<String, Number> sessionChart;
    @FXML private Button exportButton;
    @FXML private Label statusLabel;
    @FXML private Label periodLabel;
    
    // Сервисы
    private ReportService reportService;
    private String authToken;
    
    // Данные
    private LocalDate fromDate;
    private LocalDate toDate;
    private ObservableList<UserActivityRecord> activityData = FXCollections.observableArrayList();
    
    /**
     * Инициализация контроллера
     */
    @FXML
    private void initialize() {
        // Настройка таблицы
        TableColumn<UserActivityRecord, String> usernameColumn = new TableColumn<>("Пользователь");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        
        TableColumn<UserActivityRecord, String> lastLoginColumn = new TableColumn<>("Последний вход");
        lastLoginColumn.setCellValueFactory(new PropertyValueFactory<>("lastLogin"));
        
        TableColumn<UserActivityRecord, Integer> sessionsColumn = new TableColumn<>("Сессии");
        sessionsColumn.setCellValueFactory(new PropertyValueFactory<>("sessionsCount"));
        
        TableColumn<UserActivityRecord, Integer> minutesColumn = new TableColumn<>("Минуты");
        minutesColumn.setCellValueFactory(new PropertyValueFactory<>("totalMinutes"));
        
        TableColumn<UserActivityRecord, Integer> earnedColumn = new TableColumn<>("Заработано");
        earnedColumn.setCellValueFactory(new PropertyValueFactory<>("pointsEarned"));
        
        TableColumn<UserActivityRecord, Integer> spentColumn = new TableColumn<>("Потрачено");
        spentColumn.setCellValueFactory(new PropertyValueFactory<>("pointsSpent"));
        
        activityTable.getColumns().addAll(usernameColumn, lastLoginColumn, sessionsColumn, 
                                         minutesColumn, earnedColumn, spentColumn);
        activityTable.setItems(activityData);
        
        // Настройка кнопки экспорта
        exportButton.setOnAction(event -> handleExport());
    }
    
    /**
     * Устанавливает токен авторизации и инициализирует сервисы
     * @param authToken токен авторизации
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
        reportService = new ReportService(authToken);
    }
    
    /**
     * Устанавливает период для отчета
     * @param fromDate начальная дата
     * @param toDate конечная дата
     */
    public void setReportPeriod(LocalDate fromDate, LocalDate toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        periodLabel.setText("Период: " + fromDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + 
                " - " + toDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
    }
    
    /**
     * Загружает данные для отчета
     */
    public void loadReportData() {
        statusLabel.setText("Загрузка данных...");
        
        reportService.getUserActivityReport(fromDate, toDate)
                .thenAccept(reportData -> {
                    Platform.runLater(() -> {
                        activityData.clear();
                        
                        // Загружаем данные в таблицу
                        activityData.addAll(reportData.getRecords());
                        
                        // Обновляем графики
                        updateActivityChart(reportData.getActivityByDay());
                        updateSessionChart(reportData.getSessionsByUser());
                        
                        statusLabel.setText("Загружено записей: " + activityData.size());
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        logger.error("Ошибка при загрузке отчета по активности пользователей", e);
                        statusLabel.setText("Ошибка загрузки данных");
                        
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Ошибка");
                        alert.setHeaderText("Ошибка загрузки отчета");
                        alert.setContentText("Не удалось загрузить данные отчета: " + e.getMessage());
                        alert.showAndWait();
                    });
                    return null;
                });
    }
    
    /**
     * Обновляет график активности по дням
     * @param activityByDay данные активности по дням
     */
    private void updateActivityChart(Map<String, Integer> activityByDay) {
        activityChart.getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Активные пользователи");
        
        activityByDay.forEach((day, count) -> {
            series.getData().add(new XYChart.Data<>(day, count));
        });
        
        activityChart.getData().add(series);
    }
    
    /**
     * Обновляет график сессий по пользователям
     * @param sessionsByUser данные сессий по пользователям
     */
    private void updateSessionChart(Map<String, Integer> sessionsByUser) {
        sessionChart.getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Количество сессий");
        
        sessionsByUser.forEach((user, count) -> {
            series.getData().add(new XYChart.Data<>(user, count));
        });
        
        sessionChart.getData().add(series);
    }
    
    /**
     * Обработчик кнопки экспорта
     */
    private void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить отчет");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("user_activity_report_" + 
                fromDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" +
                toDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv");
        
        File file = fileChooser.showSaveDialog(activityTable.getScene().getWindow());
        
        if (file != null) {
            reportService.exportUserActivityReportToCsv(fromDate, toDate, file.getAbsolutePath())
                    .thenAccept(success -> {
                        Platform.runLater(() -> {
                            if (success) {
                                statusLabel.setText("Отчет успешно экспортирован");
                                
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Экспорт отчета");
                                alert.setHeaderText("Отчет успешно экспортирован");
                                alert.setContentText("Отчет сохранен в файл:\n" + file.getAbsolutePath());
                                alert.showAndWait();
                            } else {
                                statusLabel.setText("Ошибка экспорта отчета");
                                
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Ошибка");
                                alert.setHeaderText("Ошибка экспорта отчета");
                                alert.setContentText("Не удалось экспортировать отчет");
                                alert.showAndWait();
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            logger.error("Ошибка при экспорте отчета", e);
                            statusLabel.setText("Ошибка экспорта отчета");
                            
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Ошибка");
                            alert.setHeaderText("Ошибка экспорта отчета");
                            alert.setContentText("Не удалось экспортировать отчет: " + e.getMessage());
                            alert.showAndWait();
                        });
                        return null;
                    });
        }
    }
} 