package client.ui;

import client.MainApp;
import client.model.ProductItem;
import client.model.Session;
import client.service.CardService;
import client.service.ScheduleService;
import client.service.SessionService;
import client.ui.admin.BackupViewController;
import client.ui.admin.PromotionsViewController;
import client.ui.admin.SettingsViewController;
import client.ui.admin.UsersViewController;
import client.ui.admin.reports.FinancialReportController;
import client.ui.admin.reports.PointsReportController;
import client.ui.admin.reports.PromoCodesReportController;
import client.ui.admin.reports.PromotionsReportController;
import client.ui.admin.reports.UserActivityReportController;
import common.dto.AuthResponse;
import common.model.Card;
import common.model.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.geometry.Pos;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Контроллер главного экрана приложения
 */
public class MainViewController {
    private static final Logger logger = LoggerFactory.getLogger(MainViewController.class);
    
    @FXML
    private Label welcomeLabel;
    
    @FXML
    private TableView<Card> cardsTable;
    
    @FXML
    private TableColumn<Card, String> numberColumn;
    
    @FXML
    private TableColumn<Card, Integer> pointsColumn;
    
    @FXML
    private TableColumn<Card, String> tierColumn;
    
    @FXML
    private Button addPointsButton;
    
    @FXML
    private Button deductPointsButton;
    
    @FXML
    private Button createCardButton;
    
    @FXML
    private Button deleteCardButton;
    
    @FXML
    private Button applyPromoCodeButton;
    
    @FXML
    private Button viewMySessionHistoryButton;
    
    // Элементы для администратора
    @FXML
    private VBox adminSection;
    
    @FXML
    private VBox staffSection;
    
    @FXML
    private Button manageUsersButton;
    
    @FXML
    private Button managePromotionsButton;
    
    @FXML
    private Button systemSettingsButton;
    
    @FXML
    private Button viewReportsButton;
    
    @FXML
    private Button backupButton;
    
    @FXML
    private Button startSessionButton;
    
    @FXML
    private Button endSessionButton;
    
    @FXML
    private Button viewSessionHistoryButton;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Button productSalesButton;
    
    @FXML
    private Button scheduleButton;
    
    private MainApp mainApp;
    private AuthResponse authResponse;
    private User currentUser;
    private CardService cardService;
    private SessionService sessionService;
    private ScheduleService scheduleService;
    private ObservableList<Card> cardsList = FXCollections.observableArrayList();
    private String authToken;
    
    /**
     * Инициализация контроллера
     */
    @FXML
    private void initialize() {
        // Настройка таблицы карт
        numberColumn.setCellValueFactory(new PropertyValueFactory<>("number"));
        pointsColumn.setCellValueFactory(new PropertyValueFactory<>("points"));
        tierColumn.setCellValueFactory(cellData -> {
            Card card = cellData.getValue();
            return card.getTier() != null 
                    ? javafx.beans.binding.Bindings.createStringBinding(() -> card.getTier().getName())
                    : javafx.beans.binding.Bindings.createStringBinding(() -> "Нет");
        });
        
        cardsTable.setItems(cardsList);
        
        // Кнопки активны только при выбранной карте
        addPointsButton.disableProperty().bind(cardsTable.getSelectionModel().selectedItemProperty().isNull());
        deductPointsButton.disableProperty().bind(cardsTable.getSelectionModel().selectedItemProperty().isNull());
        deleteCardButton.disableProperty().bind(cardsTable.getSelectionModel().selectedItemProperty().isNull());
        applyPromoCodeButton.disableProperty().bind(cardsTable.getSelectionModel().selectedItemProperty().isNull());
        viewMySessionHistoryButton.disableProperty().bind(cardsTable.getSelectionModel().selectedItemProperty().isNull());
        
        // Настройка кнопок администратора
        if (manageUsersButton != null) {
            manageUsersButton.setOnAction(event -> handleManageUsers());
            managePromotionsButton.setOnAction(event -> handleManagePromotions());
            systemSettingsButton.setOnAction(event -> handleSystemSettings());
            viewReportsButton.setOnAction(event -> handleViewReports());
            backupButton.setOnAction(event -> handleBackup());
        }
    }
    
    /**
     * Инициализация после установки authResponse
     */
    public void init() {
        currentUser = authResponse.getUser();
        cardService = new CardService(authResponse.getToken());
        sessionService = new SessionService(authResponse.getToken());
        scheduleService = new ScheduleService(authResponse.getToken());
        
        // Сохраняем токен для других сервисов
        this.authToken = authResponse.getToken();
        
        // Настройка приветствия
        welcomeLabel.setText("Добро пожаловать, " + currentUser.getLogin() + "!");
        
        // Определение роли пользователя
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        boolean isAdmin = "ADMIN".equals(roleName);
        boolean isManager = "MANAGER".equals(roleName);
        boolean isStaff = "STAFF".equals(roleName);
        boolean isClient = "CLIENT".equals(roleName);
        
        // Настройка UI в зависимости от роли
        configureUIForRole(isAdmin, isManager, isStaff, isClient);
        
        // Загрузка карт пользователя
        loadCards();
    }
    
    /**
     * Настраивает UI в зависимости от роли пользователя
     */
    private void configureUIForRole(boolean isAdmin, boolean isManager, boolean isStaff, boolean isClient) {
        // Включение панели администратора для ролей ADMIN и MANAGER
        if (adminSection != null) {
            boolean showAdminSection = isAdmin || isManager;
            adminSection.setVisible(showAdminSection);
            adminSection.setManaged(showAdminSection);
            
            // Настройка доступных функций по ролям
            if (manageUsersButton != null) {
                manageUsersButton.setVisible(isAdmin);
                manageUsersButton.setManaged(isAdmin);
            }
            
            if (systemSettingsButton != null) {
                // Показываем кнопку настроек только для администраторов
                systemSettingsButton.setVisible(isAdmin);
                systemSettingsButton.setManaged(isAdmin);
            }
            
            if (backupButton != null) {
                backupButton.setVisible(false);
                backupButton.setManaged(false);
            }
        }
        
        // Включение панели сотрудника для роли STAFF
        if (staffSection != null) {
            staffSection.setVisible(isStaff);
            staffSection.setManaged(isStaff);
            
            // Настройка обработчиков для кнопок сотрудника
            if (startSessionButton != null) {
                startSessionButton.setOnAction(event -> handleStartSession());
                startSessionButton.setVisible(isStaff);
                startSessionButton.setManaged(isStaff);
            }
            
            if (endSessionButton != null) {
                endSessionButton.setOnAction(event -> handleEndSession());
                endSessionButton.setVisible(isStaff);
                endSessionButton.setManaged(isStaff);
            }
            
            if (viewSessionHistoryButton != null) {
                viewSessionHistoryButton.setOnAction(event -> handleViewSessionHistory());
                viewSessionHistoryButton.setVisible(isStaff || isAdmin || isManager);
                viewSessionHistoryButton.setManaged(isStaff || isAdmin || isManager);
            }
            
            if (productSalesButton != null) {
                productSalesButton.setOnAction(event -> handleProductSales());
                productSalesButton.setVisible(isStaff);
                productSalesButton.setManaged(isStaff);
            }
            
            if (scheduleButton != null) {
                scheduleButton.setOnAction(event -> handleSchedule());
                scheduleButton.setVisible(isStaff || isAdmin || isManager);
                scheduleButton.setManaged(isStaff || isAdmin || isManager);
            }
        }
        
        // Настройка кнопок для клиентов
        if (isClient) {
            // Клиент не может назначать себе баллы
            addPointsButton.setVisible(false);
            addPointsButton.setManaged(false);
            deductPointsButton.setVisible(false);
            deductPointsButton.setManaged(false);
            
            // Клиент не может создавать/удалять карты
            if (createCardButton != null) {
                createCardButton.setVisible(false);
                createCardButton.setManaged(false);
            }
            if (deleteCardButton != null) {
                deleteCardButton.setVisible(false);
                deleteCardButton.setManaged(false);
            }
            
            // Клиент не может управлять сессиями (только персонал)
            if (startSessionButton != null) {
                startSessionButton.setVisible(false);
                startSessionButton.setManaged(false);
            }
            if (endSessionButton != null) {
                endSessionButton.setVisible(false);
                endSessionButton.setManaged(false);
            }
            
            // Скрываем всю секцию персонала для клиентов
            if (staffSection != null) {
                staffSection.setVisible(false);
                staffSection.setManaged(false);
            }
            
            // Добавляем кнопку применения промокода для клиентов
            if (applyPromoCodeButton != null) {
                applyPromoCodeButton.setVisible(true);
                applyPromoCodeButton.setManaged(true);
                applyPromoCodeButton.setOnAction(event -> handleApplyPromoCode());
            }
            
            // Добавляем кнопку просмотра истории сессий для клиентов
            if (viewMySessionHistoryButton != null) {
                viewMySessionHistoryButton.setVisible(true);
                viewMySessionHistoryButton.setManaged(true);
            }
        } else {
            // Скрываем кнопки клиента для персонала
            if (applyPromoCodeButton != null) {
                applyPromoCodeButton.setVisible(false);
                applyPromoCodeButton.setManaged(false);
            }
            if (viewMySessionHistoryButton != null) {
                viewMySessionHistoryButton.setVisible(false);
                viewMySessionHistoryButton.setManaged(false);
            }
        }
    }
    
    /**
     * Загружает карты пользователя или все карты для администратора/менеджера
     */
    private void loadCards() {
        if (cardService == null) {
            logger.error("Card service is not initialized");
            return;
        }
        
        // Очищаем список карт
        cardsList.clear();
        
        // Определение роли пользователя
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        boolean isAdmin = "ADMIN".equals(roleName);
        boolean isManager = "MANAGER".equals(roleName);
        boolean isStaff = "STAFF".equals(roleName);
        
        logger.debug("Loading cards for current user (role: {})", roleName);
        
        // Для администраторов, менеджеров и персонала загружаем все карты
        if (isAdmin || isManager || isStaff) {
            statusLabel.setText("Загрузка списка карт...");
            cardService.getAllCards()
                    .thenAccept(cards -> {
                        Platform.runLater(() -> {
                            cardsList.addAll(cards);
                            logger.debug("Loaded {} cards for user with role {}", cards.size(), roleName);
                            statusLabel.setText("Загружено " + cards.size() + " карт");
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            logger.error("Error loading all cards", e);
                            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось загрузить список карт: " + e.getMessage());
                            statusLabel.setText("Ошибка загрузки карт");
                        });
                        return null;
                    });
        } else {
            // Для клиентов загружаем только их карты
            statusLabel.setText("Загрузка ваших карт...");
            cardService.getUserCards(currentUser.getId())
                    .thenAccept(cards -> {
                        Platform.runLater(() -> {
                            cardsList.addAll(cards);
                            logger.debug("Loaded {} cards for client user {}", cards.size(), currentUser.getId());
                            statusLabel.setText("Загружено " + cards.size() + " ваших карт");
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            logger.error("Error loading user cards", e);
                            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось загрузить ваши карты: " + e.getMessage());
                            statusLabel.setText("Ошибка загрузки карт");
                        });
                        return null;
                    });
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки создания карты
     */
    @FXML
    private void handleCreateCard() {
        try {
            // Создаем новую карту для текущего пользователя
            Card newCard = new Card();
            newCard.setUserId(currentUser.getId());
            newCard.setCardNumber(generateCardNumber());
            
            cardService.createCard(newCard)
                    .thenAccept(card -> {
                        Platform.runLater(() -> {
                            if (card != null) {
                                cardsList.add(card);
                                cardsTable.getSelectionModel().select(card);
                                showAlert(Alert.AlertType.INFORMATION, "Создание карты", 
                                        "Новая карта успешно создана: " + card.getCardNumber());
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                        "Не удалось создать новую карту");
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            logger.error("Ошибка при создании карты", e);
                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                    "Не удалось создать новую карту: " + e.getMessage());
                        });
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Ошибка при создании карты", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                    "Не удалось создать новую карту: " + e.getMessage());
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки начисления баллов
     */
    @FXML
    private void handleAddPoints() {
        Card selectedCard = cardsTable.getSelectionModel().getSelectedItem();
        if (selectedCard == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Выберите карту для начисления баллов");
            return;
        }
        
        // Создаем диалоговое окно ввода количества баллов
        TextInputDialog dialog = new TextInputDialog("100");
        dialog.setTitle("Начисление баллов");
        dialog.setHeaderText("Начисление баллов на карту " + selectedCard.getNumber());
        dialog.setContentText("Введите количество баллов:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int points = Integer.parseInt(result.get());
                
                if (points <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Внимание", "Количество баллов должно быть положительным числом");
                    return;
                }
                
                // Запрашиваем описание операции
                TextInputDialog descDialog = new TextInputDialog("Пополнение");
                descDialog.setTitle("Описание операции");
                descDialog.setHeaderText("Укажите причину начисления баллов");
                descDialog.setContentText("Описание:");
                
                Optional<String> descResult = descDialog.showAndWait();
                String description = descResult.orElse("Пополнение");
                
                // Показываем индикатор загрузки
                Alert loadingAlert = showNonBlockingAlert(Alert.AlertType.INFORMATION, "Обработка", "Выполняется начисление баллов...");
                
                cardService.addPoints(selectedCard.getId(), points, description)
                        .thenAccept(updatedCard -> {
                            Platform.runLater(() -> {
                                // Закрываем индикатор загрузки
                                loadingAlert.close();
                                
                                if (updatedCard != null) {
                                    // Обновляем карту в списке
                                    updateCardInList(updatedCard);
                                    showAlert(Alert.AlertType.INFORMATION, "Начисление баллов", 
                                            "На карту успешно начислено " + points + " баллов");
                                } else {
                                    showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                            "Не удалось начислить баллы на карту. Проверьте соединение с сервером.");
                                }
                            });
                        })
                        .exceptionally(e -> {
                            Platform.runLater(() -> {
                                // Закрываем индикатор загрузки
                                loadingAlert.close();
                                
                                logger.error("Ошибка при начислении баллов", e);
                                
                                // Показываем более информативное сообщение об ошибке
                                String errorMessage = e.getMessage();
                                if (e.getCause() != null) {
                                    errorMessage = e.getCause().getMessage();
                                }
                                
                                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                        "Не удалось начислить баллы на карту: " + errorMessage);
                            });
                            return null;
                        });
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Введите корректное число баллов");
            }
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки списания баллов
     */
    @FXML
    private void handleDeductPoints() {
        Card selectedCard = cardsTable.getSelectionModel().getSelectedItem();
        if (selectedCard == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Выберите карту для списания баллов");
            return;
        }
        
        // Создаем диалоговое окно ввода количества баллов
        TextInputDialog dialog = new TextInputDialog("50");
        dialog.setTitle("Списание баллов");
        dialog.setHeaderText("Списание баллов с карты " + selectedCard.getNumber());
        dialog.setContentText("Введите количество баллов:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int points = Integer.parseInt(result.get());
                
                if (points <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Внимание", "Количество баллов должно быть положительным числом");
                    return;
                }
                
                if (points > selectedCard.getPoints()) {
                    showAlert(Alert.AlertType.WARNING, "Внимание", 
                            "Недостаточно баллов на карте. Доступно: " + selectedCard.getPoints());
                    return;
                }
                
                // Запрашиваем описание операции
                TextInputDialog descDialog = new TextInputDialog("Оплата услуг");
                descDialog.setTitle("Описание операции");
                descDialog.setHeaderText("Укажите причину списания баллов");
                descDialog.setContentText("Описание:");
                
                Optional<String> descResult = descDialog.showAndWait();
                String description = descResult.orElse("Оплата услуг");
                
                // Показываем индикатор загрузки
                Alert loadingAlert = showNonBlockingAlert(Alert.AlertType.INFORMATION, "Обработка", "Выполняется списание баллов...");
                
                cardService.deductPoints(selectedCard.getId(), points, description)
                        .thenAccept(updatedCard -> {
                            Platform.runLater(() -> {
                                // Закрываем индикатор загрузки
                                loadingAlert.close();
                                
                                if (updatedCard != null) {
                                    // Обновляем карту в списке
                                    updateCardInList(updatedCard);
                                    showAlert(Alert.AlertType.INFORMATION, "Списание баллов", 
                                            "С карты успешно списано " + points + " баллов");
                                } else {
                                    showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                            "Не удалось списать баллы с карты. Проверьте соединение с сервером или наличие достаточного количества баллов.");
                                }
                            });
                        })
                        .exceptionally(e -> {
                            Platform.runLater(() -> {
                                // Закрываем индикатор загрузки
                                loadingAlert.close();
                                
                                logger.error("Ошибка при списании баллов", e);
                                
                                // Показываем более информативное сообщение об ошибке
                                String errorMessage = e.getMessage();
                                if (e.getCause() != null) {
                                    errorMessage = e.getCause().getMessage();
                                }
                                
                                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                        "Не удалось списать баллы с карты: " + errorMessage);
                            });
                            return null;
                        });
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Введите корректное число баллов");
            }
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки удаления карты
     */
    @FXML
    private void handleDeleteCard() {
        Card selectedCard = cardsTable.getSelectionModel().getSelectedItem();
        if (selectedCard == null) {
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText("Удаление карты");
        alert.setContentText("Вы уверены, что хотите удалить карту " + selectedCard.getNumber() + "?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            cardService.deleteCard(selectedCard.getId())
                    .thenAccept(success -> {
                        Platform.runLater(() -> {
                            if (success) {
                                cardsList.remove(selectedCard);
                                showAlert(Alert.AlertType.INFORMATION, "Успех", "Карта успешно удалена");
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось удалить карту");
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось удалить карту");
                        });
                        return null;
                    });
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки обновления
     */
    @FXML
    private void handleRefresh() {
        loadCards();
    }
    
    /**
     * Обрабатывает нажатие кнопки выхода
     */
    @FXML
    private void handleLogout() {
        mainApp.logout();
    }
    
    // Методы для функций администратора
    
    /**
     * Обрабатывает нажатие кнопки управления пользователями
     */
    private void handleManageUsers() {
        logger.debug("Запуск управления пользователями");
        
        // Проверяем права доступа - только админ может управлять пользователями
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if (!"ADMIN".equals(roleName)) {
            logger.warn("Пользователь {} с ролью {} пытается получить доступ к управлению пользователями", 
                    currentUser.getUsername(), roleName);
            showAlert(Alert.AlertType.ERROR, "Ошибка доступа", 
                    "У вас нет прав на управление пользователями");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/fxml/admin/UsersView.fxml"));
            Parent page = loader.load();
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Управление пользователями");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);
            
            UsersViewController controller = loader.getController();
            controller.setAuthToken(authToken);
            
            dialogStage.showAndWait();
        } catch (Exception e) {
            logger.error("Ошибка при открытии окна управления пользователями", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось открыть окно управления пользователями");
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки управления акциями
     */
    private void handleManagePromotions() {
        logger.debug("Запуск управления акциями и промокодами");
        
        // Проверяем права доступа
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if (!"ADMIN".equals(roleName) && !"MANAGER".equals(roleName)) {
            logger.warn("Пользователь {} с ролью {} пытается получить доступ к управлению акциями", 
                    currentUser.getUsername(), roleName);
            showAlert(Alert.AlertType.ERROR, "Ошибка доступа", 
                    "У вас нет прав на управление акциями и промокодами");
            return;
        }
        
        try {
            // Пытаемся сначала создать FXMLLoader с абсолютным путем к файлу
            File fxmlFile = new File("client/src/main/resources/fxml/admin/PromotionsView.fxml");
            logger.debug("Пытаемся загрузить FXML из файловой системы: {}", fxmlFile.getAbsolutePath());
            
            FXMLLoader loader;
            Parent page;
            
            if (fxmlFile.exists()) {
                // Загружаем файл непосредственно из файловой системы
                loader = new FXMLLoader(fxmlFile.toURI().toURL());
                logger.debug("Загрузка FXML из файла: {}", fxmlFile.getAbsolutePath());
                
                try {
                    page = loader.load();
                    logger.debug("FXML успешно загружен из файла");
        } catch (IOException e) {
                    logger.error("Ошибка при загрузке FXML из файла", e);
                    throw e;
                }
            } else {
                // Если файл не найден, пробуем стандартный путь к ресурсу
                java.net.URL resource = getClass().getClassLoader().getResource("fxml/admin/PromotionsView.fxml");
                if (resource == null) {
                    resource = getClass().getResource("/fxml/admin/PromotionsView.fxml");
                }
                
                if (resource == null) {
                    resource = getClass().getResource("/client/fxml/admin/PromotionsView.fxml");
                }
                
                if (resource == null) {
                    throw new IOException("Не удалось найти FXML файл для окна акций");
                }
                
                loader = new FXMLLoader(resource);
                logger.debug("Загрузка FXML из ресурса: {}", resource);
                
                try {
                    page = loader.load();
                    logger.debug("FXML успешно загружен из ресурса");
        } catch (IOException e) {
                    logger.error("Ошибка при загрузке FXML из ресурса", e);
                    throw e;
                }
            }
            
            // Создаем новое диалоговое окно
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Управление акциями и промокодами");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            
            // Устанавливаем иконку окна
            try {
                // Пробуем загрузить иконку напрямую из файла
                File iconFile = new File("client/src/main/resources/images/icon_promo.png");
                if (iconFile.exists()) {
                    dialogStage.getIcons().add(new Image(iconFile.toURI().toString()));
                    logger.debug("Иконка для окна акций загружена из файла");
                } else {
                    // Если не нашли файл, пробуем через ресурсы
                    java.io.InputStream iconStream = getClass().getResourceAsStream("/images/icon_promo.png");
                    if (iconStream == null) {
                        iconStream = getClass().getResourceAsStream("images/icon_promo.png");
                    }
                    
                    if (iconStream != null) {
                        dialogStage.getIcons().add(new Image(iconStream));
                        logger.debug("Иконка для окна акций успешно загружена из ресурса");
                    } else {
                        logger.warn("Не удалось найти иконку для окна акций");
                    }
                }
            } catch (Exception e) {
                logger.warn("Ошибка при загрузке иконки для окна промоакций", e);
                // Продолжаем выполнение даже если нет иконки
            }
            
            // Настраиваем владельца окна
            if (mainApp != null && mainApp.getPrimaryStage() != null) {
                dialogStage.initOwner(mainApp.getPrimaryStage());
                logger.debug("Установлен родительский Stage для окна акций");
            } else {
                logger.warn("Не удалось установить родительский Stage для окна акций");
            }
            
            // Создаем сцену и применяем стили
            Scene scene = new Scene(page);
            
            // Применяем CSS стили
            try {
                // Пробуем загрузить CSS напрямую из файла
                File cssFile = new File("client/src/main/resources/css/styles.css");
                if (cssFile.exists()) {
                    scene.getStylesheets().add(cssFile.toURI().toString());
                    logger.debug("CSS стили загружены из файла");
                } else {
                    java.net.URL cssUrl = getClass().getResource("/css/styles.css");
                    if (cssUrl == null) {
                        cssUrl = getClass().getClassLoader().getResource("css/styles.css");
                    }
                    
                    if (cssUrl != null) {
                        scene.getStylesheets().add(cssUrl.toExternalForm());
                        logger.debug("CSS стили для окна акций успешно загружены из ресурса");
                    } else {
                        logger.warn("Не удалось найти CSS стили для окна акций");
                    }
                }
            } catch (Exception e) {
                logger.warn("Ошибка при загрузке CSS стилей", e);
                // Продолжаем выполнение даже без стилей
            }
            
            // Применяем глобальную тему, если она установлена
            applyGlobalThemeToWindow(scene);
            
            dialogStage.setScene(scene);
            
            // Получаем контроллер
            PromotionsViewController controller = loader.getController();
            if (controller == null) {
                throw new RuntimeException("Не удалось получить контроллер для окна управления акциями");
            }
            
            // Настраиваем контроллер
            controller.setAuthToken(authToken);
            logger.debug("Контроллер акций настроен, токен авторизации установлен");
            
            // Отображаем окно и ждем его закрытия
            dialogStage.showAndWait();
            logger.debug("Окно управления акциями закрыто");
            
        } catch (IOException e) {
            logger.error("Ошибка при загрузке FXML файла окна управления акциями", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                    "Не удалось загрузить окно управления акциями: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Непредвиденная ошибка при открытии окна управления акциями", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                    "Не удалось открыть окно управления акциями: " + e.getMessage());
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки настроек системы
     */
    private void handleSystemSettings() {
        logger.debug("Запуск настроек системы");
        
        // Создаем диалоговое окно с простым функционалом изменения цветовой темы
        Alert settingsDialog = new Alert(Alert.AlertType.NONE);
        settingsDialog.setTitle("Настройки системы");
        settingsDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Создаем интерфейс настроек
        VBox settingsContent = new VBox(10);
        settingsContent.setPadding(new Insets(20));
        settingsContent.setMinWidth(500);
        settingsContent.setMinHeight(300);
        
        Label titleLabel = new Label("Настройки интерфейса");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Секция выбора цветовой темы
        Label themeLabel = new Label("Выберите цветовую тему:");
        ComboBox<String> themeComboBox = new ComboBox<>();
        themeComboBox.getItems().addAll("Светлая тема", "Тёмная тема", "Неоновая тема", "Ретро тема");
        themeComboBox.setValue("Светлая тема"); // По умолчанию
        
        // Превью темы
        Pane themePreview = new Pane();
        themePreview.setMinHeight(150);
        themePreview.setPrefWidth(450);
        themePreview.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #cccccc; -fx-border-width: 1px;");
        
        // Создаем элементы для превью
        Button previewButton = new Button("Кнопка");
        TextField previewTextField = new TextField("Текстовое поле");
        Label previewLabel = new Label("Текстовая метка");
        CheckBox previewCheckBox = new CheckBox("Флажок");
        
        HBox previewControls = new HBox(10, previewButton, previewTextField, previewLabel, previewCheckBox);
        previewControls.setLayoutX(20);
        previewControls.setLayoutY(20);
        
        TableView<DummyData> previewTable = new TableView<>();
        TableColumn<DummyData, String> previewCol1 = new TableColumn<>("Колонка 1");
        TableColumn<DummyData, String> previewCol2 = new TableColumn<>("Колонка 2");
        previewTable.getColumns().addAll(previewCol1, previewCol2);
        previewTable.setPrefHeight(80);
        previewTable.setPrefWidth(410);
        previewTable.setLayoutX(20);
        previewTable.setLayoutY(50);
        
        // Заполняем таблицу тестовыми данными
        ObservableList<DummyData> dummyData = FXCollections.observableArrayList(
                new DummyData("Значение 1", "Значение A"),
                new DummyData("Значение 2", "Значение B")
        );
        previewCol1.setCellValueFactory(new PropertyValueFactory<>("value1"));
        previewCol2.setCellValueFactory(new PropertyValueFactory<>("value2"));
        previewTable.setItems(dummyData);
        
        themePreview.getChildren().addAll(previewControls, previewTable);
        
        // Секция дополнительных настроек
        Label fontSizeLabel = new Label("Размер шрифта:");
        Slider fontSizeSlider = new Slider(80, 120, 100);
        fontSizeSlider.setShowTickLabels(true);
        fontSizeSlider.setShowTickMarks(true);
        fontSizeSlider.setMajorTickUnit(10);
        fontSizeSlider.setBlockIncrement(5);
        
        // Обработчики событий
        
        // Обновление превью темы при выборе
        themeComboBox.setOnAction(e -> {
            String selectedTheme = themeComboBox.getValue();
            String previewStyle = "";
            
            // Устанавливаем стиль для превью в зависимости от выбранной темы
            switch (selectedTheme) {
                case "Тёмная тема":
                    previewStyle = "-fx-background-color: #2e2e2e;";
                    previewButton.setStyle("-fx-background-color: #505050; -fx-text-fill: white;");
                    previewTextField.setStyle("-fx-background-color: #3e3e3e; -fx-text-fill: white;");
                    previewLabel.setStyle("-fx-text-fill: white;");
                    previewCheckBox.setStyle("-fx-text-fill: white;");
                    previewTable.setStyle("-fx-background-color: #3e3e3e; -fx-text-fill: white;");
                    break;
                case "Неоновая тема":
                    previewStyle = "-fx-background-color: #0c1021;";
                    previewButton.setStyle("-fx-background-color: #3e0972; -fx-text-fill: #00ff99;");
                    previewTextField.setStyle("-fx-background-color: #1a0033; -fx-text-fill: #00ff99;");
                    previewLabel.setStyle("-fx-text-fill: #00ff99;");
                    previewCheckBox.setStyle("-fx-text-fill: #00ff99;");
                    previewTable.setStyle("-fx-background-color: #1a0033; -fx-text-fill: #00ff99;");
                    break;
                case "Ретро тема":
                    previewStyle = "-fx-background-color: #fdf6e3;";
                    previewButton.setStyle("-fx-background-color: #d33682; -fx-text-fill: #073642;");
                    previewTextField.setStyle("-fx-background-color: #eee8d5; -fx-text-fill: #073642;");
                    previewLabel.setStyle("-fx-text-fill: #073642;");
                    previewCheckBox.setStyle("-fx-text-fill: #073642;");
                    previewTable.setStyle("-fx-background-color: #eee8d5; -fx-text-fill: #073642;");
                    break;
                default: // Светлая тема
                    previewStyle = "-fx-background-color: #f5f5f5;";
                    previewButton.setStyle("-fx-background-color: #4285f4; -fx-text-fill: white;");
                    previewTextField.setStyle("-fx-background-color: white; -fx-text-fill: black;");
                    previewLabel.setStyle("-fx-text-fill: black;");
                    previewCheckBox.setStyle("-fx-text-fill: black;");
                    previewTable.setStyle("-fx-background-color: white; -fx-text-fill: black;");
                    break;
            }
            
            themePreview.setStyle(previewStyle + "-fx-border-color: #cccccc; -fx-border-width: 1px;");
        });
        
        // Секция кнопок действий
        Button applyButton = new Button("Применить настройки");
        applyButton.setDefaultButton(true);
        
        // Обработчик кнопки применения настроек
        applyButton.setOnAction(e -> {
            // Получаем выбранную тему
            String selectedTheme = themeComboBox.getValue();
            double fontScale = fontSizeSlider.getValue() / 100.0;
            
            // Генерируем CSS для выбранной темы
            String themeCSS = "";
            switch (selectedTheme) {
                case "Тёмная тема":
                    themeCSS = generateDarkThemeCSS(fontScale);
                    break;
                case "Неоновая тема":
                    themeCSS = generateNeonThemeCSS(fontScale);
                    break;
                case "Ретро тема":
                    themeCSS = generateRetroThemeCSS(fontScale);
                    break;
                default:
                    themeCSS = generateLightThemeCSS(fontScale);
                    break;
            }
            
            // Применяем тему ко всему приложению
            applyThemeToApplication(themeCSS);
            
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Настройки применены");
            successAlert.setHeaderText(null);
            successAlert.setContentText("Настройки интерфейса успешно применены!");
            successAlert.showAndWait();
        });
        
        // Формируем интерфейс настроек
        settingsContent.getChildren().addAll(
                titleLabel, 
                new Separator(),
                themeLabel, 
                themeComboBox,
                themePreview,
                new Separator(),
                fontSizeLabel,
                fontSizeSlider,
                new Separator(),
                applyButton
        );
        
        // Инициализируем превью темы
        themeComboBox.fireEvent(new ActionEvent());
        
        // Устанавливаем содержимое диалога
        settingsDialog.getDialogPane().setContent(settingsContent);
        
        // Отображаем диалог
        settingsDialog.showAndWait();
    }
    
    /**
     * Вспомогательный класс для тестовых данных
     */
    public static class DummyData {
        private final String value1;
        private final String value2;
        
        public DummyData(String value1, String value2) {
            this.value1 = value1;
            this.value2 = value2;
        }
        
        public String getValue1() {
            return value1;
        }
        
        public String getValue2() {
            return value2;
        }
    }
    
    /**
     * Применяет тему ко всему приложению
     */
    private void applyThemeToApplication(String css) {
        try {
            // Сохраняем тему глобально сразу
            setGlobalTheme(css);
            
            // Применяем к главному окну
            Scene mainScene = mainApp.getPrimaryStage().getScene();
            applyThemeToScene(mainScene, css);
            
            // Применяем ко всем уже открытым окнам
            for (Window window : Window.getWindows()) {
                if (window instanceof Stage) {
                    Stage stage = (Stage) window;
                    Scene windowScene = stage.getScene();
                    if (windowScene != null && windowScene != mainScene) {
                        applyThemeToScene(windowScene, css);
                    }
                }
            }
            
            logger.info("Тема успешно применена ко всем окнам приложения");
            
        } catch (Exception e) {
            logger.error("Ошибка при применении темы", e);
        }
    }
    
    /**
     * Применяет тему к конкретной сцене
     */
    private void applyThemeToScene(Scene scene, String css) {
        if (scene == null) return;
        
        try {
        // Удаляем все пользовательские стили
            scene.getStylesheets().removeIf(stylesheet -> 
                stylesheet.startsWith("data:text/css,"));
        
            // Пытаемся добавить стандартные стили, если их нет
            String appStylesheet = null;
            try {
                appStylesheet = getClass().getResource("/css/styles.css").toExternalForm();
            } catch (Exception e) {
                logger.debug("Стандартные стили не найдены: {}", e.getMessage());
            }
            
            if (appStylesheet != null && !scene.getStylesheets().contains(appStylesheet)) {
        scene.getStylesheets().add(appStylesheet);
            }
        
        // Добавляем новую тему
            if (css != null && !css.isEmpty()) {
                String encodedCss = "data:text/css," + URLEncoder.encode(css, StandardCharsets.UTF_8);
                scene.getStylesheets().add(encodedCss);
            }
            
        } catch (Exception e) {
            logger.debug("Не удалось применить тему к сцене: {}", e.getMessage());
        }
    }
    
    // Статическая переменная для хранения текущей темы
    private static String currentGlobalTheme = null;
    
    /**
     * Устанавливает глобальную тему приложения
     */
    private static void setGlobalTheme(String css) {
        currentGlobalTheme = css;
    }
    
    /**
     * Применяет сохраненную тему к новому окну
     */
    public static void applyGlobalThemeToWindow(Scene scene) {
        if (currentGlobalTheme != null && scene != null) {
            try {
                // Удаляем старые темы
                scene.getStylesheets().removeIf(stylesheet -> 
                    stylesheet.startsWith("data:text/css,"));
                
                // Добавляем стандартные стили, если их нет
                String appStylesheet = null;
                try {
                    appStylesheet = MainViewController.class.getResource("/css/styles.css").toExternalForm();
                } catch (Exception e) {
                    // Игнорируем, если стандартные стили не найдены
                }
                
                if (appStylesheet != null && !scene.getStylesheets().contains(appStylesheet)) {
                    scene.getStylesheets().add(appStylesheet);
                }
                
                // Применяем глобальную тему
                String encodedCss = "data:text/css," + URLEncoder.encode(currentGlobalTheme, StandardCharsets.UTF_8);
                scene.getStylesheets().add(encodedCss);
                
                Logger logger = LoggerFactory.getLogger(MainViewController.class);
                logger.debug("Глобальная тема применена к новому окну");
                
            } catch (Exception e) {
                Logger logger = LoggerFactory.getLogger(MainViewController.class);
                logger.debug("Не удалось применить глобальную тему к окну: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Генерирует CSS для темной темы
     */
    private String generateDarkThemeCSS(double fontScale) {
        return String.format(
                ".root { -fx-base: #3c3f41; -fx-background: #2b2b2b; -fx-control-inner-background: #2b2b2b; -fx-font-size: %.0f%%; }\n" +
                ".label { -fx-text-fill: #afb1b3; }\n" +
                ".text-field { -fx-background-color: #45494a; -fx-text-fill: #afb1b3; }\n" +
                ".button { -fx-background-color: #4c5052; -fx-text-fill: #afb1b3; }\n" +
                ".button:hover { -fx-background-color: #5c6164; }\n" +
                ".table-view { -fx-background-color: #2b2b2b; }\n" +
                ".table-view .column-header { -fx-background-color: #3c3f41; }\n" +
                ".table-row-cell { -fx-background-color: #2b2b2b; -fx-text-fill: #afb1b3; }\n" +
                ".table-row-cell:odd { -fx-background-color: #323232; }\n" +
                ".table-row-cell:selected { -fx-background-color: #4b6eaf; }\n" +
                ".combo-box { -fx-background-color: #4c5052; -fx-text-fill: #afb1b3; }\n" +
                ".combo-box .list-cell { -fx-text-fill: #afb1b3; }\n" +
                ".tab-pane .tab-header-area .tab-header-background { -fx-background-color: #3c3f41; }\n" +
                ".tab-pane .tab { -fx-background-color: #2b2b2b; }\n" +
                ".tab-pane .tab:selected { -fx-background-color: #4b6eaf; }\n",
                fontScale * 100
        );
    }
    
    /**
     * Генерирует CSS для светлой темы
     */
    private String generateLightThemeCSS(double fontScale) {
        return String.format(
                ".root { -fx-base: #ececec; -fx-background: #ffffff; -fx-control-inner-background: #ffffff; -fx-font-size: %.0f%%; }\n" +
                ".label { -fx-text-fill: #000000; }\n" +
                ".text-field { -fx-background-color: #ffffff; -fx-text-fill: #000000; }\n" +
                ".button { -fx-background-color: #4285f4; -fx-text-fill: white; }\n" +
                ".button:hover { -fx-background-color: #2a75f3; }\n" +
                ".table-view { -fx-background-color: #ffffff; }\n" +
                ".table-view .column-header { -fx-background-color: #f3f3f3; }\n" +
                ".table-row-cell { -fx-background-color: #ffffff; -fx-text-fill: #000000; }\n" +
                ".table-row-cell:odd { -fx-background-color: #f5f5f5; }\n" +
                ".table-row-cell:selected { -fx-background-color: #cfe2ff; -fx-text-fill: #000000; }\n" +
                ".combo-box { -fx-background-color: #ffffff; -fx-text-fill: #000000; }\n" +
                ".combo-box .list-cell { -fx-text-fill: #000000; }\n" +
                ".tab-pane .tab-header-area .tab-header-background { -fx-background-color: #f3f3f3; }\n" +
                ".tab-pane .tab { -fx-background-color: #e7e7e7; }\n" +
                ".tab-pane .tab:selected { -fx-background-color: #4285f4; -fx-text-fill: white; }\n",
                fontScale * 100
        );
    }
    
    /**
     * Генерирует CSS для неоновой темы
     */
    private String generateNeonThemeCSS(double fontScale) {
        return String.format(
                ".root { -fx-base: #0c1021; -fx-background: #0c1021; -fx-control-inner-background: #1a0033; -fx-font-size: %.0f%%; }\n" +
                ".label { -fx-text-fill: #00ff99; }\n" +
                ".text-field { -fx-background-color: #1a0033; -fx-text-fill: #00ff99; -fx-border-color: #6600cc; }\n" +
                ".button { -fx-background-color: #3e0972; -fx-text-fill: #00ff99; -fx-border-color: #6600cc; }\n" +
                ".button:hover { -fx-background-color: #6600cc; }\n" +
                ".table-view { -fx-background-color: #0c1021; -fx-border-color: #6600cc; }\n" +
                ".table-view .column-header { -fx-background-color: #1a0033; -fx-text-fill: #00ff99; }\n" +
                ".table-row-cell { -fx-background-color: #0c1021; -fx-text-fill: #00ff99; }\n" +
                ".table-row-cell:odd { -fx-background-color: #0f1629; }\n" +
                ".table-row-cell:selected { -fx-background-color: #6600cc; }\n" +
                ".combo-box { -fx-background-color: #1a0033; -fx-text-fill: #00ff99; -fx-border-color: #6600cc; }\n" +
                ".combo-box .list-cell { -fx-background-color: #1a0033; -fx-text-fill: #00ff99; }\n" +
                ".tab-pane .tab-header-area .tab-header-background { -fx-background-color: #0c1021; }\n" +
                ".tab-pane .tab { -fx-background-color: #1a0033; -fx-text-fill: #00ff99; }\n" +
                ".tab-pane .tab:selected { -fx-background-color: #6600cc; -fx-text-fill: #00ff99; }\n" +
                ".menu-bar { -fx-background-color: #1a0033; }\n" +
                ".menu { -fx-background-color: transparent; }\n" +
                ".menu .label { -fx-text-fill: #00ff99; }\n" +
                ".menu-item .label { -fx-text-fill: #00ff99; }\n" +
                ".menu-item { -fx-background-color: #1a0033; }\n",
                fontScale * 100
        );
    }
    
    /**
     * Генерирует CSS для ретро темы
     */
    private String generateRetroThemeCSS(double fontScale) {
        return String.format(
                ".root { -fx-base: #fdf6e3; -fx-background: #fdf6e3; -fx-control-inner-background: #eee8d5; -fx-font-size: %.0f%%; -fx-font-family: 'Courier New'; }\n" +
                ".label { -fx-text-fill: #073642; }\n" +
                ".text-field { -fx-background-color: #eee8d5; -fx-text-fill: #073642; -fx-border-color: #93a1a1; }\n" +
                ".button { -fx-background-color: #d33682; -fx-text-fill: #073642; -fx-border-color: #93a1a1; }\n" +
                ".button:hover { -fx-background-color: #cb4b16; }\n" +
                ".table-view { -fx-background-color: #eee8d5; -fx-border-color: #93a1a1; }\n" +
                ".table-view .column-header { -fx-background-color: #d33682; -fx-text-fill: #073642; }\n" +
                ".table-row-cell { -fx-background-color: #eee8d5; -fx-text-fill: #073642; }\n" +
                ".table-row-cell:odd { -fx-background-color: #fdf6e3; }\n" +
                ".table-row-cell:selected { -fx-background-color: #268bd2; -fx-text-fill: #073642; }\n" +
                ".combo-box { -fx-background-color: #eee8d5; -fx-text-fill: #073642; -fx-border-color: #93a1a1; }\n" +
                ".combo-box .list-cell { -fx-background-color: #eee8d5; -fx-text-fill: #073642; }\n" +
                ".tab-pane .tab-header-area .tab-header-background { -fx-background-color: #fdf6e3; }\n" +
                ".tab-pane .tab { -fx-background-color: #eee8d5; -fx-text-fill: #073642; }\n" +
                ".tab-pane .tab:selected { -fx-background-color: #d33682; -fx-text-fill: #073642; }\n",
                fontScale * 100
        );
    }
    
    /**
     * Обрабатывает нажатие кнопки просмотра отчетов
     */
    private void handleViewReports() {
        logger.debug("Запуск просмотра отчетов");
        try {
            // Создаем диалог выбора типа отчета
            Dialog<String> reportTypeDialog = new Dialog<>();
            reportTypeDialog.setTitle("Выбор отчета");
            reportTypeDialog.setHeaderText("Выберите тип отчета для просмотра");
        
        // Кнопка закрытия
            ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType okButtonType = new ButtonType("Показать", ButtonBar.ButtonData.OK_DONE);
            reportTypeDialog.getDialogPane().getButtonTypes().addAll(cancelButtonType, okButtonType);
            
            // Создание списка типов отчетов
            ComboBox<String> reportTypeCombo = new ComboBox<>();
            reportTypeCombo.getItems().addAll(
                "Отчет о начисленных баллах",
                "Отчет по активности пользователей",
                "Отчет по использованию акций",
                "Отчет по использованию промокодов",
                "Финансовый отчет"
            );
            reportTypeCombo.setValue(reportTypeCombo.getItems().get(0));
            
            // Создание панели с датами
            DatePicker fromDatePicker = new DatePicker(LocalDate.now().minusMonths(1));
            DatePicker toDatePicker = new DatePicker(LocalDate.now());
            
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
            grid.add(new Label("Тип отчета:"), 0, 0);
            grid.add(reportTypeCombo, 1, 0);
            grid.add(new Label("Период с:"), 0, 1);
            grid.add(fromDatePicker, 1, 1);
            grid.add(new Label("По:"), 0, 2);
            grid.add(toDatePicker, 1, 2);
            
            reportTypeDialog.getDialogPane().setContent(grid);
            
            // Конвертация результата
            reportTypeDialog.setResultConverter(dialogButton -> {
                if (dialogButton == okButtonType) {
                    return reportTypeCombo.getValue();
                }
                return null;
            });
            
            // Показываем диалог и получаем выбранный тип отчета
            Optional<String> reportType = reportTypeDialog.showAndWait();
            
            if (reportType.isPresent()) {
                // Генерируем отчет в зависимости от выбранного типа
                String selectedReportType = reportType.get();
                LocalDate fromDate = fromDatePicker.getValue();
                LocalDate toDate = toDatePicker.getValue();
                
                // Вызываем соответствующий метод в зависимости от типа отчета
                if (selectedReportType.contains("начисленных баллах")) {
                    showPointsReport(fromDate, toDate);
                } else if (selectedReportType.contains("активности пользователей")) {
                    showUserActivityReport(fromDate, toDate);
                } else if (selectedReportType.contains("использованию акций")) {
                    showPromotionsReport(fromDate, toDate);
                } else if (selectedReportType.contains("использованию промокодов")) {
                    showPromoCodesReport(fromDate, toDate);
                } else if (selectedReportType.contains("Финансовый")) {
                    showFinancialReport(fromDate, toDate);
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка при открытии отчетов", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось открыть отчеты");
        }
    }
    
    private void showPointsReport(LocalDate fromDate, LocalDate toDate) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/fxml/admin/reports/PointsReportView.fxml"));
            Parent page = loader.load();
            
            Stage reportStage = new Stage();
            reportStage.setTitle("Отчет о начисленных баллах");
            reportStage.initModality(Modality.WINDOW_MODAL);
            Scene scene = new Scene(page);
            reportStage.setScene(scene);
            
            PointsReportController controller = loader.getController();
            controller.setAuthToken(authToken);
            controller.setReportPeriod(fromDate, toDate);
            controller.loadReportData();
            
            reportStage.showAndWait();
        } catch (Exception e) {
            logger.error("Ошибка при открытии отчета о начисленных баллах", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                    "Не удалось открыть отчет о начисленных баллах: " + e.getMessage());
        }
    }
    
    private void showUserActivityReport(LocalDate fromDate, LocalDate toDate) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/fxml/admin/reports/UserActivityReportView.fxml"));
            Parent page = loader.load();
            
            Stage reportStage = new Stage();
            reportStage.setTitle("Отчет по активности пользователей");
            reportStage.initModality(Modality.WINDOW_MODAL);
            Scene scene = new Scene(page);
            reportStage.setScene(scene);
            
            UserActivityReportController controller = loader.getController();
            controller.setAuthToken(authToken);
            controller.setReportPeriod(fromDate, toDate);
            controller.loadReportData();
            
            reportStage.showAndWait();
        } catch (Exception e) {
            logger.error("Ошибка при открытии отчета по активности пользователей", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                    "Не удалось открыть отчет по активности пользователей: " + e.getMessage());
        }
    }
    
    private void showPromotionsReport(LocalDate fromDate, LocalDate toDate) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/fxml/admin/reports/PromotionsReportView.fxml"));
            Parent page = loader.load();
            
            Stage reportStage = new Stage();
            reportStage.setTitle("Отчет по использованию акций");
            reportStage.initModality(Modality.WINDOW_MODAL);
            Scene scene = new Scene(page);
            reportStage.setScene(scene);
            
            PromotionsReportController controller = loader.getController();
            controller.setAuthToken(authToken);
            controller.setReportPeriod(fromDate, toDate);
            controller.loadReportData();
            
            reportStage.showAndWait();
        } catch (Exception e) {
            logger.error("Ошибка при открытии отчета по использованию акций", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                    "Не удалось открыть отчет по использованию акций: " + e.getMessage());
        }
    }
    
    private void showPromoCodesReport(LocalDate fromDate, LocalDate toDate) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/fxml/admin/reports/PromoCodesReportView.fxml"));
            Parent page = loader.load();
            
            Stage reportStage = new Stage();
            reportStage.setTitle("Отчет по использованию промокодов");
            reportStage.initModality(Modality.WINDOW_MODAL);
            Scene scene = new Scene(page);
            reportStage.setScene(scene);
            
            PromoCodesReportController controller = loader.getController();
            controller.setAuthToken(authToken);
            controller.setReportPeriod(fromDate, toDate);
            controller.loadReportData();
            
            reportStage.showAndWait();
        } catch (Exception e) {
            logger.error("Ошибка при открытии отчета по использованию промокодов", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                    "Не удалось открыть отчет по использованию промокодов: " + e.getMessage());
        }
    }
    
    /**
     * Показать финансовый отчет
     */
    private void showFinancialReport(LocalDate fromDate, LocalDate toDate) {
        try {
            // Загружаем FXML
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/admin/reports/FinancialReportView.fxml"));
            Parent reportView = loader.load();
            
            // Получаем контроллер
            FinancialReportController controller = loader.getController();
            controller.setAuthToken(authToken);
            controller.setReportPeriod(fromDate, toDate);
            controller.loadReportData();
            
            // Создаем новое окно
            Stage reportStage = new Stage();
            reportStage.setTitle("Финансовый отчет");
            reportStage.initModality(Modality.WINDOW_MODAL);
            reportStage.initOwner(mainApp.getPrimaryStage());
            
            Scene scene = new Scene(reportView);
            reportStage.setScene(scene);
            
            // Устанавливаем иконку
            reportStage.getIcons().add(new Image("/images/icon_analytics.png"));
            
            // Показываем окно
            reportStage.show();
            
        } catch (IOException e) {
            logger.error("Ошибка при открытии финансового отчета", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                    "Не удалось открыть финансовый отчет: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Непредвиденная ошибка при открытии финансового отчета", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                    "Непредвиденная ошибка при открытии финансового отчета: " + e.getMessage());
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки резервного копирования
     */
    private void handleBackup() {
        logger.debug("Запуск управления резервными копиями");
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/fxml/admin/BackupView.fxml"));
            Parent page = loader.load();
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Управление резервными копиями");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);
            
            BackupViewController controller = loader.getController();
            controller.setAuthToken(authToken);
            
            dialogStage.showAndWait();
        } catch (Exception e) {
            logger.error("Ошибка при открытии окна резервных копий", e);
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось открыть окно управления резервными копиями");
        }
    }
    
    /**
     * Обновляет карту в списке
     * @param updatedCard обновленная карта
     */
    private void updateCardInList(Card updatedCard) {
        for (int i = 0; i < cardsList.size(); i++) {
            if (cardsList.get(i).getId().equals(updatedCard.getId())) {
                cardsList.set(i, updatedCard);
                cardsTable.getSelectionModel().select(i);
                break;
            }
        }
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
        
        // Применяем глобальную тему к диалогу
        alert.getDialogPane().getScene().getStylesheets().clear();
        applyGlobalThemeToWindow(alert.getDialogPane().getScene());
        
        alert.showAndWait();
    }
    
    /**
     * Показывает диалоговое окно без блокировки (для информационных сообщений)
     * @param type тип окна
     * @param title заголовок
     * @param message сообщение
     * @return созданный Alert объект для последующего закрытия
     */
    private Alert showNonBlockingAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Применяем глобальную тему к диалогу
        alert.getDialogPane().getScene().getStylesheets().clear();
        applyGlobalThemeToWindow(alert.getDialogPane().getScene());
        
        // Показываем диалог без блокировки
        alert.show();
        return alert;
    }
    
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }
    
    public void setAuthResponse(AuthResponse authResponse) {
        this.authResponse = authResponse;
    }
    
    /**
     * Обрабатывает нажатие кнопки начала игровой сессии - МАКСИМАЛЬНО УПРОЩЕННАЯ ВЕРСИЯ
     */
    private void handleStartSession() {
        // Run this method on JavaFX Application Thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::handleStartSession);
            return;
        }

        // Создание диалога
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Начать игровую сессию");
        dialog.setHeaderText("Введите информацию о новой сессии");
        
        // Кнопки
        ButtonType startButtonType = new ButtonType("Начать", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(startButtonType, ButtonType.CANCEL);
        
        // Содержимое диалога
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField cardNumberField = new TextField();
        cardNumberField.setPromptText("Номер карты");
        
        ComboBox<String> computerComboBox = new ComboBox<>();
        computerComboBox.getItems().addAll("ПК #1", "ПК #2", "ПК #3", "ПК #4", "ПК #5");
        computerComboBox.setValue("ПК #1");
        
        TextField hoursField = new TextField();
        hoursField.setPromptText("Количество часов");
        hoursField.setText("1");
        
        grid.add(new Label("Номер карты:"), 0, 0);
        grid.add(cardNumberField, 1, 0);
        grid.add(new Label("Компьютер:"), 0, 1);
        grid.add(computerComboBox, 1, 1);
        grid.add(new Label("Количество часов:"), 0, 2);
        grid.add(hoursField, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        // Фокус на поле номера карты
        Platform.runLater(cardNumberField::requestFocus);
        
        // Показ диалога и обработка результата
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == startButtonType) {
            String cardNumber = cardNumberField.getText();
            String computer = computerComboBox.getValue();
            String hoursText = hoursField.getText();
            
            if (cardNumber.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Предупреждение", "Введите номер карты");
                return;
            }
            
            try {
                int hours = Integer.parseInt(hoursText);
                if (hours <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Предупреждение", 
                            "Количество часов должно быть положительным числом");
                    return;
                }
                
                int minutes = hours * 60;
                
                try (Connection conn = DriverManager.getConnection(
                        SessionService.DB_URL, SessionService.DB_USER, SessionService.DB_PASSWORD)) {
                    
                    // 1. Получаем ID карты
                    Long cardId = null;
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "SELECT id FROM cards WHERE number = ?")) {
                        stmt.setString(1, cardNumber);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                cardId = rs.getLong("id");
                            } else {
                                showAlert(Alert.AlertType.WARNING, "Предупреждение", 
                                        "Карта с номером " + cardNumber + " не найдена");
                                return;
                            }
                        }
                    }
                    
                    // 2. Напрямую вставляем запись в таблицу sessions
                    // Получаем user_id по card_id сначала
                    Long userId = null;
                    try (PreparedStatement userStmt = conn.prepareStatement("SELECT user_id FROM cards WHERE id = ?")) {
                        userStmt.setLong(1, cardId);
                        try (ResultSet userRs = userStmt.executeQuery()) {
                            if (userRs.next()) {
                                userId = userRs.getLong("user_id");
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось найти пользователя для карты");
                                return;
                            }
                        }
                    }
                    
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO sessions (card_id, user_id, computer_number, start_time, minutes, status) " +
                            "VALUES (?, ?, ?, ?, ?, 'ACTIVE') RETURNING id", Statement.RETURN_GENERATED_KEYS)) {
                        
                        stmt.setLong(1, cardId);
                        stmt.setLong(2, userId);
                        stmt.setInt(3, Integer.parseInt(computer.replaceAll("\\D", ""))); // Извлекаем номер компьютера
                        stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                        stmt.setInt(5, minutes);
                        
                        int affectedRows = stmt.executeUpdate();
                        
                        if (affectedRows > 0) {
                            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                                if (generatedKeys.next()) {
                    showAlert(Alert.AlertType.INFORMATION, "Сессия начата", 
                            "Игровая сессия успешно начата!\n" +
                                            "Карта: " + cardNumber + "\n" +
                            "Компьютер: " + computer + "\n" +
                            "Продолжительность: " + hours + " ч.");
                                    return;
                                }
                            }
                        }
                        
                                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                "Не удалось начать игровую сессию - ошибка при добавлении записи");
                    }
                    
                } catch (SQLException e) {
                    logger.error("Ошибка при работе с БД: {}", e.getMessage(), e);
                    showAlert(Alert.AlertType.ERROR, "Ошибка БД", 
                            "Ошибка при работе с базой данных: " + e.getMessage());
                }
                
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Предупреждение", "Введите корректное количество часов");
            }
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки завершения игровой сессии
     */
    private void handleEndSession() {
        // Run this method on JavaFX Application Thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::handleEndSession);
            return;
        }
        
        // Создание диалога
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Завершить игровую сессию");
        dialog.setHeaderText("Выберите активную сессию для завершения");
        
        // Кнопки
        ButtonType endButtonType = new ButtonType("Завершить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(endButtonType, ButtonType.CANCEL);
        
        // Содержимое диалога
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Таблица активных сессий
        TableView<Session> sessionsTable = new TableView<>();
        
        TableColumn<Session, String> computerCol = new TableColumn<>("Компьютер");
        computerCol.setCellValueFactory(param -> {
            Session session = param.getValue();
            String computerName = session.getComputerName();
            return new SimpleStringProperty(computerName);
        });
        
        TableColumn<Session, String> cardCol = new TableColumn<>("Карта");
        cardCol.setCellValueFactory(param -> 
            new SimpleStringProperty(param.getValue().getCardNumber() != null ? 
                    param.getValue().getCardNumber() : ""));
        
        TableColumn<Session, String> startTimeCol = new TableColumn<>("Начало");
        startTimeCol.setCellValueFactory(param -> 
            new SimpleStringProperty(param.getValue().getStartTime() != null ? 
                    param.getValue().getStartTime().toString() : ""));
        
        TableColumn<Session, String> remainingTimeCol = new TableColumn<>("Осталось");
        remainingTimeCol.setCellValueFactory(param -> {
            Session session = param.getValue();
            int remainingMinutes = session.getRemainingMinutes(LocalDateTime.now());
            int hours = remainingMinutes / 60;
            int minutes = remainingMinutes % 60;
            return new SimpleStringProperty(String.format("%d:%02d", hours, minutes));
        });
        
        sessionsTable.getColumns().addAll(computerCol, cardCol, startTimeCol, remainingTimeCol);
        
        // Показываем индикатор загрузки
        final Alert loadingAlert = showNonBlockingAlert(Alert.AlertType.INFORMATION, "Загрузка данных", 
                "Выполняется загрузка активных сессий...");
        
        // Загрузка всех активных сессий напрямую из базы
        List<Session> activeSessions = sessionService.getActiveSessions();
        
        // Закрываем индикатор загрузки
        if (loadingAlert != null && loadingAlert.isShowing()) {
            loadingAlert.close();
        }
        
        sessionsTable.setItems(FXCollections.observableArrayList(activeSessions));
        if (activeSessions.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Информация", 
                    "Активные сессии отсутствуют");
        }
        
        content.getChildren().add(sessionsTable);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(500, 300);
        
        // Блокировка кнопки завершения, если не выбрана сессия
        Node endButton = dialog.getDialogPane().lookupButton(endButtonType);
        endButton.setDisable(true);
        
        // Активация кнопки при выборе сессии
        sessionsTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> endButton.setDisable(newValue == null));
        
        // Показ диалога и обработка результата
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == endButtonType) {
            Session selectedSession = sessionsTable.getSelectionModel().getSelectedItem();
            if (selectedSession != null) {
                // Показываем индикатор загрузки
                final Alert finishAlert = showNonBlockingAlert(Alert.AlertType.INFORMATION, "Завершение сессии", 
                        "Выполняется завершение игровой сессии...");
                
                // Завершение сессии через сервис
                Optional<Session> finishedSessionOpt = sessionService.finishSession(selectedSession.getId());
                
                // Закрываем индикатор загрузки
                if (finishAlert != null && finishAlert.isShowing()) {
                    finishAlert.close();
                }
                
                if (finishedSessionOpt.isPresent()) {
                    Session finishedSession = finishedSessionOpt.get();
                    String computer = finishedSession.getComputerName();
                    String card = finishedSession.getCardNumber() != null ? finishedSession.getCardNumber() : "";
                    String points = finishedSession.getPoints() != null ? finishedSession.getPoints().toString() : "0";
                    
                    showAlert(Alert.AlertType.INFORMATION, "Сессия завершена", 
                            "Игровая сессия успешно завершена!\n" +
                            "Компьютер: " + computer + "\n" +
                            "Карта: " + card + "\n" +
                            "Начислено баллов: " + points);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Ошибка", 
                            "Не удалось завершить игровую сессию. Повторите попытку.");
                }
            }
        }
    }
    
    /**
     * Обрабатывает нажатие кнопки просмотра истории сессий
     */
    private void handleViewSessionHistory() {
        // Run this method on JavaFX Application Thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::handleViewSessionHistory);
            return;
        }
        
        // Создание диалога
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("История игровых сессий");
        dialog.setHeaderText("Просмотр истории игровых сессий");
        
        // Кнопки
        ButtonType closeButtonType = new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);
        
        // Содержимое диалога
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Фильтры
        GridPane filterGrid = new GridPane();
        filterGrid.setHgap(10);
        filterGrid.setVgap(10);
        filterGrid.setPadding(new Insets(0, 0, 10, 0));
        
        TextField cardNumberField = new TextField();
        cardNumberField.setPromptText("Номер карты");
        
        DatePicker fromDatePicker = new DatePicker();
        fromDatePicker.setPromptText("С даты");
        
        DatePicker toDatePicker = new DatePicker();
        toDatePicker.setPromptText("По дату");
        
        Button searchButton = new Button("Поиск");
        
        filterGrid.add(new Label("Номер карты:"), 0, 0);
        filterGrid.add(cardNumberField, 1, 0);
        filterGrid.add(new Label("С даты:"), 2, 0);
        filterGrid.add(fromDatePicker, 3, 0);
        filterGrid.add(new Label("По дату:"), 4, 0);
        filterGrid.add(toDatePicker, 5, 0);
        filterGrid.add(searchButton, 6, 0);
        
        // Таблица сессий
        TableView<Session> sessionsTable = new TableView<>();
        
        TableColumn<Session, String> cardCol = new TableColumn<>("Карта");
        cardCol.setCellValueFactory(param -> 
            new SimpleStringProperty(param.getValue().getCardNumber() != null ? 
                    param.getValue().getCardNumber() : ""));
        
        TableColumn<Session, String> computerCol = new TableColumn<>("Компьютер");
        computerCol.setCellValueFactory(param -> {
            Session session = param.getValue();
            return new SimpleStringProperty(session.getComputerName());
        });
        
        TableColumn<Session, String> startTimeCol = new TableColumn<>("Начало");
        startTimeCol.setCellValueFactory(param -> 
            new SimpleStringProperty(param.getValue().getStartTime() != null ? 
                    param.getValue().getStartTime().toString() : ""));
        
        TableColumn<Session, String> endTimeCol = new TableColumn<>("Окончание");
        endTimeCol.setCellValueFactory(param -> 
            new SimpleStringProperty(param.getValue().getEndTime() != null ? 
                    param.getValue().getEndTime().toString() : "В процессе"));
        
        TableColumn<Session, String> durationCol = new TableColumn<>("Длительность");
        durationCol.setCellValueFactory(param -> {
            Session session = param.getValue();
            int minutes = session.getMinutes() != null ? session.getMinutes() : 0;
            int hours = minutes / 60;
            int mins = minutes % 60;
            return new SimpleStringProperty(String.format("%d:%02d", hours, mins));
        });
        
        TableColumn<Session, String> pointsCol = new TableColumn<>("Баллы");
        pointsCol.setCellValueFactory(param -> 
            new SimpleStringProperty(param.getValue().getPoints() != null ? 
                    param.getValue().getPoints().toString() : "0"));
        
        sessionsTable.getColumns().addAll(cardCol, computerCol, startTimeCol, endTimeCol, durationCol, pointsCol);
        
        // Установка начальных данных - все сессии
        ObservableList<Session> sessionData = FXCollections.observableArrayList();
        sessionsTable.setItems(sessionData);
        
        // Показываем индикатор загрузки
        final Alert loadingAlert = showNonBlockingAlert(Alert.AlertType.INFORMATION, "Загрузка данных", 
                "Загрузка истории сессий...");
        
        // Загружаем все сессии
        List<Session> sessions = sessionService.getAllSessions();
        
        // Закрываем индикатор загрузки
        if (loadingAlert != null && loadingAlert.isShowing()) {
            loadingAlert.close();
        }
        
                    sessionData.setAll(sessions);
        
        // Обработчик нажатия кнопки поиска
        searchButton.setOnAction(event -> {
            String cardNumber = cardNumberField.getText().trim();
            LocalDateTime fromDate = fromDatePicker.getValue() != null ? 
                    fromDatePicker.getValue().atStartOfDay() : null;
            LocalDateTime toDate = toDatePicker.getValue() != null ? 
                    toDatePicker.getValue().plusDays(1).atStartOfDay() : null;
            
            // Показываем индикатор загрузки
            final Alert searchAlert = showNonBlockingAlert(Alert.AlertType.INFORMATION, "Поиск", 
                    "Выполняется поиск сессий...");
            
            try {
                List<Session> filteredSessions;
            
            if (!cardNumber.isEmpty()) {
                // Поиск карты по номеру
                    try (Connection conn = DriverManager.getConnection(
                            SessionService.DB_URL, SessionService.DB_USER, SessionService.DB_PASSWORD);
                         PreparedStatement stmt = conn.prepareStatement(
                             "SELECT id FROM cards WHERE number = ?")) {
                        
                        stmt.setString(1, cardNumber);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                Long cardId = rs.getLong("id");
                                
                            // Получаем сессии по карте и периоду
                                filteredSessions = sessionService.getSessionsByCardIdAndPeriod(
                                        cardId, fromDate, toDate);
                } else {
                    showAlert(Alert.AlertType.WARNING, "Предупреждение", 
                            "Карта с номером " + cardNumber + " не найдена");
                                filteredSessions = new ArrayList<>();
                }
                        }
                    }
            } else {
                    // Если номер карты не указан, берем все сессии и фильтруем по датам
                    filteredSessions = sessions;
                    
                if (fromDate != null || toDate != null) {
                        filteredSessions = sessions.stream()
                        .filter(s -> {
                            boolean afterFrom = fromDate == null || 
                                            (s.getStartTime() != null && 
                                             (s.getStartTime().isEqual(fromDate) || s.getStartTime().isAfter(fromDate)));
                            boolean beforeTo = toDate == null || 
                                            (s.getStartTime() != null && s.getStartTime().isBefore(toDate));
                            return afterFrom && beforeTo;
                        })
                        .collect(java.util.stream.Collectors.toList());
                    }
                }
                
                // Закрываем индикатор загрузки
                if (searchAlert != null && searchAlert.isShowing()) {
                    searchAlert.close();
                }
                
                sessionData.setAll(filteredSessions);
                
            } catch (SQLException e) {
                // Закрываем индикатор загрузки
                if (searchAlert != null && searchAlert.isShowing()) {
                    searchAlert.close();
                }
                
                logger.error("Ошибка при поиске сессий", e);
                showAlert(Alert.AlertType.ERROR, "Ошибка", 
                        "Не удалось найти сессии: " + e.getMessage());
            }
        });
        
        content.getChildren().addAll(filterGrid, sessionsTable);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(800, 500);
        
        // Показ диалога
        dialog.showAndWait();
    }

    /**
     * Handles the click on the product sales button
     */
    private void handleProductSales() {
        // Create dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Продажа товаров");
        dialog.setHeaderText("Выберите товары для продажи");
        
        // Buttons
        ButtonType sellButtonType = new ButtonType("Оформить продажу", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sellButtonType, ButtonType.CANCEL);
        
        // Dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));
        
        // Customer card field
        TextField cardNumberField = new TextField();
        cardNumberField.setPromptText("Номер карты клиента (опционально)");
        
        // Available products
        ListView<ProductItem> productsListView = new ListView<>();
        ObservableList<ProductItem> productItems = FXCollections.observableArrayList(
            new ProductItem(1L, "Кола", "Напитки", 120.0, 48),
            new ProductItem(2L, "Чипсы", "Закуски", 150.0, 35),
            new ProductItem(3L, "Энергетик", "Напитки", 180.0, 24),
            new ProductItem(4L, "Шоколадный батончик", "Закуски", 90.0, 56),
            new ProductItem(5L, "Кофе", "Напитки", 100.0, 100)
        );
        productsListView.setItems(productItems);
        
        // Selected products
        ListView<ProductItem> cartListView = new ListView<>();
        ObservableList<ProductItem> cartItems = FXCollections.observableArrayList();
        cartListView.setItems(cartItems);
        
        // Labels for lists
        Label availableProductsLabel = new Label("Доступные товары:");
        Label cartLabel = new Label("Корзина покупок:");
        
        // Total price
        Label totalPriceLabel = new Label("Итого: 0.0 ₽");
        
        // Add/Remove buttons
        Button addToCartButton = new Button("Добавить в корзину >");
        Button removeFromCartButton = new Button("< Удалить из корзины");
        
        addToCartButton.setOnAction(e -> {
            ProductItem selectedItem = productsListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                cartItems.add(selectedItem);
                updateTotalPrice(totalPriceLabel, cartItems);
            }
        });
        
        removeFromCartButton.setOnAction(e -> {
            ProductItem selectedItem = cartListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                cartItems.remove(selectedItem);
                updateTotalPrice(totalPriceLabel, cartItems);
            }
        });
        
        // Layout
        grid.add(new Label("Номер карты клиента:"), 0, 0);
        grid.add(cardNumberField, 1, 0, 3, 1);
        
        grid.add(availableProductsLabel, 0, 1);
        grid.add(cartLabel, 2, 1);
        
        grid.add(productsListView, 0, 2);
        grid.add(cartListView, 2, 2);
        
        VBox buttonsBox = new VBox(10);
        buttonsBox.setPadding(new Insets(50, 10, 10, 10));
        buttonsBox.getChildren().addAll(addToCartButton, removeFromCartButton);
        grid.add(buttonsBox, 1, 2);
        
        grid.add(totalPriceLabel, 2, 3);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefSize(600, 400);
        
        // Show dialog and process result
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == sellButtonType) {
            if (cartItems.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Предупреждение", "Корзина пуста, добавьте товары для продажи");
                return;
            }
            
            String cardNumber = cardNumberField.getText().trim();
            double totalPrice = calculateTotalPrice(cartItems);
            
            if (!cardNumber.isEmpty()) {
                // Check if card exists and add bonus points
                cardService.getCardByNumber(cardNumber)
                    .thenAccept(cardOpt -> {
                        if (cardOpt.isPresent()) {
                            Card card = cardOpt.get();
                            // Calculate bonus points (10% of purchase)
                            int bonusPoints = (int)(totalPrice * 0.1);
                            
                            // Add points to card
                            cardService.addPoints(card.getId(), bonusPoints, "Бонус за покупку товаров")
                                .thenAccept(updatedCard -> {
                                    Platform.runLater(() -> {
                                        showAlert(Alert.AlertType.INFORMATION, "Продажа оформлена", 
                                            "Продажа успешно оформлена!\n" +
                                            "Сумма: " + totalPrice + " ₽\n" +
                                            "Начислено бонусных баллов: " + bonusPoints);
                                        
                                        // Update card in list if it exists
                                        updateCardInList(updatedCard);
                                    });
                                });
                        } else {
                            Platform.runLater(() -> {
                                showAlert(Alert.AlertType.WARNING, "Предупреждение", 
                                    "Карта с номером " + cardNumber + " не найдена.\n" +
                                    "Продажа оформлена без начисления баллов.\n" +
                                    "Сумма: " + totalPrice + " ₽");
                            });
                        }
                    });
            } else {
                // Complete sale without bonus points
                showAlert(Alert.AlertType.INFORMATION, "Продажа оформлена", 
                    "Продажа успешно оформлена!\n" +
                    "Сумма: " + totalPrice + " ₽");
            }
        }
    }
    
    /**
     * Helper method to update total price label
     */
    private void updateTotalPrice(Label totalPriceLabel, List<ProductItem> items) {
        double total = calculateTotalPrice(items);
        totalPriceLabel.setText(String.format("Итого: %.2f ₽", total));
    }
    
    /**
     * Helper method to calculate total price
     */
    private double calculateTotalPrice(List<ProductItem> items) {
        return items.stream()
            .mapToDouble(ProductItem::getPrice)
            .sum();
    }
    
    /**
     * Handles the click on the schedule button
     */
    private void handleSchedule() {
        // Run this method on JavaFX Application Thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::handleSchedule);
            return;
        }
        
        // Create dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Расписание компьютеров");
        dialog.setHeaderText("Управление расписанием компьютеров");
        
        // Buttons
        ButtonType refreshButtonType = new ButtonType("Обновить", ButtonBar.ButtonData.APPLY);
        ButtonType closeButtonType = new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(refreshButtonType, closeButtonType);
        
        // Dialog content
        BorderPane dialogContent = new BorderPane();
        
        // Date picker for selecting the day
        HBox datePickerContainer = new HBox(10);
        datePickerContainer.setPadding(new Insets(10));
        datePickerContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        DatePicker datePicker = new DatePicker(LocalDate.now());
        Label dateLabel = new Label("Выберите дату: ");
        
        datePickerContainer.getChildren().addAll(dateLabel, datePicker);
        dialogContent.setTop(datePickerContainer);
        
        // Computers tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        dialogContent.setCenter(tabPane);
        
        // Alert for loading
        final Alert[] loadingAlert = new Alert[1];
        
        // Создаем финальную ссылку для использования в лямбда-выражениях
        final Runnable[] reloadScheduleRef = new Runnable[1];
        
        // Простой Runnable метод для загрузки расписания
        Runnable reloadSchedule = new Runnable() {
            @Override
            public void run() {
                LocalDate selectedDate = datePicker.getValue();
                
                loadingAlert[0] = showNonBlockingAlert(Alert.AlertType.INFORMATION, "Загрузка данных", 
                        "Загрузка расписания на " + selectedDate + "...");
                
                // Очищаем существующие табы
                tabPane.getTabs().clear();
                
                // Получаем компьютеры и записи для выбранной даты
                Map<String, List<client.model.ScheduleEntry>> computerEntries = 
                        scheduleService.getComputerEntriesByDate(selectedDate);
                
                // Сортируем компьютеры по имени
                List<String> computerNames = new ArrayList<>(computerEntries.keySet());
                java.util.Collections.sort(computerNames);
                
                // Создаем вкладку для каждого компьютера
                for (String computerName : computerNames) {
                    Tab tab = new Tab(computerName);
                    
                    // Tab content
                    BorderPane tabContent = new BorderPane();
                    
                    // Schedule grid
                    GridPane scheduleGrid = new GridPane();
                    scheduleGrid.setHgap(5);
                    scheduleGrid.setVgap(5);
                    scheduleGrid.setPadding(new Insets(10));
                    
                    // Hours header
                    for (int hour = 9; hour <= 23; hour++) {
                        Label timeLabel = new Label(String.format("%02d:00", hour));
                        timeLabel.setPadding(new Insets(5));
                        scheduleGrid.add(timeLabel, 0, hour - 8);
                    }
                    
                    // Create time slots
                    for (int hour = 9; hour <= 23; hour++) {
                        final int bookingHour = hour;
                        final LocalDateTime slotTime = selectedDate.atTime(hour, 0);
                        
                        Button timeSlot = new Button();
                        timeSlot.setPrefWidth(150);
                        
                        // Check if this hour is busy
                        client.model.ScheduleEntry activeEntry = scheduleService.getActiveEntry(computerName, slotTime);
                        boolean isBooked = activeEntry != null;
                        
                        if (isBooked) {
                            // Slot is booked
                            String clientName = activeEntry.getClientName();
                            String cardNumber = activeEntry.getCardNumber() != null ? 
                                    activeEntry.getCardNumber() : "Нет карты";
                            
                            timeSlot.setText("Занято: " + clientName);
                            timeSlot.setStyle("-fx-background-color: #ff5252;");
                            
                            // Allow staff to cancel bookings
                            String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
                            boolean isStaff = "STAFF".equals(roleName) || "ADMIN".equals(roleName) || "MANAGER".equals(roleName);
                            
                            if (isStaff) {
                                timeSlot.setDisable(false);
                                final Long entryId = activeEntry.getId();
                                
                                timeSlot.setOnAction(e -> {
                                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                                    confirmAlert.setTitle("Отмена бронирования");
                                    confirmAlert.setHeaderText("Отменить бронирование?");
                                    confirmAlert.setContentText("Вы уверены, что хотите отменить бронирование?\n" +
                                            "Компьютер: " + computerName + "\n" +
                                            "Время: " + bookingHour + ":00\n" +
                                            "Клиент: " + clientName + "\n" +
                                            "Карта: " + cardNumber);
                                    
                                    Optional<ButtonType> result = confirmAlert.showAndWait();
                                    if (result.isPresent() && result.get() == ButtonType.OK) {
                                        boolean canceled = scheduleService.cancelEntry(entryId);
                                        if (canceled) {
                                            reloadScheduleRef[0].run(); // Используем ссылку из массива
                                            showAlert(Alert.AlertType.INFORMATION, "Бронирование отменено", 
                                                    "Бронирование компьютера успешно отменено.");
                                        } else {
                                            showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                                    "Не удалось отменить бронирование.");
                                        }
                                    }
                                });
                            } else {
                                timeSlot.setDisable(true);
                            }
                        } else {
                            // Slot is available
                            timeSlot.setText("Свободно");
                            timeSlot.setStyle("-fx-background-color: #4CAF50;");
                            
                            timeSlot.setOnAction(e -> {
                                Dialog<ButtonType> bookingDialog = new Dialog<>();
                                bookingDialog.setTitle("Бронирование компьютера");
                                bookingDialog.setHeaderText("Бронирование " + computerName + " на " + 
                                                String.format("%02d:00", bookingHour));
                                
                                // Booking form
                                GridPane bookingForm = new GridPane();
                                bookingForm.setHgap(10);
                                bookingForm.setVgap(10);
                                bookingForm.setPadding(new Insets(20, 150, 10, 10));
                                
                                TextField nameField = new TextField();
                                nameField.setPromptText("Имя клиента");
                                
                                TextField cardField = new TextField();
                                cardField.setPromptText("Номер карты (опционально)");
                                
                                ComboBox<Integer> durationBox = new ComboBox<>();
                                durationBox.getItems().addAll(1, 2, 3, 4);
                                durationBox.setValue(1);
                                
                                bookingForm.add(new Label("Имя клиента:"), 0, 0);
                                bookingForm.add(nameField, 1, 0);
                                bookingForm.add(new Label("Номер карты:"), 0, 1);
                                bookingForm.add(cardField, 1, 1);
                                bookingForm.add(new Label("Длительность (ч):"), 0, 2);
                                bookingForm.add(durationBox, 1, 2);
                                
                                ButtonType bookButtonType = new ButtonType("Забронировать", ButtonBar.ButtonData.OK_DONE);
                                bookingDialog.getDialogPane().getButtonTypes().addAll(bookButtonType, ButtonType.CANCEL);
                                bookingDialog.getDialogPane().setContent(bookingForm);
                                
                                Optional<ButtonType> bookingResult = bookingDialog.showAndWait();
                                if (bookingResult.isPresent() && bookingResult.get() == bookButtonType) {
                                    String clientName = nameField.getText().trim();
                                    if (clientName.isEmpty()) {
                                        showAlert(Alert.AlertType.WARNING, "Предупреждение", "Введите имя клиента");
                                        return;
                                    }
                                    
                                    int duration = durationBox.getValue();
                                    LocalDateTime startTime = slotTime;
                                    
                                    // Show loading alert
                                    final Alert bookingLoadingAlert = showNonBlockingAlert(Alert.AlertType.INFORMATION, 
                                            "Создание бронирования", "Создание бронирования...");
                                    
                                    // Check card and create booking
                                    String cardNumber = cardField.getText().trim();
                                    boolean success;
                                    
                                    if (!cardNumber.isEmpty()) {
                                        // Create booking with card
                                        success = scheduleService.createBookingWithCard(cardNumber, computerName, 
                                                clientName, startTime, duration);
                                    } else {
                                        // Create booking without card
                                        success = scheduleService.createBooking(computerName, clientName, 
                                                startTime, duration);
                                    }
                                    
                                    // Close loading alert
                                    if (bookingLoadingAlert != null && bookingLoadingAlert.isShowing()) {
                                        bookingLoadingAlert.close();
                                    }
                                    
                                    if (success) {
                                        showAlert(Alert.AlertType.INFORMATION, "Бронирование успешно", 
                                            "Компьютер успешно забронирован!\n" +
                                            "Клиент: " + clientName + "\n" +
                                            (cardNumber.isEmpty() ? "" : "Карта: " + cardNumber + "\n") +
                                            "Время: " + String.format("%02d:00", bookingHour) + "\n" +
                                            "Продолжительность: " + duration + " ч.");
                                        
                                        // Refresh schedule
                                        reloadScheduleRef[0].run(); // Используем ссылку из массива
                                    } else {
                                        showAlert(Alert.AlertType.ERROR, "Ошибка", 
                                            "Не удалось создать бронирование. Возможно, это время уже занято.");
                                    }
                                }
                            });
                        }
                        
                        scheduleGrid.add(timeSlot, 1, hour - 8);
                    }
                    
                    // Add schedule grid to tab
                    VBox tabLayout = new VBox(10);
                    tabLayout.setPadding(new Insets(10));
                    
                    Label scheduleDateLabel = new Label("Расписание на " + selectedDate.getDayOfMonth() + "." + 
                                                      selectedDate.getMonthValue() + "." + selectedDate.getYear());
                    scheduleDateLabel.setStyle("-fx-font-weight: bold;");
                    
                    tabLayout.getChildren().addAll(scheduleDateLabel, scheduleGrid);
                    tabContent.setCenter(tabLayout);
                    tab.setContent(tabContent);
                    
                    tabPane.getTabs().add(tab);
                }
                
                // Close loading alert if it's still showing
                if (loadingAlert[0] != null && loadingAlert[0].isShowing()) {
                    loadingAlert[0].close();
                }
            }
        };
        
        // Сохраняем ссылку в массиве
        reloadScheduleRef[0] = reloadSchedule;
        
        // Initial load
        reloadSchedule.run();
        
        // Add event handler for date change
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null && !newDate.equals(oldDate)) {
                reloadSchedule.run();
            }
        });
        
        // Обработчик для кнопки обновления
        Node refreshButton = dialog.getDialogPane().lookupButton(refreshButtonType);
        refreshButton.addEventFilter(ActionEvent.ACTION, event -> {
            reloadSchedule.run();
            event.consume(); // Важно - останавливаем распространение события, чтобы диалог не закрывался
        });
        
        // Set dialog content
        dialog.getDialogPane().setContent(dialogContent);
        dialog.getDialogPane().setPrefSize(600, 700);
        
        // Show dialog
        dialog.showAndWait();
    }
    
    // Method to generate a random card number
    private String generateCardNumber() {
        // Генерация случайного 16-значного номера карты
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 16; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
    
    /**
     * Обрабатывает применение промокода клиентом
     */
    @FXML
    private void handleApplyPromoCode() {
        Card selectedCard = cardsTable.getSelectionModel().getSelectedItem();
        if (selectedCard == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Выберите карту для применения промокода");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Применить промокод");
        dialog.setHeaderText("Введите промокод для применения к карте " + selectedCard.getNumber());
        dialog.setContentText("Промокод:");
        
        Optional<String> dialogResult = dialog.showAndWait();
        if (dialogResult.isPresent() && !dialogResult.get().trim().isEmpty()) {
            String promoCode = dialogResult.get().trim().toUpperCase();
            
            // Показать индикатор загрузки
            Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
            loadingAlert.setTitle("Применение промокода");
            loadingAlert.setHeaderText("Проверка промокода...");
            loadingAlert.getDialogPane().getButtonTypes().clear();
            loadingAlert.show();
            
            // Применить промокод через API сервера
            CompletableFuture.supplyAsync(() -> {
                try (Connection conn = getConnection()) {
                    // Проверяем существование и действительность промокода
                    String checkSql = "SELECT * FROM promo_codes WHERE code = ? AND is_active = true AND " +
                                      "(expiry_date IS NULL OR expiry_date >= CURRENT_DATE) AND " +
                                      "(uses_limit IS NULL OR uses_count < uses_limit)";
                    
                    try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                        stmt.setString(1, promoCode);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (!rs.next()) {
                                throw new RuntimeException("Промокод недействителен или истек срок действия");
                            }
                            
                            Long promoCodeId = rs.getLong("id");
                            Integer bonusPoints = rs.getObject("bonus_points") != null ? rs.getInt("bonus_points") : null;
                            Double discountPercent = rs.getObject("discount_percent") != null ? rs.getDouble("discount_percent") : null;
                            
                            // Проверяем, не применялся ли уже этот промокод к этой карте
                            String usageSql = "SELECT id FROM promo_codes WHERE id = ? AND used_by = ?";
                            try (PreparedStatement usageStmt = conn.prepareStatement(usageSql)) {
                                usageStmt.setLong(1, promoCodeId);
                                usageStmt.setLong(2, selectedCard.getId());
                                try (ResultSet usageRs = usageStmt.executeQuery()) {
                                    if (usageRs.next()) {
                                        throw new RuntimeException("Этот промокод уже применен к данной карте");
                                    }
                                }
                            }
                            
                            // Применяем промокод
                            if (bonusPoints != null && bonusPoints > 0) {
                                // Начисляем бонусные баллы
                                String updateCardSql = "UPDATE cards SET points = points + ? WHERE id = ?";
                                try (PreparedStatement updateStmt = conn.prepareStatement(updateCardSql)) {
                                    updateStmt.setInt(1, bonusPoints);
                                    updateStmt.setLong(2, selectedCard.getId());
                                    updateStmt.executeUpdate();
                                }
                            }
                            
                            // Отмечаем промокод как использованный
                            String markUsedSql = "UPDATE promo_codes SET used_by = ?, uses_count = uses_count + 1 WHERE id = ?";
                            try (PreparedStatement markStmt = conn.prepareStatement(markUsedSql)) {
                                markStmt.setLong(1, selectedCard.getId());
                                markStmt.setLong(2, promoCodeId);
                                markStmt.executeUpdate();
                            }
                            
                            // Получаем обновленные данные карты
                            String getCardSql = "SELECT * FROM cards WHERE id = ?";
                            try (PreparedStatement getStmt = conn.prepareStatement(getCardSql)) {
                                getStmt.setLong(1, selectedCard.getId());
                                try (ResultSet cardRs = getStmt.executeQuery()) {
                                    if (cardRs.next()) {
                                        selectedCard.setPoints(cardRs.getInt("points"));
                                    }
                                }
                            }
                            
                            return String.format("Промокод успешно применен!\n" +
                                                 (bonusPoints != null ? "Начислено баллов: " + bonusPoints : "") +
                                                 (discountPercent != null ? "Скидка: " + discountPercent + "%" : ""));
                        }
                    }
                } catch (Exception e) {
                    logger.error("Ошибка при применении промокода", e);
                    throw new RuntimeException(e.getMessage());
                }
            }).whenComplete((promoResult, throwable) -> {
                Platform.runLater(() -> {
                    loadingAlert.close();
                    
                    if (throwable != null) {
                        showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось применить промокод: " + throwable.getMessage());
                    } else {
                        showAlert(Alert.AlertType.INFORMATION, "Успех", promoResult);
                        // Обновляем отображение карты
                        cardsTable.refresh();
                    }
                });
            });
        }
    }

    /**
     * Обрабатывает просмотр истории сессий клиента
     */
    @FXML
    private void handleViewMySessionHistory() {
        Card selectedCard = cardsTable.getSelectionModel().getSelectedItem();
        if (selectedCard == null) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Выберите карту для просмотра истории сессий");
            return;
        }
        
        Stage stage = new Stage();
        stage.setTitle("История игровых сессий - Карта " + selectedCard.getNumber());
        stage.initModality(Modality.APPLICATION_MODAL);
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        
        Label titleLabel = new Label("История игровых сессий");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        TableView<Session> sessionsTable = new TableView<>();
        sessionsTable.setPrefHeight(400);
        
        TableColumn<Session, String> computerCol = new TableColumn<>("Компьютер");
        computerCol.setCellValueFactory(param -> {
            Session session = param.getValue();
            return new SimpleStringProperty(session.getComputerName());
        });
        
        TableColumn<Session, String> startTimeCol = new TableColumn<>("Начало");
        startTimeCol.setCellValueFactory(param -> 
            new SimpleStringProperty(param.getValue().getStartTime() != null ? 
                    param.getValue().getStartTime().toString() : ""));
        
        TableColumn<Session, String> endTimeCol = new TableColumn<>("Окончание");
        endTimeCol.setCellValueFactory(param -> 
            new SimpleStringProperty(param.getValue().getEndTime() != null ? 
                    param.getValue().getEndTime().toString() : "В процессе"));
        
        TableColumn<Session, String> durationCol = new TableColumn<>("Длительность");
        durationCol.setCellValueFactory(param -> {
            Session session = param.getValue();
            int minutes = session.getMinutes() != null ? session.getMinutes() : 0;
            int hours = minutes / 60;
            int mins = minutes % 60;
            return new SimpleStringProperty(String.format("%d:%02d", hours, mins));
        });
        
        TableColumn<Session, String> pointsCol = new TableColumn<>("Заработано баллов");
        pointsCol.setCellValueFactory(param -> 
            new SimpleStringProperty(param.getValue().getPoints() != null ? 
                    param.getValue().getPoints().toString() : "0"));
        
        sessionsTable.getColumns().addAll(computerCol, startTimeCol, endTimeCol, durationCol, pointsCol);
        
        // Загружаем сессии только для выбранной карты
        ObservableList<Session> sessionData = FXCollections.observableArrayList();
        List<Session> sessions = sessionService.getSessionsByCardIdAndPeriod(
                selectedCard.getId(), null, null);
        sessionData.addAll(sessions);
        sessionsTable.setItems(sessionData);
        
        Button closeButton = new Button("Закрыть");
        closeButton.setOnAction(e -> stage.close());
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().add(closeButton);
        
        root.getChildren().addAll(titleLabel, sessionsTable, buttonBox);
        
        Scene scene = new Scene(root, 700, 500);
        stage.setScene(scene);
        stage.show();
    }
    
    /**
     * Получает соединение с базой данных
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                SessionService.DB_URL, SessionService.DB_USER, SessionService.DB_PASSWORD);
    }
} 