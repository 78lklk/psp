package client.ui.admin.reports;

import client.service.ReportService;
import client.service.ReportService.PromotionRecord;
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
 * Контроллер для отчета по акциям
 */
public class PromotionsReportController {
    private static final Logger logger = LoggerFactory.getLogger(PromotionsReportController.class);
    
    // Компоненты UI
    @FXML private TableView<PromotionRecord> promotionsTable;
    @FXML private BarChart<String, Number> popularityChart;
    @FXML private PieChart effectivenessChart;
    @FXML private Button exportButton;
    @FXML private Label statusLabel;
    @FXML private Label periodLabel;
    
    // Сервисы
    private ReportService reportService;
    private String authToken;
    
    // Данные
    private LocalDate fromDate;
    private LocalDate toDate;
    private ObservableList<PromotionRecord> promotionsData = FXCollections.observableArrayList();
    
    /**
     * Инициализация контроллера
     */
    @FXML
    private void initialize() {
        // Настройка таблицы
        TableColumn<PromotionRecord, String> nameColumn = new TableColumn<>("Название");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        TableColumn<PromotionRecord, String> descriptionColumn = new TableColumn<>("Описание");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        TableColumn<PromotionRecord, String> startDateColumn = new TableColumn<>("Дата начала");
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        
        TableColumn<PromotionRecord, String> endDateColumn = new TableColumn<>("Дата окончания");
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        
        TableColumn<PromotionRecord, Integer> participantsColumn = new TableColumn<>("Участники");
        participantsColumn.setCellValueFactory(new PropertyValueFactory<>("participantsCount"));
        
        TableColumn<PromotionRecord, Double> effectivenessColumn = new TableColumn<>("Эффективность");
        effectivenessColumn.setCellValueFactory(new PropertyValueFactory<>("effectiveness"));
        
        promotionsTable.getColumns().addAll(nameColumn, descriptionColumn, startDateColumn, 
                                           endDateColumn, participantsColumn, effectivenessColumn);
        promotionsTable.setItems(promotionsData);
        
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
        
        reportService.getPromotionsReport(fromDate, toDate)
                .thenAccept(reportData -> {
                    Platform.runLater(() -> {
                        promotionsData.clear();
                        
                        // Загружаем данные в таблицу
                        promotionsData.addAll(reportData.getRecords());
                        
                        // Обновляем графики
                        updatePopularityChart(reportData.getPopularityByPromotion());
                        updateEffectivenessChart(reportData.getEffectivenessByType());
                        
                        statusLabel.setText("Загружено записей: " + promotionsData.size());
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        logger.error("Ошибка при загрузке отчета по акциям", e);
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
     * Обновляет график популярности акций
     * @param popularityByPromotion данные популярности по акциям
     */
    private void updatePopularityChart(Map<String, Integer> popularityByPromotion) {
        popularityChart.getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Участники");
        
        popularityByPromotion.forEach((name, count) -> {
            series.getData().add(new XYChart.Data<>(name, count));
        });
        
        popularityChart.getData().add(series);
    }
    
    /**
     * Обновляет круговую диаграмму эффективности
     * @param effectivenessByType данные эффективности по типам
     */
    private void updateEffectivenessChart(Map<String, Double> effectivenessByType) {
        effectivenessChart.getData().clear();
        
        effectivenessByType.forEach((type, value) -> {
            effectivenessChart.getData().add(new PieChart.Data(type, value));
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
        fileChooser.setInitialFileName("promotions_report_" + 
                fromDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" +
                toDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv");
        
        File file = fileChooser.showSaveDialog(promotionsTable.getScene().getWindow());
        
        if (file != null) {
            reportService.exportPromotionsReportToCsv(fromDate, toDate, file.getAbsolutePath())
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