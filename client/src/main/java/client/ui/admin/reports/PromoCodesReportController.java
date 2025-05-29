package client.ui.admin.reports;

import client.service.ReportService;
import client.service.ReportService.PromoCodeRecord;
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
 * Контроллер для отчета по использованию промокодов
 */
public class PromoCodesReportController {
    private static final Logger logger = LoggerFactory.getLogger(PromoCodesReportController.class);
    
    // Компоненты UI
    @FXML private TableView<PromoCodeRecord> promoCodesTable;
    @FXML private BarChart<String, Number> usageChart;
    @FXML private PieChart distributionChart;
    @FXML private Button exportButton;
    @FXML private Label statusLabel;
    @FXML private Label periodLabel;
    
    // Сервисы
    private ReportService reportService;
    private String authToken;
    
    // Данные
    private LocalDate fromDate;
    private LocalDate toDate;
    private ObservableList<PromoCodeRecord> promoCodesData = FXCollections.observableArrayList();
    
    /**
     * Инициализация контроллера
     */
    @FXML
    private void initialize() {
        // Настройка таблицы
        TableColumn<PromoCodeRecord, String> codeColumn = new TableColumn<>("Промокод");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        
        TableColumn<PromoCodeRecord, String> descriptionColumn = new TableColumn<>("Описание");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        TableColumn<PromoCodeRecord, String> validFromColumn = new TableColumn<>("Действует с");
        validFromColumn.setCellValueFactory(new PropertyValueFactory<>("validFrom"));
        
        TableColumn<PromoCodeRecord, String> validToColumn = new TableColumn<>("Действует по");
        validToColumn.setCellValueFactory(new PropertyValueFactory<>("validTo"));
        
        TableColumn<PromoCodeRecord, Integer> usageColumn = new TableColumn<>("Использований");
        usageColumn.setCellValueFactory(new PropertyValueFactory<>("usageCount"));
        
        TableColumn<PromoCodeRecord, Double> discountColumn = new TableColumn<>("Скидка");
        discountColumn.setCellValueFactory(new PropertyValueFactory<>("discountValue"));
        
        promoCodesTable.getColumns().addAll(codeColumn, descriptionColumn, validFromColumn, 
                                           validToColumn, usageColumn, discountColumn);
        promoCodesTable.setItems(promoCodesData);
        
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
        
        reportService.getPromoCodesReport(fromDate, toDate)
                .thenAccept(reportData -> {
                    Platform.runLater(() -> {
                        promoCodesData.clear();
                        
                        // Загружаем данные в таблицу
                        promoCodesData.addAll(reportData.getRecords());
                        
                        // Обновляем графики
                        updateUsageChart(reportData.getUsageByCode());
                        updateDistributionChart(reportData.getDistributionByType());
                        
                        statusLabel.setText("Загружено записей: " + promoCodesData.size());
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        logger.error("Ошибка при загрузке отчета по промокодам", e);
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
     * Обновляет график использования промокодов
     * @param usageByCode данные использования по промокодам
     */
    private void updateUsageChart(Map<String, Integer> usageByCode) {
        usageChart.getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Использований");
        
        usageByCode.forEach((code, count) -> {
            series.getData().add(new XYChart.Data<>(code, count));
        });
        
        usageChart.getData().add(series);
    }
    
    /**
     * Обновляет круговую диаграмму распределения по типам
     * @param distributionByType данные распределения по типам
     */
    private void updateDistributionChart(Map<String, Integer> distributionByType) {
        distributionChart.getData().clear();
        
        distributionByType.forEach((type, count) -> {
            distributionChart.getData().add(new PieChart.Data(type, count));
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
        fileChooser.setInitialFileName("promocodes_report_" + 
                fromDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" +
                toDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv");
        
        File file = fileChooser.showSaveDialog(promoCodesTable.getScene().getWindow());
        
        if (file != null) {
            reportService.exportPromoCodesReportToCsv(fromDate, toDate, file.getAbsolutePath())
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