package client.ui.admin.reports;

import client.service.ReportService;
import client.service.ReportService.PointsReportData;
import client.service.ReportService.PointsRecord;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Контроллер для отчета о начисленных баллах
 */
public class PointsReportController {
    private static final Logger logger = LoggerFactory.getLogger(PointsReportController.class);
    
    @FXML
    private DatePicker fromDatePicker;
    
    @FXML
    private DatePicker toDatePicker;
    
    @FXML
    private Button updateButton;
    
    @FXML
    private Button exportButton;
    
    @FXML
    private Button closeButton;
    
    @FXML
    private TableView<PointsRecord> pointsTable;
    
    @FXML
    private TableColumn<PointsRecord, String> dateColumn;
    
    @FXML
    private TableColumn<PointsRecord, String> cardNumberColumn;
    
    @FXML
    private TableColumn<PointsRecord, String> userColumn;
    
    @FXML
    private TableColumn<PointsRecord, Integer> pointsColumn;
    
    @FXML
    private TableColumn<PointsRecord, String> reasonColumn;
    
    @FXML
    private TableColumn<PointsRecord, String> typeColumn;
    
    @FXML
    private BarChart<String, Number> pointsChart;
    
    @FXML
    private CategoryAxis dateAxis;
    
    @FXML
    private NumberAxis pointsAxis;
    
    @FXML
    private Label totalPointsLabel;
    
    @FXML
    private Label totalOperationsLabel;
    
    private ReportService reportService;
    private String authToken;
    private LocalDate fromDate;
    private LocalDate toDate;
    private ObservableList<PointsRecord> pointsList = FXCollections.observableArrayList();
    
    /**
     * Инициализирует контроллер
     */
    @FXML
    private void initialize() {
        // Настройка таблицы
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateFormatted"));
        cardNumberColumn.setCellValueFactory(new PropertyValueFactory<>("cardNumber"));
        userColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        pointsColumn.setCellValueFactory(new PropertyValueFactory<>("points"));
        reasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        
        pointsTable.setItems(pointsList);
        
        // Настройка обработчиков событий
        updateButton.setOnAction(event -> loadReportData());
        exportButton.setOnAction(event -> exportToCsv());
        closeButton.setOnAction(event -> closeWindow());
    }
    
    /**
     * Устанавливает токен авторизации и создает сервис отчетов
     * @param authToken токен авторизации
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
        reportService = new ReportService(authToken);
    }
    
    /**
     * Устанавливает период для отчета
     * @param fromDate дата начала периода
     * @param toDate дата окончания периода
     */
    public void setReportPeriod(LocalDate fromDate, LocalDate toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        
        fromDatePicker.setValue(fromDate);
        toDatePicker.setValue(toDate);
    }
    
    /**
     * Загружает данные отчета
     */
    public void loadReportData() {
        if (reportService == null) {
            logger.error("Report service is not initialized");
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Сервис отчетов не инициализирован");
            return;
        }
        
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        
        if (from == null || to == null) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Выберите период для отчета");
            return;
        }
        
        if (from.isAfter(to)) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Дата начала должна быть раньше даты окончания");
            return;
        }
        
        pointsTable.setPlaceholder(new Label("Загрузка данных..."));
        pointsList.clear();
        pointsChart.getData().clear();
        
        reportService.getPointsReport(from, to)
                .thenAccept(data -> {
                    Platform.runLater(() -> {
                        updateReportView(data);
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        logger.error("Error loading points report", e);
                        showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось загрузить отчет: " + e.getMessage());
                        pointsTable.setPlaceholder(new Label("Ошибка загрузки данных"));
                    });
                    return null;
                });
    }
    
    /**
     * Обновляет отображение отчета с новыми данными
     * @param data данные отчета
     */
    private void updateReportView(PointsReportData data) {
        if (data == null || data.getRecords() == null) {
            pointsTable.setPlaceholder(new Label("Нет данных за выбранный период"));
            return;
        }
        
        // Загружаем данные в таблицу
        pointsList.clear();
        pointsList.addAll(data.getRecords());
        
        // Обновляем итоговые значения
        int totalPoints = data.getRecords().stream().mapToInt(PointsRecord::getPoints).sum();
        totalPointsLabel.setText(String.valueOf(totalPoints));
        totalOperationsLabel.setText(String.valueOf(data.getRecords().size()));
        
        // Обновляем график
        pointsChart.getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Баллы по дням");
        
        data.getPointsByDay().forEach((date, points) -> {
            series.getData().add(new XYChart.Data<>(date, points));
        });
        
        pointsChart.getData().add(series);
        
        // Сортируем даты на оси X
        List<String> sortedDates = new ArrayList<>(data.getPointsByDay().keySet());
        sortedDates.sort((d1, d2) -> {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate date1 = LocalDate.parse(d1, formatter);
                LocalDate date2 = LocalDate.parse(d2, formatter);
                return date1.compareTo(date2);
            } catch (Exception e) {
                return d1.compareTo(d2);
            }
        });
        
        dateAxis.getCategories().clear();
        dateAxis.setCategories(FXCollections.observableArrayList(sortedDates));
    }
    
    /**
     * Экспортирует отчет в CSV файл
     */
    private void exportToCsv() {
        if (reportService == null) {
            logger.error("Report service is not initialized");
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Сервис отчетов не инициализирован");
            return;
        }
        
        if (pointsList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Нет данных для экспорта");
            return;
        }
        
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить отчет");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV файлы (*.csv)", "*.csv"));
        
        String defaultFileName = "points_report_" + from.toString() + "_" + to.toString() + ".csv";
        fileChooser.setInitialFileName(defaultFileName);
        
        Stage stage = (Stage) exportButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            reportService.exportPointsReportToCsv(from, to, file.getAbsolutePath())
                    .thenAccept(success -> {
                        Platform.runLater(() -> {
                            if (success) {
                                showAlert(Alert.AlertType.INFORMATION, "Экспорт", 
                                        "Отчет успешно экспортирован в " + file.getAbsolutePath());
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                        "Не удалось экспортировать отчет");
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            logger.error("Error exporting points report", e);
                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                    "Не удалось экспортировать отчет: " + e.getMessage());
                        });
                        return null;
                    });
        }
    }
    
    /**
     * Закрывает окно отчета
     */
    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Показывает диалоговое окно с сообщением
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