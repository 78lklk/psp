package client.ui.admin;

import client.service.PromotionService;
import client.service.PromotionService.PromotionStatistics;
import common.model.Promotion;
import common.model.PromoCode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Контроллер для управления акциями и промокодами
 */
public class PromotionsViewController {
    private static final Logger logger = LoggerFactory.getLogger(PromotionsViewController.class);
    
    // Таблица акций
    @FXML
    private TableView<Promotion> promotionsTable;
    
    @FXML
    private TableColumn<Promotion, Long> promoIdColumn;
    
    @FXML
    private TableColumn<Promotion, String> promoNameColumn;
    
    @FXML
    private TableColumn<Promotion, String> promoDescriptionColumn;
    
    @FXML
    private TableColumn<Promotion, String> promoStartDateColumn;
    
    @FXML
    private TableColumn<Promotion, String> promoEndDateColumn;
    
    @FXML
    private TableColumn<Promotion, Boolean> promoActiveColumn;
    
    // Поиск акций
    @FXML
    private TextField promotionSearchField;
    
    // Таблица промокодов
    @FXML
    private TableView<PromoCode> promoCodesTable;
    
    @FXML
    private TableColumn<PromoCode, Long> codeIdColumn;
    
    @FXML
    private TableColumn<PromoCode, String> codeValueColumn;
    
    @FXML
    private TableColumn<PromoCode, String> codeDescriptionColumn;
    
    @FXML
    private TableColumn<PromoCode, String> codeBonusColumn;
    
    @FXML
    private TableColumn<PromoCode, String> codeExpiryColumn;
    
    @FXML
    private TableColumn<PromoCode, Integer> codeUsesColumn;
    
    @FXML
    private TableColumn<PromoCode, Boolean> codeActiveColumn;
    
    // Поиск промокодов
    @FXML
    private TextField promoCodeSearchField;
    
    // Статистика
    @FXML
    private ComboBox<String> periodComboBox;
    
    @FXML
    private Pane promotionsChartPane;
    
    @FXML
    private Pane promoCodesChartPane;
    
    @FXML
    private Pane activityChartPane;
    
    @FXML
    private TableView<StatItem> statsTable;
    
    @FXML
    private TableColumn<StatItem, String> statNameColumn;
    
    @FXML
    private TableColumn<StatItem, String> statValueColumn;
    
    // Кнопки
    @FXML
    private Button addPromotionButton;
    
    @FXML
    private Button editPromotionButton;
    
    @FXML
    private Button deletePromotionButton;
    
    @FXML
    private Button addPromoCodeButton;
    
    @FXML
    private Button editPromoCodeButton;
    
    @FXML
    private Button deletePromoCodeButton;
    
    @FXML
    private Label statusLabel;
    
    private PromotionService promotionService;
    private ObservableList<Promotion> promotionsList = FXCollections.observableArrayList();
    private ObservableList<PromoCode> promoCodesList = FXCollections.observableArrayList();
    private ObservableList<StatItem> statsList = FXCollections.observableArrayList();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private String authToken;
    
    /**
     * Вспомогательный класс для отображения статистики
     */
    public static class StatItem {
        private final String name;
        private final String value;
        
        public StatItem(String name, String value) {
            this.name = name;
            this.value = value;
        }
        
        public String getName() {
            return name;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    /**
     * Инициализация после установки authToken
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
        logger.debug("Токен авторизации установлен для PromotionsViewController");
        try {
        promotionService = new PromotionService(authToken);
            logger.debug("Сервис акций успешно создан");
        loadData();
        } catch (Exception e) {
            logger.error("Ошибка при инициализации PromotionsViewController", e);
            Platform.runLater(() -> {
                statusLabel.setText("Ошибка инициализации. Проверьте соединение с сервером.");
            });
        }
    }
    
    @FXML
    private void initialize() {
        // Инициализация таблицы акций
        promoIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        promoNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        promoDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        // Форматирование дат
        promoStartDateColumn.setCellValueFactory(cellData -> {
            Promotion promotion = cellData.getValue();
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> promotion.getStartDate() != null ? 
                            promotion.getStartDate().format(dateFormatter) : "");
        });
        
        promoEndDateColumn.setCellValueFactory(cellData -> {
            Promotion promotion = cellData.getValue();
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> promotion.getEndDate() != null ? 
                            promotion.getEndDate().format(dateFormatter) : "");
        });
        
        // Активность акции
        promoActiveColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        
        // Настройка таблицы промокодов
        codeIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        codeValueColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        codeDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        // Бонус промокода (может быть баллы или процент)
        codeBonusColumn.setCellValueFactory(cellData -> {
            PromoCode promoCode = cellData.getValue();
            return javafx.beans.binding.Bindings.createStringBinding(() -> {
                if (promoCode.getBonusPoints() != null) {
                    return promoCode.getBonusPoints() + " баллов";
                } else if (promoCode.getDiscountPercent() != null) {
                    return promoCode.getDiscountPercent() + "%";
                } else {
                    return "";
                }
            });
        });
        
        // Срок действия промокода
        codeExpiryColumn.setCellValueFactory(cellData -> {
            PromoCode promoCode = cellData.getValue();
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> promoCode.getExpiryDate() != null ? 
                            promoCode.getExpiryDate().format(dateFormatter) : "");
        });
        
        // Использование промокода
        codeUsesColumn.setCellValueFactory(new PropertyValueFactory<>("usesCount"));
        
        // Активность промокода
        codeActiveColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        
        // Настройка таблицы статистики
        statNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        statValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        
        // Установка данных
        promotionsTable.setItems(promotionsList);
        promoCodesTable.setItems(promoCodesList);
        statsTable.setItems(statsList);
        
        // Период для статистики
        periodComboBox.getItems().addAll(
                "Текущий месяц", 
                "Предыдущий месяц", 
                "Текущий квартал", 
                "Текущий год", 
                "Все время");
        periodComboBox.setValue("Текущий месяц");
        
        // Кнопки активны только при выбранном элементе
        editPromotionButton.disableProperty().bind(
                promotionsTable.getSelectionModel().selectedItemProperty().isNull());
        deletePromotionButton.disableProperty().bind(
                promotionsTable.getSelectionModel().selectedItemProperty().isNull());
        
        editPromoCodeButton.disableProperty().bind(
                promoCodesTable.getSelectionModel().selectedItemProperty().isNull());
        deletePromoCodeButton.disableProperty().bind(
                promoCodesTable.getSelectionModel().selectedItemProperty().isNull());
        
        // Поиск по нажатию Enter
        promotionSearchField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handlePromotionSearch();
            }
        });
        
        promoCodeSearchField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handlePromoCodeSearch();
            }
        });
        
        // Двойной клик для редактирования
        promotionsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleEditPromotion();
            }
        });
        
        promoCodesTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleEditPromoCode();
            }
        });
    }
    
    /**
     * Загружает все данные
     */
    private void loadData() {
        logger.debug("Начало загрузки данных для экрана акций и промокодов");
        loadPromotions();
        loadPromoCodes();
        loadStatistics();
    }
    
    /**
     * Загружает список акций
     */
    private void loadPromotions() {
        statusLabel.setText("Загрузка акций...");
        promotionService.getAllPromotions()
                .thenAccept(promotions -> {
                    Platform.runLater(() -> {
                        promotionsList.clear();
                        promotionsList.addAll(promotions);
                        statusLabel.setText("Загружено акций: " + promotions.size());
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                "Не удалось загрузить список акций");
                        statusLabel.setText("Ошибка загрузки акций");
                    });
                    return null;
                });
    }
    
    /**
     * Загружает список промокодов
     */
    private void loadPromoCodes() {
        statusLabel.setText("Загрузка промокодов...");
        promotionService.getAllPromoCodes()
                .thenAccept(promoCodes -> {
                    Platform.runLater(() -> {
                        promoCodesList.clear();
                        promoCodesList.addAll(promoCodes);
                        statusLabel.setText("Загружено промокодов: " + promoCodes.size());
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                "Не удалось загрузить список промокодов");
                        statusLabel.setText("Ошибка загрузки промокодов");
                    });
                    return null;
                });
    }
    
    /**
     * Загружает статистику
     */
    private void loadStatistics() {
        statusLabel.setText("Загрузка статистики...");
        
        // Загрузка реальных данных статистики
        promotionService.getPromotionStatistics()
                .thenAccept(stats -> {
                    if (stats != null) {
                        Platform.runLater(() -> {
                            statsList.clear();
                                    
                            // Добавляем статистику из полученных данных
                            statsList.add(new StatItem("Всего акций", String.valueOf(stats.getTotalPromotions())));
                            statsList.add(new StatItem("Активных акций", String.valueOf(stats.getActivePromotions())));
                            statsList.add(new StatItem("Всего промокодов", String.valueOf(stats.getTotalPromoCodes())));
                            statsList.add(new StatItem("Использовано промокодов", String.valueOf(stats.getUsedPromoCodes())));
                            statsList.add(new StatItem("Конверсия промокодов", String.format("%.2f%%", stats.getPromoCodeConversion() * 100)));
                            statsList.add(new StatItem("Всего начислено баллов", String.valueOf(stats.getTotalBonusPoints())));
                            statsList.add(new StatItem("Среднее количество баллов", String.format("%.2f", stats.getAverageBonusPoints())));
                            
                            // Обновляем графики
                            updateCharts(stats);
                            
                            statusLabel.setText("Статистика загружена");
                        });
                    } else {
                        // Если статистика не загрузилась, используем данные из таблиц
        Platform.runLater(() -> {
            statsList.clear();
                        
                        // Добавляем статистику из акций
                        statsList.add(new StatItem("Всего акций", String.valueOf(promotionsList.size())));
                        statsList.add(new StatItem("Активных акций", String.valueOf(promotionsList.stream()
                                .filter(Promotion::isActive).count())));
                                
                        // Добавляем статистику из промокодов
                        statsList.add(new StatItem("Всего промокодов", String.valueOf(promoCodesList.size())));
                        statsList.add(new StatItem("Активных промокодов", String.valueOf(promoCodesList.stream()
                                .filter(PromoCode::isActive).count())));
                        
                            // Инициализируем пустые графики
                            initializeEmptyCharts();
                            
                            statusLabel.setText("Ограниченная статистика (ошибка загрузки полных данных)");
                        });
                        }
                })
                .exceptionally(e -> {
                    logger.error("Ошибка при загрузке статистики", e);
                    Platform.runLater(() -> {
                        statsList.clear();
                        
                        // Добавляем базовую статистику из имеющихся данных
                        statsList.add(new StatItem("Всего акций", String.valueOf(promotionsList.size())));
                        statsList.add(new StatItem("Активных акций", String.valueOf(promotionsList.stream()
                                .filter(Promotion::isActive).count())));
                        statsList.add(new StatItem("Всего промокодов", String.valueOf(promoCodesList.size())));
                        statsList.add(new StatItem("Активных промокодов", String.valueOf(promoCodesList.stream()
                                .filter(PromoCode::isActive).count())));
                        
                        // Инициализируем пустые графики
                        initializeEmptyCharts();
                        
                        statusLabel.setText("Ошибка загрузки статистики: " + e.getMessage());
                        showAlert(Alert.AlertType.WARNING, "Внимание", 
                                "Не удалось загрузить полную статистику. Отображаются ограниченные данные.");
                    });
                    return null;
                });
    }
    
    /**
     * Обновляет графики статистическими данными
     * @param stats статистические данные
     */
    private void updateCharts(PromotionStatistics stats) {
        // Обновление графиков производится на основе полученных данных
        // Пример реализации с использованием JavaFX Charts
        // Примечание: фактическая реализация будет зависеть от структуры данных в PromotionStatistics
        
        // Пример для графика использования акций
        if (stats.getPromotionUsageData() != null && !stats.getPromotionUsageData().isEmpty()) {
            javafx.scene.chart.PieChart promotionsChart = new javafx.scene.chart.PieChart();
            
            stats.getPromotionUsageData().forEach((name, count) -> {
                promotionsChart.getData().add(new javafx.scene.chart.PieChart.Data(name, count));
            });
            
            // Очищаем и добавляем новый график
            promotionsChartPane.getChildren().clear();
            promotionsChartPane.getChildren().add(promotionsChart);
            promotionsChart.prefWidthProperty().bind(promotionsChartPane.widthProperty());
            promotionsChart.prefHeightProperty().bind(promotionsChartPane.heightProperty());
        } else {
            initializeEmptyCharts();
        }
        
        // Пример для графика использования промокодов
        if (stats.getPromoCodeUsageData() != null && !stats.getPromoCodeUsageData().isEmpty()) {
            javafx.scene.chart.PieChart promoCodesChart = new javafx.scene.chart.PieChart();
            
            stats.getPromoCodeUsageData().forEach((code, count) -> {
                promoCodesChart.getData().add(new javafx.scene.chart.PieChart.Data(code, count));
            });
            
            // Очищаем и добавляем новый график
            promoCodesChartPane.getChildren().clear();
            promoCodesChartPane.getChildren().add(promoCodesChart);
            promoCodesChart.prefWidthProperty().bind(promoCodesChartPane.widthProperty());
            promoCodesChart.prefHeightProperty().bind(promoCodesChartPane.heightProperty());
        } else {
            initializeEmptyCharts();
        }
        
        // Пример для графика активности по дням
        if (stats.getDailyActivityData() != null && !stats.getDailyActivityData().isEmpty()) {
            javafx.scene.chart.BarChart<String, Number> activityChart = 
                    new javafx.scene.chart.BarChart<>(
                            new javafx.scene.chart.CategoryAxis(), 
                            new javafx.scene.chart.NumberAxis());
            
            javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
            series.setName("Активность");
            
            stats.getDailyActivityData().forEach((day, count) -> {
                series.getData().add(new javafx.scene.chart.XYChart.Data<>(day, count));
            });
            
            activityChart.getData().add(series);
            
            // Очищаем и добавляем новый график
            activityChartPane.getChildren().clear();
            activityChartPane.getChildren().add(activityChart);
            activityChart.prefWidthProperty().bind(activityChartPane.widthProperty());
            activityChart.prefHeightProperty().bind(activityChartPane.heightProperty());
        } else {
            initializeEmptyCharts();
        }
    }
    
    /**
     * Гарантирует, что графики правильно инициализированы даже при отсутствии данных
     */
    private void initializeEmptyCharts() {
        logger.debug("Инициализация пустых графиков для экрана акций");
        
        // Инициализация графика использования акций
        javafx.scene.chart.PieChart promotionsChart = new javafx.scene.chart.PieChart();
        promotionsChart.setTitle("Нет данных по акциям");
        promotionsChart.getData().add(new javafx.scene.chart.PieChart.Data("Нет данных", 1));
        promotionsChartPane.getChildren().clear();
        promotionsChartPane.getChildren().add(promotionsChart);
        promotionsChart.prefWidthProperty().bind(promotionsChartPane.widthProperty());
        promotionsChart.prefHeightProperty().bind(promotionsChartPane.heightProperty());
        
        // Инициализация графика использования промокодов
        javafx.scene.chart.PieChart promoCodesChart = new javafx.scene.chart.PieChart();
        promoCodesChart.setTitle("Нет данных по промокодам");
        promoCodesChart.getData().add(new javafx.scene.chart.PieChart.Data("Нет данных", 1));
        promoCodesChartPane.getChildren().clear();
        promoCodesChartPane.getChildren().add(promoCodesChart);
        promoCodesChart.prefWidthProperty().bind(promoCodesChartPane.widthProperty());
        promoCodesChart.prefHeightProperty().bind(promoCodesChartPane.heightProperty());
        
        // Инициализация графика активности
        javafx.scene.chart.BarChart<String, Number> activityChart = 
                new javafx.scene.chart.BarChart<>(
                        new javafx.scene.chart.CategoryAxis(), 
                        new javafx.scene.chart.NumberAxis());
        activityChart.setTitle("Нет данных по активности");
        activityChartPane.getChildren().clear();
        activityChartPane.getChildren().add(activityChart);
        activityChart.prefWidthProperty().bind(activityChartPane.widthProperty());
        activityChart.prefHeightProperty().bind(activityChartPane.heightProperty());
    }
    
    // Обработчики событий
    
    @FXML
    private void handleClose() {
        Stage stage = (Stage) promotionsTable.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    private void handleAddPromotion() {
        Promotion newPromotion = new Promotion();
        newPromotion.setStartDate(LocalDate.now());
        newPromotion.setEndDate(LocalDate.now().plusMonths(1));
        newPromotion.setActive(true);
        
        PromotionDialog dialog = new PromotionDialog(newPromotion);
        Optional<Promotion> result = dialog.showAndWait();
        
        result.ifPresent(promotion -> {
            statusLabel.setText("Создание акции...");
            
            try {
            promotionService.createPromotion(promotion)
                    .thenAccept(createdPromotion -> {
                        Platform.runLater(() -> {
                            if (createdPromotion != null) {
                                promotionsList.add(createdPromotion);
                                promotionsTable.getSelectionModel().select(createdPromotion);
                                statusLabel.setText("Акция успешно создана");
                                
                                // Обновляем статистику
                                loadStatistics();
                            } else {
                                    logger.error("Не удалось создать акцию, сервер вернул null");
                                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                            "Не удалось создать акцию. Проверьте соединение с сервером.");
                                statusLabel.setText("Ошибка создания акции");
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                                logger.error("Исключение при создании акции", e);
                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                    "Не удалось создать акцию: " + e.getMessage());
                            statusLabel.setText("Ошибка создания акции");
                        });
                        return null;
                    });
            } catch (Exception e) {
                logger.error("Ошибка при формировании запроса на создание акции", e);
                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                        "Не удалось отправить запрос на создание акции: " + e.getMessage());
                statusLabel.setText("Ошибка создания акции");
            }
        });
    }
    
    @FXML
    private void handleEditPromotion() {
        Promotion selectedPromotion = promotionsTable.getSelectionModel().getSelectedItem();
        if (selectedPromotion == null) {
            return;
        }
        
        // Создаем копию для редактирования
        Promotion editedPromotion = new Promotion();
        editedPromotion.setId(selectedPromotion.getId());
        editedPromotion.setName(selectedPromotion.getName());
        editedPromotion.setDescription(selectedPromotion.getDescription());
        editedPromotion.setStartDate(selectedPromotion.getStartDate());
        editedPromotion.setEndDate(selectedPromotion.getEndDate());
        editedPromotion.setActive(selectedPromotion.isActive());
        editedPromotion.setBonusPercent(selectedPromotion.getBonusPercent());
        editedPromotion.setBonusPoints(selectedPromotion.getBonusPoints());
        
        PromotionDialog dialog = new PromotionDialog(editedPromotion);
        Optional<Promotion> result = dialog.showAndWait();
        
        result.ifPresent(promotion -> {
            statusLabel.setText("Обновление акции...");
            
            try {
            promotionService.updatePromotion(promotion)
                    .thenAccept(updatedPromotion -> {
                        Platform.runLater(() -> {
                            if (updatedPromotion != null) {
                                // Находим индекс старой акции и заменяем ее обновленной
                                int index = promotionsList.indexOf(selectedPromotion);
                                if (index >= 0) {
                                    promotionsList.set(index, updatedPromotion);
                                    promotionsTable.getSelectionModel().select(index);
                                }
                                statusLabel.setText("Акция успешно обновлена");
                                
                                // Обновляем статистику
                                loadStatistics();
                            } else {
                                    logger.error("Не удалось обновить акцию, сервер вернул null");
                                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                            "Не удалось обновить акцию. Проверьте соединение с сервером.");
                                statusLabel.setText("Ошибка обновления акции");
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                                logger.error("Исключение при обновлении акции", e);
                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                    "Не удалось обновить акцию: " + e.getMessage());
                            statusLabel.setText("Ошибка обновления акции");
                        });
                        return null;
                    });
            } catch (Exception e) {
                logger.error("Ошибка при формировании запроса на обновление акции", e);
                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                        "Не удалось отправить запрос на обновление акции: " + e.getMessage());
                statusLabel.setText("Ошибка обновления акции");
            }
        });
    }
    
    @FXML
    private void handleDeletePromotion() {
        Promotion selectedPromotion = promotionsTable.getSelectionModel().getSelectedItem();
        if (selectedPromotion == null) {
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Удаление акции");
        alert.setContentText("Вы уверены, что хотите удалить акцию \"" + 
                selectedPromotion.getName() + "\"?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            statusLabel.setText("Удаление акции...");
            
            promotionService.deletePromotion(selectedPromotion.getId())
                    .thenAccept(success -> {
                        Platform.runLater(() -> {
                            if (success) {
                                promotionsList.remove(selectedPromotion);
                                statusLabel.setText("Акция успешно удалена");
                                
                                // Обновляем статистику
                                loadStatistics();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                        "Не удалось удалить акцию");
                                statusLabel.setText("Ошибка удаления акции");
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                    "Не удалось удалить акцию: " + e.getMessage());
                            statusLabel.setText("Ошибка удаления акции");
                        });
                        return null;
                    });
        }
    }
    
    @FXML
    private void handlePromotionSearch() {
        String searchTerm = promotionSearchField.getText().toLowerCase().trim();
        if (searchTerm.isEmpty()) {
            loadPromotions();
            return;
        }
        
        statusLabel.setText("Поиск акций...");
        
        // Выполняем поиск в уже загруженных данных
        promotionService.getAllPromotions()
                .thenAccept(allPromotions -> {
                    List<Promotion> filteredPromotions = allPromotions.stream()
                            .filter(promotion -> 
                                    (promotion.getName() != null && 
                                            promotion.getName().toLowerCase().contains(searchTerm)) ||
                                    (promotion.getDescription() != null && 
                                            promotion.getDescription().toLowerCase().contains(searchTerm)))
                            .collect(Collectors.toList());
                    
                    Platform.runLater(() -> {
                        promotionsList.clear();
                        promotionsList.addAll(filteredPromotions);
                        statusLabel.setText("Найдено акций: " + filteredPromotions.size());
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                "Не удалось выполнить поиск акций");
                        statusLabel.setText("Ошибка поиска акций");
                    });
                    return null;
                });
    }
    
    @FXML
    private void handleAddPromoCode() {
        PromoCode newPromoCode = new PromoCode();
        newPromoCode.setActive(true);
        newPromoCode.setExpiryDate(LocalDate.now().plusMonths(1));
        
        PromoCodeDialog dialog = new PromoCodeDialog(newPromoCode);
        Optional<PromoCode> result = dialog.showAndWait();
        
        result.ifPresent(promoCode -> {
            statusLabel.setText("Создание промокода...");
            
            try {
            promotionService.createPromoCode(promoCode)
                    .thenAccept(createdPromoCode -> {
                        Platform.runLater(() -> {
                            if (createdPromoCode != null) {
                                promoCodesList.add(createdPromoCode);
                                promoCodesTable.getSelectionModel().select(createdPromoCode);
                                statusLabel.setText("Промокод успешно создан");
                                
                                // Обновляем статистику
                                loadStatistics();
                            } else {
                                    logger.error("Не удалось создать промокод, сервер вернул null");
                                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                            "Не удалось создать промокод. Проверьте соединение с сервером.");
                                statusLabel.setText("Ошибка создания промокода");
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                                logger.error("Исключение при создании промокода", e);
                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                    "Не удалось создать промокод: " + e.getMessage());
                            statusLabel.setText("Ошибка создания промокода");
                        });
                        return null;
                    });
            } catch (Exception e) {
                logger.error("Ошибка при формировании запроса на создание промокода", e);
                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                        "Не удалось отправить запрос на создание промокода: " + e.getMessage());
                statusLabel.setText("Ошибка создания промокода");
            }
        });
    }
    
    @FXML
    private void handleEditPromoCode() {
        PromoCode selectedPromoCode = promoCodesTable.getSelectionModel().getSelectedItem();
        if (selectedPromoCode == null) {
            return;
        }
        
        // Создаем копию для редактирования
        PromoCode editedPromoCode = new PromoCode();
        editedPromoCode.setId(selectedPromoCode.getId());
        editedPromoCode.setCode(selectedPromoCode.getCode());
        editedPromoCode.setDescription(selectedPromoCode.getDescription());
        editedPromoCode.setBonusPoints(selectedPromoCode.getBonusPoints());
        editedPromoCode.setDiscountPercent(selectedPromoCode.getDiscountPercent());
        editedPromoCode.setExpiryDate(selectedPromoCode.getExpiryDate());
        editedPromoCode.setActive(selectedPromoCode.isActive());
        editedPromoCode.setUsesLimit(selectedPromoCode.getUsesLimit());
        editedPromoCode.setUsesCount(selectedPromoCode.getUsesCount());
        
        PromoCodeDialog dialog = new PromoCodeDialog(editedPromoCode);
        Optional<PromoCode> result = dialog.showAndWait();
        
        result.ifPresent(promoCode -> {
            statusLabel.setText("Обновление промокода...");
            
            try {
            promotionService.updatePromoCode(promoCode)
                    .thenAccept(updatedPromoCode -> {
                        Platform.runLater(() -> {
                            if (updatedPromoCode != null) {
                                // Находим индекс старого промокода и заменяем его обновленным
                                int index = promoCodesList.indexOf(selectedPromoCode);
                                if (index >= 0) {
                                    promoCodesList.set(index, updatedPromoCode);
                                    promoCodesTable.getSelectionModel().select(index);
                                }
                                statusLabel.setText("Промокод успешно обновлен");
                                
                                // Обновляем статистику
                                loadStatistics();
                            } else {
                                    logger.error("Не удалось обновить промокод, сервер вернул null");
                                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                            "Не удалось обновить промокод. Проверьте соединение с сервером.");
                                statusLabel.setText("Ошибка обновления промокода");
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                                logger.error("Исключение при обновлении промокода", e);
                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                    "Не удалось обновить промокод: " + e.getMessage());
                            statusLabel.setText("Ошибка обновления промокода");
                        });
                        return null;
                    });
            } catch (Exception e) {
                logger.error("Ошибка при формировании запроса на обновление промокода", e);
                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                        "Не удалось отправить запрос на обновление промокода: " + e.getMessage());
                statusLabel.setText("Ошибка обновления промокода");
            }
        });
    }
    
    @FXML
    private void handleDeletePromoCode() {
        PromoCode selectedPromoCode = promoCodesTable.getSelectionModel().getSelectedItem();
        if (selectedPromoCode == null) {
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Удаление промокода");
        alert.setContentText("Вы уверены, что хотите удалить промокод \"" + 
                selectedPromoCode.getCode() + "\"?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            statusLabel.setText("Удаление промокода...");
            
            promotionService.deletePromoCode(selectedPromoCode.getId())
                    .thenAccept(success -> {
                        Platform.runLater(() -> {
                            if (success) {
                                promoCodesList.remove(selectedPromoCode);
                                statusLabel.setText("Промокод успешно удален");
                                
                                // Обновляем статистику
                                loadStatistics();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                        "Не удалось удалить промокод");
                                statusLabel.setText("Ошибка удаления промокода");
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                    "Не удалось удалить промокод: " + e.getMessage());
                            statusLabel.setText("Ошибка удаления промокода");
                        });
                        return null;
                    });
        }
    }
    
    @FXML
    private void handlePromoCodeSearch() {
        String searchTerm = promoCodeSearchField.getText().toLowerCase().trim();
        if (searchTerm.isEmpty()) {
            loadPromoCodes();
            return;
        }
        
        statusLabel.setText("Поиск промокодов...");
        
        // Выполняем поиск в уже загруженных данных
        promotionService.getAllPromoCodes()
                .thenAccept(allPromoCodes -> {
                    List<PromoCode> filteredPromoCodes = allPromoCodes.stream()
                            .filter(promoCode -> 
                                    (promoCode.getCode() != null && 
                                            promoCode.getCode().toLowerCase().contains(searchTerm)) ||
                                    (promoCode.getDescription() != null && 
                                            promoCode.getDescription().toLowerCase().contains(searchTerm)))
                            .collect(Collectors.toList());
                    
                    Platform.runLater(() -> {
                        promoCodesList.clear();
                        promoCodesList.addAll(filteredPromoCodes);
                        statusLabel.setText("Найдено промокодов: " + filteredPromoCodes.size());
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        logger.error("Ошибка при поиске промокодов", e);
                        showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                "Не удалось выполнить поиск промокодов: " + e.getMessage());
                        statusLabel.setText("Ошибка поиска промокодов");
                    });
                    return null;
                });
    }
    
    @FXML
    private void handleRefreshStats() {
        loadStatistics();
        statusLabel.setText("Статистика обновлена");
    }
    
    /**
     * Показывает диалоговое окно
     * @param type тип окна
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