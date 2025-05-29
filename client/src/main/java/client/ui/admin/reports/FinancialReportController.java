package client.ui.admin.reports;

import client.service.ReportService;
import client.service.ReportService.FinancialRecord;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
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
 * Контроллер для финансового отчета
 */
public class FinancialReportController {
    private static final Logger logger = LoggerFactory.getLogger(FinancialReportController.class);
    
    // Компоненты UI
    @FXML private TableView<FinancialRecord> financialTable;
    @FXML private BarChart<String, Number> revenueChart;
    @FXML private PieChart categoryChart;
    @FXML private Button exportButton;
    @FXML private Label statusLabel;
    @FXML private Label periodLabel;
    
    // Сервисы
    private ReportService reportService;
    private String authToken;
    
    // Данные
    private LocalDate fromDate;
    private LocalDate toDate;
    private ObservableList<FinancialRecord> financialData = FXCollections.observableArrayList();
    
    /**
     * Инициализация контроллера
     */
    @FXML
    private void initialize() {
        // Настройка таблицы
        TableColumn<FinancialRecord, String> dateColumn = new TableColumn<>("Дата");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        
        TableColumn<FinancialRecord, String> categoryColumn = new TableColumn<>("Категория");
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        
        TableColumn<FinancialRecord, Double> revenueColumn = new TableColumn<>("Доход");
        revenueColumn.setCellValueFactory(new PropertyValueFactory<>("revenue"));
        
        TableColumn<FinancialRecord, Double> expensesColumn = new TableColumn<>("Расходы");
        expensesColumn.setCellValueFactory(new PropertyValueFactory<>("expenses"));
        
        TableColumn<FinancialRecord, Double> profitColumn = new TableColumn<>("Прибыль");
        profitColumn.setCellValueFactory(new PropertyValueFactory<>("profit"));
        
        financialTable.getColumns().addAll(dateColumn, categoryColumn, revenueColumn, expensesColumn, profitColumn);
        financialTable.setItems(financialData);
        
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
        
        reportService.getFinancialReport(fromDate, toDate)
                .thenAccept(reportData -> {
                    Platform.runLater(() -> {
                        financialData.clear();
                        
                        // Загружаем данные в таблицу
                        financialData.addAll(reportData.getRecords());
                        
                        // Обновляем графики
                        updateRevenueChart(reportData.getRevenueByDay());
                        updateCategoryChart(reportData.getRevenueByCategory());
                        
                        statusLabel.setText("Загружено записей: " + financialData.size());
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        logger.error("Ошибка при загрузке финансового отчета", e);
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
     * Обновляет график доходов
     * @param revenueByDay данные по дням
     */
    private void updateRevenueChart(Map<String, Double> revenueByDay) {
        revenueChart.getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Доход по дням");
        
        revenueByDay.forEach((day, revenue) -> {
            series.getData().add(new XYChart.Data<>(day, revenue));
        });
        
        revenueChart.getData().add(series);
    }
    
    /**
     * Обновляет круговую диаграмму по категориям
     * @param revenueByCategory данные по категориям
     */
    private void updateCategoryChart(Map<String, Double> revenueByCategory) {
        categoryChart.getData().clear();
        
        revenueByCategory.forEach((category, revenue) -> {
            categoryChart.getData().add(new PieChart.Data(category, revenue));
        });
    }
    
    /**
     * Обработчик кнопки экспорта
     */
    private void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить отчет");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("financial_report_" + 
                fromDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" +
                toDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv");
        
        File file = fileChooser.showSaveDialog(financialTable.getScene().getWindow());
        
        if (file != null) {
            reportService.exportFinancialReportToCsv(fromDate, toDate, file.getAbsolutePath())
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